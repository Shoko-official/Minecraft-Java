package dev.shoko.minecraft.data.codec;

import static dev.shoko.minecraft.data.testing.Cx.blocks;
import static dev.shoko.minecraft.data.testing.Cx.universe;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.FakeAccess;
import dev.shoko.minecraft.data.testing.Jx;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: data-datapack-loading R56, R62, R63, R64 (adversarial): random JSON against composite
// codecs. A result is a value or an error, never an exception, and the same input gives the same
// diagnostics. Fixed seeds, no clock.
class CodecsAttackTest {

  private static final String[] KEYS = {"a", "b", "c", "type", "x", ""};

  private static void value(Random random, int depth, StringBuilder out) {
    int kind = random.nextInt(depth > 4 ? 6 : 8);
    switch (kind) {
      case 0 -> out.append("null");
      case 1 -> out.append(random.nextBoolean());
      case 2 ->
          out.append(
              new String[] {
                    "0",
                    "-0",
                    "1",
                    "-1",
                    "2147483648",
                    "1.0",
                    "1e2",
                    "-0.0",
                    "1e40",
                    "9007199254740993"
                  }
                  [random.nextInt(10)]);
      case 3, 4 ->
          out.append('"')
              .append(
                  new String[] {"a", "b", "c", "#t", "x", "minecraft:a", "Bad:Id", "", "#", "first"}
                      [random.nextInt(10)])
              .append('"');
      case 5 -> out.append("[]");
      case 6 -> {
        out.append('[');
        int count = random.nextInt(4);
        for (int i = 0; i < count; i++) {
          out.append(i == 0 ? "" : ",");
          value(random, depth + 1, out);
        }
        out.append(']');
      }
      default -> {
        out.append('{');
        int count = random.nextInt(4);
        for (int i = 0; i < count; i++) {
          out.append(i == 0 ? "" : ",").append('"').append(KEYS[random.nextInt(KEYS.length)]);
          out.append("\":");
          value(random, depth + 1, out);
        }
        out.append('}');
      }
    }
  }

  private static List<Codec<?>> codecs() {
    Codec<List<Integer>> pair =
        Codecs.object(
            UnknownFields.REJECT,
            reader -> {
              Supplier<Integer> a = reader.required("a", Codecs.integer());
              Supplier<Double> b = reader.optional("b", Codecs.decimal(), 0.5);
              Supplier<List<Integer>> c =
                  reader.optional("c", Codecs.list(Codecs.integer()), List.of());
              return () -> List.of(a.get(), b.get().intValue(), c.get().size());
            });
    return List.of(
        Codecs.string(),
        Codecs.bool(),
        Codecs.integer(),
        Codecs.longInteger(),
        Codecs.decimal(),
        Codecs.floatDecimal(),
        Codecs.identifier(),
        Codecs.raw(),
        Codecs.list(Codecs.integer()),
        Codecs.list(Codecs.list(Codecs.string())),
        Codecs.holderSet(blocks()),
        Codecs.registryReference(blocks()),
        Codecs.list(Codecs.holderSet(blocks())),
        pair,
        Codecs.list(pair));
  }

  private static void assertWellFormed(Outcome<?> outcome, String context) {
    if (outcome.value().isEmpty()) {
      assertThat(outcome.hasErrors()).as("a codec failure has an error: %s", context).isTrue();
    }
    for (Diagnostic diagnostic : outcome.diagnostics()) {
      assertThat(diagnostic.code().severity()).as(context).isNotNull();
      assertThat(diagnostic.path().toString()).as(context).startsWith("$");
      assertThat(diagnostic.code())
          .as(context)
          .isNotIn(
              DiagnosticCode.JSON_SYNTAX,
              DiagnosticCode.JSON_BOM,
              DiagnosticCode.JSON_EMPTY,
              DiagnosticCode.JSON_TRAILING,
              DiagnosticCode.JSON_UTF8,
              DiagnosticCode.PACK_INVALID,
              DiagnosticCode.PACK_FLAGS_INACTIVE,
              DiagnosticCode.FILE_IGNORED);
    }
  }

  @Test
  @Timeout(60)
  void provisionalRandomJsonNeverBreaksAnyCodecAndResultsAreDeterministic() {
    // spec: R54, R56, R62 g
    Random random = new Random(0xC0DECL);
    FakeAccess access = universe();
    List<Codec<?>> codecs = codecs();
    for (int i = 0; i < 4_000; i++) {
      StringBuilder text = new StringBuilder();
      value(random, 0, text);
      JsonNode node = Jx.parseLenient(text.toString());
      for (int c = 0; c < codecs.size(); c++) {
        String context = "codec #" + c + " on " + text;
        Outcome<?> first = codecs.get(c).decode(node, access);
        Outcome<?> second = codecs.get(c).decode(node, access);
        assertWellFormed(first, context);
        assertThat(Diags.summary(second)).as(context).isEqualTo(Diags.summary(first));
        assertThat(second.value().isPresent()).as(context).isEqualTo(first.value().isPresent());
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalAnObjectCodecReportsEachUnknownFieldOnce() {
    // spec: R63 b: n unknown keys give n diagnostics, in text order
    Codec<Integer> strict =
        Codecs.object(
            UnknownFields.REJECT,
            reader -> {
              Supplier<Integer> a = reader.required("a", Codecs.integer());
              return a;
            });
    StringBuilder json = new StringBuilder("{\"a\":1");
    for (int i = 0; i < 2_000; i++) {
      json.append(",\"u").append(i).append("\":").append(i);
    }
    Outcome<Integer> outcome =
        strict.decode(Jx.parse(json.append('}').toString()), FakeAccess.create());
    assertThat(outcome.value()).hasValue(1);
    assertThat(outcome.diagnostics()).hasSize(2_000);
    assertThat(Diags.summary(outcome).get(0)).isEqualTo("CODEC_UNKNOWN_FIELD@$.u0");
    assertThat(Diags.summary(outcome).get(1_999)).isEqualTo("CODEC_UNKNOWN_FIELD@$.u1999");
  }

  @Test
  @Timeout(60)
  void provisionalDeepNestingOfListsIsDecodedOrRefusedWithoutAStackError() {
    // spec: R57 (a tree has at most 255 levels): the deepest tree the reader accepts
    String text = "[".repeat(254) + "1" + "]".repeat(254);
    Codec<?> codec = Codecs.raw();
    for (int i = 0; i < 254; i++) {
      codec = Codecs.list(codec);
    }
    Outcome<?> outcome = codec.decode(Jx.parse(text), FakeAccess.create());
    assertThat(outcome.diagnostics()).isEmpty();
    Codec<?> tooShallow = Codecs.list(Codecs.integer());
    Outcome<?> mismatch = tooShallow.decode(Jx.parse(text), FakeAccess.create());
    Diags.assertSummary(mismatch, "CODEC_TYPE_MISMATCH@$[0]");
  }

  @Test
  @Timeout(60)
  void provisionalHugeListsAreDecodedWithEveryElementPathCorrect() {
    // spec: R62 g
    StringBuilder json = new StringBuilder("[");
    for (int i = 0; i < 50_000; i++) {
      json.append(i == 0 ? "" : ",").append(i % 1000 == 999 ? "\"bad\"" : "1");
    }
    Outcome<List<Integer>> outcome =
        Codecs.list(Codecs.integer())
            .decode(Jx.parse(json.append(']').toString()), FakeAccess.create());
    assertThat(outcome.value()).isEmpty();
    assertThat(outcome.diagnostics()).hasSize(50);
    assertThat(Diags.summary(outcome).get(0)).isEqualTo("CODEC_TYPE_MISMATCH@$[999]");
    assertThat(Diags.summary(outcome).get(49)).isEqualTo("CODEC_TYPE_MISMATCH@$[49999]");
  }

  @Test
  @Timeout(60)
  void provisionalHolderSetsOverManyEntriesKeepFirstOccurrences() {
    // spec: R64 c
    String[] names = new String[300];
    for (int i = 0; i < names.length; i++) {
      names[i] = "e" + i;
    }
    FakeAccess access = FakeAccess.create().entries(blocks(), names);
    StringBuilder json = new StringBuilder("[");
    for (int i = 0; i < 6_000; i++) {
      json.append(i == 0 ? "" : ",").append("\"e").append(i % 300).append('"');
    }
    Outcome<HolderSet<String>> outcome =
        Codecs.holderSet(blocks()).decode(Jx.parse(json.append(']').toString()), access);
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(outcome.value().orElseThrow().holders()).hasSize(300);
  }
}
