package dev.shoko.minecraft.data.codec;

import static dev.shoko.minecraft.data.testing.Cx.decode;
import static dev.shoko.minecraft.data.testing.Cx.value;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Expect;
import dev.shoko.minecraft.data.testing.FakeAccess;
import dev.shoko.minecraft.data.testing.Jx;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// spec: data-datapack-loading R56, R62 g, R63, vectors VC13 to VC15 (object codecs)
class CodecsObjectTest {

  private record Point(int x, String name, Optional<Boolean> flag, double scale) {}

  private static Codec<Integer> onlyA(UnknownFields mode) {
    return Codecs.object(
        mode,
        reader -> {
          Supplier<Integer> a = reader.required("a", Codecs.integer());
          return a;
        });
  }

  private static Codec<Point> point(UnknownFields mode) {
    return Codecs.object(
        mode,
        reader -> {
          Supplier<Integer> x = reader.required("x", Codecs.integer());
          Supplier<String> name = reader.required("name", Codecs.string());
          Supplier<Optional<Boolean>> flag = reader.optional("flag", Codecs.bool());
          Supplier<Double> scale = reader.optional("scale", Codecs.decimal(), 1.0);
          return () -> new Point(x.get(), name.get(), flag.get(), scale.get());
        });
  }

  @Test
  void provisionalTheDefaultModeIgnoresUnknownFields() {
    // spec: VC13, R63 a
    Codec<Integer> codec =
        Codecs.object(
            reader -> {
              Supplier<Integer> a = reader.required("a", Codecs.integer());
              return a;
            });
    Outcome<Integer> outcome = decode(codec, "{\"a\":1,\"b\":2}");
    assertThat(outcome.value()).hasValue(1);
    assertThat(outcome.diagnostics()).isEmpty();
  }

  @Test
  void provisionalTheStrictModeReportsEveryUnknownFieldAndStillDecodes() {
    // spec: VC14, R63 b
    Outcome<Integer> outcome = decode(onlyA(UnknownFields.REJECT), "{\"a\":1,\"b\":2,\"c\":3}");
    assertThat(outcome.value()).hasValue(1);
    Diags.assertSummary(outcome, "CODEC_UNKNOWN_FIELD@$.b", "CODEC_UNKNOWN_FIELD@$.c");
    assertThat(outcome.hasErrors()).isTrue();
  }

  @Test
  void provisionalTheExplicitIgnoreModeBehavesLikeTheDefault() {
    // spec: R63 a
    Outcome<Integer> outcome = decode(onlyA(UnknownFields.IGNORE), "{\"b\":1,\"a\":2}");
    assertThat(outcome.value()).hasValue(2);
    assertThat(outcome.diagnostics()).isEmpty();
  }

  @Test
  void provisionalAMissingRequiredFieldIsReportedAtTheMissingMember() {
    // spec: VC15, R56
    Outcome<Integer> outcome = decode(onlyA(UnknownFields.IGNORE), "{\"b\":2}");
    assertThat(outcome.value()).isEmpty();
    Diags.assertSummary(outcome, "CODEC_MISSING_FIELD@$.a");
  }

  @Test
  void provisionalMissingAndUnknownFieldsAreBothReportedInStrictMode() {
    // spec: VC15 (two diagnostics, order between them not checked)
    Outcome<Integer> outcome = decode(onlyA(UnknownFields.REJECT), "{\"b\":2}");
    assertThat(outcome.value()).isEmpty();
    assertThat(Diags.summary(outcome))
        .containsExactlyInAnyOrder("CODEC_MISSING_FIELD@$.a", "CODEC_UNKNOWN_FIELD@$.b");
  }

  @ParameterizedTest
  @ValueSource(strings = {"[]", "1", "1.0", "\"s\"", "true", "null"})
  void provisionalAnObjectCodecAcceptsOnlyObjects(String json) {
    // spec: Codecs.object Javadoc, R62 f
    Diags.assertSingleError(
        decode(onlyA(UnknownFields.IGNORE), json), DiagnosticCode.CODEC_TYPE_MISMATCH, "$");
  }

  @Test
  void provisionalARequiredFieldOfTheWrongTypeIsAnErrorNotADefault() {
    // spec: R56, R62
    Diags.assertSingleError(
        decode(onlyA(UnknownFields.IGNORE), "{\"a\":\"1\"}"),
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        "$.a");
    Diags.assertSingleError(
        decode(onlyA(UnknownFields.IGNORE), "{\"a\":null}"),
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        "$.a");
    Diags.assertSingleError(
        decode(onlyA(UnknownFields.IGNORE), "{\"a\":1.5}"),
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        "$.a");
    Diags.assertSingleError(
        decode(onlyA(UnknownFields.IGNORE), "{\"a\":99999999999}"),
        DiagnosticCode.CODEC_NUMBER_RANGE,
        "$.a");
  }

  @Test
  void provisionalOptionalFieldsTakeTheirDefaultOrEmptyWhenAbsent() {
    // spec: R56
    Point point = value(point(UnknownFields.IGNORE), "{\"x\":3,\"name\":\"n\"}");
    assertThat(point).isEqualTo(new Point(3, "n", Optional.empty(), 1.0));
  }

  @Test
  void provisionalOptionalFieldsAreReadWhenPresent() {
    // spec: R56
    Point point =
        value(point(UnknownFields.IGNORE), "{\"x\":3,\"name\":\"n\",\"flag\":false,\"scale\":2}");
    assertThat(point).isEqualTo(new Point(3, "n", Optional.of(false), 2.0));
  }

  @Test
  void provisionalAPresentOptionalFieldOfTheWrongTypeIsAnErrorNeverTheDefault() {
    // spec: R56 ("champ present de mauvais type: erreur, jamais le defaut")
    Outcome<Point> outcome =
        decode(
            point(UnknownFields.IGNORE), "{\"x\":3,\"name\":\"n\",\"flag\":1,\"scale\":\"big\"}");
    assertThat(outcome.value()).isEmpty();
    Diags.assertSummary(outcome, "CODEC_TYPE_MISMATCH@$.flag", "CODEC_TYPE_MISMATCH@$.scale");
  }

  @Test
  void provisionalANullOptionalFieldIsAnError() {
    // spec: R56 ("une valeur null presente est une erreur de type")
    Outcome<Point> outcome =
        decode(point(UnknownFields.IGNORE), "{\"x\":3,\"name\":\"n\",\"flag\":null}");
    Diags.assertSummary(outcome, "CODEC_TYPE_MISMATCH@$.flag");
    assertThat(outcome.value()).isEmpty();
  }

  @Test
  void provisionalAllErrorsOfAnObjectAreCollectedInTextOrder() {
    // spec: R62 g (declaration order equals text order here)
    Outcome<Point> outcome =
        decode(
            point(UnknownFields.REJECT),
            "{\"x\":\"a\",\"name\":1,\"flag\":2,\"scale\":[],\"zzz\":0}");
    assertThat(outcome.value()).isEmpty();
    Diags.assertSummary(
        outcome,
        "CODEC_TYPE_MISMATCH@$.x",
        "CODEC_TYPE_MISMATCH@$.name",
        "CODEC_TYPE_MISMATCH@$.flag",
        "CODEC_TYPE_MISMATCH@$.scale",
        "CODEC_UNKNOWN_FIELD@$.zzz");
  }

  @Test
  void provisionalAnErrorOfOneFieldDoesNotStopTheOthers() {
    // spec: R62 g
    Outcome<Point> outcome = decode(point(UnknownFields.IGNORE), "{\"x\":1.5}");
    Diags.assertSummary(outcome, "CODEC_TYPE_MISMATCH@$.x", "CODEC_MISSING_FIELD@$.name");
  }

  @Test
  void provisionalNestedObjectsReportNestedPaths() {
    // spec: R61 a, R62 g
    Codec<Integer> inner = onlyA(UnknownFields.IGNORE);
    Codec<List<Integer>> outer =
        Codecs.object(
            reader -> {
              Supplier<Integer> child = reader.required("child", inner);
              Supplier<List<Integer>> items = reader.required("items", Codecs.list(inner));
              return () -> List.of(child.get(), items.get().get(0));
            });
    assertThat(value(outer, "{\"child\":{\"a\":1},\"items\":[{\"a\":2}]}")).containsExactly(1, 2);
    Outcome<List<Integer>> outcome =
        decode(outer, "{\"child\":{\"a\":\"x\"},\"items\":[{\"a\":1},{},{\"a\":true}]}");
    Diags.assertSummary(
        outcome,
        "CODEC_TYPE_MISMATCH@$.child.a",
        "CODEC_MISSING_FIELD@$.items[1].a",
        "CODEC_TYPE_MISMATCH@$.items[2].a");
  }

  @Test
  void provisionalAHandleIsOnlyReadWhenNoFieldFailed() {
    // spec: ObjectReader Javadoc, Codecs.object Javadoc ("the supplier ... is called")
    AtomicInteger builds = new AtomicInteger();
    Codec<Integer> codec =
        Codecs.object(
            reader -> {
              Supplier<Integer> a = reader.required("a", Codecs.integer());
              return () -> {
                builds.incrementAndGet();
                return a.get();
              };
            });
    decode(codec, "{\"a\":\"x\"}");
    decode(codec, "{}");
    assertThat(builds).hasValue(0);
    assertThat(value(codec, "{\"a\":4}")).isEqualTo(4);
    assertThat(builds).hasValue(1);
  }

  @Test
  void provisionalReadingAFailedHandleIsAnIllegalState() {
    // spec: ObjectReader.required Javadoc
    AtomicReference<Supplier<Integer>> handle = new AtomicReference<>();
    Codec<Integer> codec =
        Codecs.object(
            reader -> {
              handle.set(reader.required("a", Codecs.integer()));
              return () -> handle.get().get();
            });
    decode(codec, "{\"a\":\"x\"}");
    Expect.illegalState(() -> handle.get().get());
  }

  @Test
  void provisionalReadingAnObjectWithNoFieldAtAllNeedsOnlyOptionals() {
    // spec: R56
    Codec<Optional<Integer>> codec =
        Codecs.object(
            reader -> {
              Supplier<Optional<Integer>> a = reader.optional("a", Codecs.integer());
              return a;
            });
    assertThat(value(codec, "{}")).isEmpty();
    assertThat(value(codec, "{\"a\":7}")).hasValue(7);
    assertThat(value(codec, "{\"zzz\":7}")).isEmpty();
  }

  @Test
  void provisionalHasMarksTheKeyAsKnownAndTellsPresence() {
    // spec: ObjectReader Javadoc ("A key is known as soon as one method ... has included")
    AtomicReference<List<Boolean>> seen = new AtomicReference<>();
    Codec<Integer> codec =
        Codecs.object(
            UnknownFields.REJECT,
            reader -> {
              Supplier<Integer> a = reader.required("a", Codecs.integer());
              seen.set(List.of(reader.has("c"), reader.has("d"), reader.has("n")));
              return a;
            });
    Outcome<Integer> outcome = decode(codec, "{\"a\":1,\"c\":9,\"n\":null,\"other\":0}");
    assertThat(seen.get()).containsExactly(true, false, true);
    Diags.assertSummary(outcome, "CODEC_UNKNOWN_FIELD@$.other");
  }

  @Test
  void provisionalTheReaderExposesTheNodeTheAccessAndTheMode() {
    // spec: ObjectReader Javadoc
    FakeAccess access = FakeAccess.create();
    AtomicReference<JsonObject> node = new AtomicReference<>();
    AtomicReference<RegistryAccess> given = new AtomicReference<>();
    AtomicReference<UnknownFields> mode = new AtomicReference<>();
    Codec<Integer> codec =
        Codecs.object(
            UnknownFields.REJECT,
            reader -> {
              node.set(reader.node());
              given.set(reader.access());
              mode.set(reader.unknownFields());
              Supplier<Integer> a = reader.required("a", Codecs.integer());
              return a;
            });
    JsonNode root = Jx.parse("{\"a\":1}");
    codec.decode(root, access);
    assertThat(node.get()).isSameAs(root);
    assertThat(given.get()).isSameAs(access);
    assertThat(mode.get()).isEqualTo(UnknownFields.REJECT);
    AtomicReference<UnknownFields> defaultMode = new AtomicReference<>();
    Codecs.<Integer>object(
            reader -> {
              defaultMode.set(reader.unknownFields());
              Supplier<Integer> a = reader.required("a", Codecs.integer());
              return a;
            })
        .decode(root, access);
    assertThat(defaultMode.get()).isEqualTo(UnknownFields.IGNORE);
  }

  @Test
  void provisionalTheReadingFunctionIsCalledOncePerDecode() {
    // spec: Codecs.object Javadoc ("calls reading once per decode")
    AtomicInteger calls = new AtomicInteger();
    Codec<Integer> codec =
        Codecs.object(
            reader -> {
              calls.incrementAndGet();
              Supplier<Integer> a = reader.required("a", Codecs.integer());
              return a;
            });
    decode(codec, "{\"a\":1}");
    assertThat(calls).hasValue(1);
    decode(codec, "{\"a\":2}");
    decode(codec, "{}");
    assertThat(calls).hasValue(3);
  }

  @Test
  void provisionalAnEmptyKeyCanBeAFieldName() {
    // spec: R56 (any key name)
    Codec<Integer> codec =
        Codecs.object(
            reader -> {
              Supplier<Integer> empty = reader.required("", Codecs.integer());
              return empty;
            });
    assertThat(value(codec, "{\"\":5}")).isEqualTo(5);
  }

  @Test
  void provisionalFieldNamesAreCaseSensitive() {
    // spec: R58 a (keys are compared without case folding)
    Diags.assertSummary(
        decode(onlyA(UnknownFields.IGNORE), "{\"A\":1}"), "CODEC_MISSING_FIELD@$.a");
  }

  @Test
  void provisionalAnIdentifierFieldIsReadThroughTheIdentifierCodec() {
    // spec: K1, R64
    Codec<Identifier> codec =
        Codecs.object(
            reader -> {
              Supplier<Identifier> id = reader.required("id", Codecs.identifier());
              return id;
            });
    assertThat(value(codec, "{\"id\":\"stone\"}")).isEqualTo(Identifier.parse("minecraft:stone"));
    Diags.assertSummary(decode(codec, "{\"id\":\"Bad:Id\"}"), "CODEC_INVALID_IDENTIFIER@$.id");
  }
}
