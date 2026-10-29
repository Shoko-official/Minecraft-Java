package dev.shoko.minecraft.data.json;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.MiniData;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: data-datapack-loading R52 to R54, R57 to R61 (adversarial): fixed seeds, no clock, only
// typed outcomes. Invariant of every read: a value and no error, or no value and exactly one error.
class JsonReaderAttackTest {

  private static final String GRIN = new String(Character.toChars(0x1F600));

  private static final String SEED_DOCUMENT =
      "{\"a\":[1,2.5,-3e2,true,false,null,\"x\\n\\u00e9\\ud83d\\ude00\"],\"b\":{\"c\":{},\"d\":[]},"
          + "\"e\":\"\",\"f\":0,\"g\":-0.0}";

  private static void assertInvariant(Outcome<JsonNode> outcome, String context) {
    long errors =
        outcome.diagnostics().stream().filter(d -> d.code().severity() == Severity.ERROR).count();
    if (outcome.value().isPresent()) {
      assertThat(errors).as("errors of a read with a value: %s", context).isZero();
      assertThat(outcome.diagnostics())
          .as("only duplicate-key warnings: %s", context)
          .allSatisfy(d -> assertThat(d.code()).isEqualTo(DiagnosticCode.JSON_DUPLICATE_KEY));
    } else {
      assertThat(outcome.diagnostics())
          .as("diagnostics of an invalid read: %s", context)
          .hasSize(1);
      assertThat(errors).as("errors of an invalid read: %s", context).isEqualTo(1);
    }
  }

  @Test
  @Timeout(60)
  void provisionalRandomBytesNeverBreakTheInvariant() {
    // spec: R54, R61 c
    Random random = new Random(0xA77AC4L);
    for (int i = 0; i < 20_000; i++) {
      byte[] bytes = new byte[random.nextInt(24)];
      random.nextBytes(bytes);
      assertInvariant(Jx.read(bytes), "random bytes #" + i);
    }
  }

  @Test
  @Timeout(60)
  void provisionalRandomJsonLikeTextNeverBreaksTheInvariant() {
    // spec: R52, R54, R61 c
    String alphabet = "{}[]\",:0123456789.eE+-truefalsnl \n\\/u*#'xA\351";
    Random random = new Random(0xB10CL);
    for (int i = 0; i < 30_000; i++) {
      StringBuilder text = new StringBuilder();
      int length = random.nextInt(30);
      for (int k = 0; k < length; k++) {
        text.append(alphabet.charAt(random.nextInt(alphabet.length())));
      }
      assertInvariant(Jx.read(text.toString()), text.toString());
    }
  }

  @Test
  @Timeout(60)
  void provisionalEveryPrefixOfAValidFileKeepsTheInvariant() {
    // spec: R53, R54 (a truncated file is invalid, never an exception)
    byte[] full = Jx.utf8(SEED_DOCUMENT);
    for (int length = 0; length < full.length; length++) {
      byte[] prefix = Arrays.copyOf(full, length);
      Outcome<JsonNode> outcome = Jx.read(prefix);
      assertInvariant(outcome, "prefix " + length);
      assertThat(outcome.value()).as("proper prefix %s", length).isEmpty();
    }
    assertThat(Jx.read(full).diagnostics()).isEmpty();
  }

  @Test
  @Timeout(60)
  void provisionalSingleByteMutationsKeepTheInvariantAndAreDeterministic() {
    // spec: R54, R61 c, determinism
    byte[] full = Jx.utf8(SEED_DOCUMENT);
    Random random = new Random(0x33L);
    for (int i = 0; i < 20_000; i++) {
      byte[] mutated = full.clone();
      mutated[random.nextInt(mutated.length)] = (byte) random.nextInt(256);
      Outcome<JsonNode> first = Jx.read(mutated);
      Outcome<JsonNode> second = Jx.read(mutated);
      assertInvariant(first, "mutation #" + i);
      assertThat(Diags.summary(second)).isEqualTo(Diags.summary(first));
      assertThat(second.value().isPresent()).isEqualTo(first.value().isPresent());
    }
  }

  @Test
  @Timeout(60)
  void provisionalGeneratedValidDocumentsReadBackExactly() {
    // spec: R49, R52, R58, R60: whitespace, escapes and nesting of valid text
    Random random = new Random(0x600DL);
    for (int i = 0; i < 3_000; i++) {
      StringBuilder text = new StringBuilder();
      StringBuilder expected = new StringBuilder();
      generate(random, 0, text, expected);
      JsonNode node = Jx.parse(text.toString());
      assertThat(MiniData.describe(node)).as(text.toString()).isEqualTo(expected.toString());
    }
  }

  private static void space(Random random, StringBuilder text) {
    String[] blanks = {"", "", " ", "\n", "\t", "\r\n", "  "};
    text.append(blanks[random.nextInt(blanks.length)]);
  }

  private static void generate(
      Random random, int depth, StringBuilder text, StringBuilder expected) {
    space(random, text);
    int kind = random.nextInt(depth > 6 ? 5 : 7);
    switch (kind) {
      case 0 -> literal(text, expected, "null");
      case 1 -> literal(text, expected, random.nextBoolean() ? "true" : "false");
      case 2 -> literal(text, expected, number(random));
      case 3, 4 -> string(random, text, expected, false);
      case 5 -> {
        text.append('[');
        expected.append('[');
        int count = random.nextInt(4);
        for (int k = 0; k < count; k++) {
          if (k > 0) {
            text.append(',');
            expected.append(", ");
          }
          generate(random, depth + 1, text, expected);
        }
        space(random, text);
        text.append(']');
        expected.append(']');
      }
      default -> {
        text.append('{');
        expected.append('{');
        int count = random.nextInt(4);
        for (int k = 0; k < count; k++) {
          if (k > 0) {
            text.append(',');
          }
          space(random, text);
          string(random, text, expected, true);
          expected.append('=');
          space(random, text);
          text.append(':');
          generate(random, depth + 1, text, expected);
          expected.append(',');
        }
        space(random, text);
        text.append('}');
        expected.append('}');
      }
    }
    space(random, text);
  }

  private static void literal(StringBuilder text, StringBuilder expected, String value) {
    text.append(value);
    expected.append(value);
  }

  private static String number(Random random) {
    String[] samples = {
      "0", "-0", "7", "-12", "1.5", "-0.0", "1e5", "2E-3", "123456789012", "0.25"
    };
    return samples[random.nextInt(samples.length)];
  }

  /** Writes a string; keys get their index so that they are unique within an object. */
  private static void string(
      Random random, StringBuilder text, StringBuilder expected, boolean key) {
    String[][] pieces = {
      {"a", "a"},
      {"Z", "Z"},
      {" ", " "},
      {"\\n", "\n"},
      {"\\t", "\t"},
      {"\\\"", "\""},
      {"\\\\", "\\"},
      {"\\/", "/"},
      {"\\u00e9", "\351"},
      {"\351", "\351"},
      {"\\ud83d\\ude00", GRIN},
      {"\\ud800", "\ud800"},
      {"\\u0000", "\u0000"},
      {GRIN, GRIN}
    };
    StringBuilder raw = new StringBuilder();
    StringBuilder value = new StringBuilder();
    int count = random.nextInt(5);
    for (int k = 0; k < count; k++) {
      String[] piece = pieces[random.nextInt(pieces.length)];
      raw.append(piece[0]);
      value.append(piece[1]);
    }
    if (key) {
      String suffix = Integer.toString(text.length());
      raw.append('#').append(suffix);
      value.append('#').append(suffix);
    }
    text.append('"').append(raw).append('"');
    expected.append(key ? value.toString() : '"' + value.toString() + '"');
  }

  @Test
  @Timeout(60)
  void provisionalRandomDepthsAreAcceptedUpTo255AndRefusedBeyond() {
    // spec: R57
    Random random = new Random(0xDEEDL);
    for (int i = 0; i < 300; i++) {
      int depth = 1 + random.nextInt(600);
      StringBuilder open = new StringBuilder();
      StringBuilder close = new StringBuilder();
      for (int k = 0; k < depth; k++) {
        if (random.nextBoolean()) {
          open.append('[');
          close.insert(0, ']');
        } else {
          open.append("{\"k\":");
          close.insert(0, '}');
        }
      }
      String text = open + "0" + close;
      Outcome<JsonNode> outcome = Jx.read(text);
      if (depth <= JsonReader.MAX_DEPTH) {
        assertThat(outcome.diagnostics()).as("depth %s", depth).isEmpty();
      } else {
        Diags.assertSingleError(outcome, DiagnosticCode.JSON_TOO_DEEP);
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalRandomIntegersFollowTheSigned64BitRange() {
    // spec: R59 a, b (model: BigInteger)
    Random random = new Random(0x1177L);
    BigInteger min = BigInteger.valueOf(Long.MIN_VALUE);
    BigInteger max = BigInteger.valueOf(Long.MAX_VALUE);
    for (int i = 0; i < 5_000; i++) {
      int digits = 1 + random.nextInt(24);
      StringBuilder text = new StringBuilder(random.nextBoolean() ? "-" : "");
      text.append(1 + random.nextInt(9));
      for (int k = 1; k < digits; k++) {
        text.append(random.nextInt(10));
      }
      BigInteger value = new BigInteger(text.toString());
      Outcome<JsonNode> outcome = Jx.read(text.toString());
      if (value.compareTo(min) >= 0 && value.compareTo(max) <= 0) {
        JsonNumber number = (JsonNumber) outcome.value().orElseThrow();
        assertThat(number.isInteger()).isTrue();
        assertThat(number.longValue()).as(text.toString()).isEqualTo(value.longValueExact());
      } else {
        Diags.assertSingleError(outcome, DiagnosticCode.JSON_NUMBER_RANGE);
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalRandomDecimalsAreCorrectlyRoundedOrOutOfRange() {
    // spec: R50, R59 d, f (model: the correctly rounded conversion of the standard library)
    Random random = new Random(0xDEC1L);
    for (int i = 0; i < 5_000; i++) {
      StringBuilder text = new StringBuilder(random.nextBoolean() ? "-" : "");
      text.append(random.nextInt(10));
      int fraction = 1 + random.nextInt(25);
      text.append('.');
      for (int k = 0; k < fraction; k++) {
        text.append(random.nextInt(10));
      }
      text.append(random.nextBoolean() ? 'e' : 'E').append(random.nextInt(700) - 350);
      double expected = Double.parseDouble(text.toString());
      Outcome<JsonNode> outcome = Jx.read(text.toString());
      if (Double.isInfinite(expected)) {
        Diags.assertSingleError(outcome, DiagnosticCode.JSON_NUMBER_RANGE);
      } else {
        JsonNumber number = (JsonNumber) outcome.value().orElseThrow();
        assertThat(number.isInteger()).isFalse();
        assertThat(Double.doubleToRawLongBits(number.doubleValue()))
            .as(text.toString())
            .isEqualTo(Double.doubleToRawLongBits(expected));
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalHugeInputsDoNotCrashTheReader() {
    // spec: R57, R59 f: a long number, a long string, a long run of siblings, a long key list
    assertInvariant(Jx.read("9".repeat(200_000)), "200000 digits");
    assertInvariant(Jx.read("0." + "9".repeat(200_000)), "200000 decimals");
    assertInvariant(Jx.read("\"" + "\\u00e9".repeat(200_000) + "\""), "long escapes");
    assertInvariant(Jx.read("[" + "{},".repeat(200_000) + "{}]"), "many siblings");
    StringBuilder keys = new StringBuilder("{");
    for (int i = 0; i < 100_000; i++) {
      keys.append(i == 0 ? "" : ",").append("\"k").append(i).append("\":").append(i);
    }
    Outcome<JsonNode> outcome = Jx.read(keys.append('}').toString());
    assertThat(((JsonObject) outcome.value().orElseThrow()).members()).hasSize(100_000);
    for (Diagnostic d : outcome.diagnostics()) {
      assertThat(d.code()).isEqualTo(DiagnosticCode.JSON_DUPLICATE_KEY);
    }
  }
}
