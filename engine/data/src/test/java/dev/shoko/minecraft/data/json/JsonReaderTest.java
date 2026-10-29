package dev.shoko.minecraft.data.json;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: data-datapack-loading R29, R49, R54, R56, R57, R61, vectors VJ1 to VJ5 (tree and depth)
class JsonReaderTest {

  private static JsonPath arrayPath(int depth) {
    JsonPath path = JsonPath.root();
    for (int i = 0; i < depth; i++) {
      path = path.index(0);
    }
    return path;
  }

  private static JsonPath objectPath(int depth) {
    JsonPath path = JsonPath.root();
    for (int i = 0; i < depth; i++) {
      path = path.field("a");
    }
    return path;
  }

  @Test
  void provisionalMaxDepthIs255() {
    // spec: R57
    assertThat(JsonReader.MAX_DEPTH).isEqualTo(255);
  }

  @Test
  void anyJsonTypeIsAValidRoot() {
    // spec: R29
    assertThat(Jx.parse("{}").type()).isEqualTo(JsonType.OBJECT);
    assertThat(Jx.parse("[]").type()).isEqualTo(JsonType.ARRAY);
    assertThat(Jx.parse("\"s\"").type()).isEqualTo(JsonType.STRING);
    assertThat(Jx.parse("1").type()).isEqualTo(JsonType.NUMBER);
    assertThat(Jx.parse("1.0").type()).isEqualTo(JsonType.NUMBER);
    assertThat(Jx.parse("true").type()).isEqualTo(JsonType.BOOLEAN);
    assertThat(Jx.parse("false").type()).isEqualTo(JsonType.BOOLEAN);
    assertThat(Jx.parse("null").type()).isEqualTo(JsonType.NULL);
  }

  @Test
  void provisionalReaderAcceptsNullEvenIfCodecsRefuseIt() {
    // spec: R56, "Arbre JSON" (null accepted by the reader)
    JsonNode root = Jx.parse("{\"a\":null}");
    assertThat(((JsonObject) root).get("a")).containsInstanceOf(JsonNull.class);
  }

  @Test
  void booleansHoldTheirValue() {
    assertThat(((JsonBoolean) Jx.parse("true")).value()).isTrue();
    assertThat(((JsonBoolean) Jx.parse("false")).value()).isFalse();
  }

  @Test
  void objectsKeepTheOrderOfTheFile() {
    // spec: "Arbre JSON" (order of first occurrence), determinism
    JsonObject object = (JsonObject) Jx.parse("{\"z\":1,\"a\":2,\"m\":3,\"b\":4}");
    assertThat(object.members().keySet()).containsExactly("z", "a", "m", "b");
  }

  @Test
  void arraysKeepTheOrderOfTheFile() {
    JsonArray array = (JsonArray) Jx.parse("[3,1,2,\"x\",true,null,[],{}]");
    assertThat(array.elements()).hasSize(8);
    assertThat(array.elements().get(0)).isInstanceOf(JsonNumber.class);
    assertThat(((JsonNumber) array.elements().get(0)).longValue()).isEqualTo(3);
    assertThat(((JsonNumber) array.elements().get(2)).longValue()).isEqualTo(2);
    assertThat(array.elements().get(3)).isInstanceOf(JsonString.class);
    assertThat(array.elements().get(5)).isInstanceOf(JsonNull.class);
    assertThat(array.elements().get(6)).isInstanceOf(JsonArray.class);
    assertThat(array.elements().get(7)).isInstanceOf(JsonObject.class);
  }

  @Test
  void provisionalEveryNodeKnowsItsFileAndItsPath() {
    // spec: "Arbre JSON" (each node knows its pack, file path and JSON path), R61 a
    JsonObject root = (JsonObject) Jx.parse("{\"a\":[1,{\"b\":true}],\"c\":\"s\"}");
    JsonArray a = (JsonArray) root.get("a").orElseThrow();
    JsonObject inner = (JsonObject) a.elements().get(1);
    List<JsonNode> nodes =
        List.of(
            root,
            a,
            a.elements().get(0),
            inner,
            inner.get("b").orElseThrow(),
            root.get("c").orElseThrow());
    assertThat(nodes).allSatisfy(node -> assertThat(node.origin()).isEqualTo(Jx.ORIGIN));
    assertThat(root.path()).isEqualTo(JsonPath.root());
    assertThat(a.path().toString()).isEqualTo("$.a");
    assertThat(a.elements().get(0).path().toString()).isEqualTo("$.a[0]");
    assertThat(inner.path().toString()).isEqualTo("$.a[1]");
    assertThat(inner.get("b").orElseThrow().path().toString()).isEqualTo("$.a[1].b");
    assertThat(root.get("c").orElseThrow().path().toString()).isEqualTo("$.c");
  }

  @Test
  void provisionalWhitespaceBetweenTokensIsSpaceTabLineFeedCarriageReturn() {
    // spec: R52 (RFC 8259 whitespace), VJ35
    JsonObject root = (JsonObject) Jx.parse(" \t\r\n{ \"a\" \t:\r\n [ 1 ,\n2 ] }\r\n\t ");
    assertThat(root.members()).containsOnlyKeys("a");
  }

  @Test
  void anEmptyKeyIsValid() {
    // RFC 8259 allows it; R49 only notes that the target data has no empty string
    JsonObject root = (JsonObject) Jx.parse("{\"\":1}");
    assertThat(root.get("")).isPresent();
  }

  @Test
  void aSmallDatapackFileReads() {
    // spec: R2 (shape of a built-in pack.mcmeta)
    String text =
        "{\"features\":{\"enabled\":[\"minecraft:trade_rebalance\"]},\"pack\":{\"description\":"
            + "{\"translate\":\"dataPack.trade_rebalance.description\"},\"max_format\":121,"
            + "\"min_format\":121}}";
    JsonObject root = (JsonObject) Jx.parse(text);
    assertThat(root.members().keySet()).containsExactly("features", "pack");
  }

  @Test
  void provisionalDepthSeventeenIsValid() {
    // spec: VJ1, R49
    Jx.parse("[".repeat(17) + "]".repeat(17));
  }

  @Test
  void provisionalDepth255IsValid() {
    // spec: VJ2, R57
    JsonNode root = Jx.parse("[".repeat(255) + "]".repeat(255));
    assertThat(root).isInstanceOf(JsonArray.class);
  }

  @Test
  void provisionalDepth255OfObjectsIsValid() {
    // spec: R57 (every '{' opens a level)
    Jx.parse("{\"a\":".repeat(254) + "{}" + "}".repeat(254));
  }

  @Test
  void provisionalDepth256OfArraysIsTooDeepAtThe256thContainer() {
    // spec: VJ3, R57
    Outcome<JsonNode> outcome = Jx.read("[".repeat(256) + "]".repeat(256));
    Diags.assertSingleError(outcome, DiagnosticCode.JSON_TOO_DEEP);
    assertThat(outcome.diagnostics().get(0).path()).isEqualTo(arrayPath(255));
  }

  @Test
  void provisionalDepth256OfObjectsIsTooDeepAtThe256thContainer() {
    // spec: VJ4, R57
    Outcome<JsonNode> outcome = Jx.read("{\"a\":".repeat(256) + "0" + "}".repeat(256));
    Diags.assertSingleError(outcome, DiagnosticCode.JSON_TOO_DEEP);
    assertThat(outcome.diagnostics().get(0).path()).isEqualTo(objectPath(255));
  }

  @Test
  void provisionalAnEmptyContainerCountsAsALevel() {
    // spec: R57 (empty container included): 255 arrays around one empty object is 256 levels
    Outcome<JsonNode> outcome = Jx.read("[".repeat(255) + "{}" + "]".repeat(255));
    Diags.assertSingleError(outcome, DiagnosticCode.JSON_TOO_DEEP);
    assertThat(outcome.diagnostics().get(0).path()).isEqualTo(arrayPath(255));
  }

  @Test
  void provisionalMixedNestingIsCountedLevelByLevel() {
    // spec: R57: [ {"a": [ {"a": ... alternating, 255 levels in all
    String open = "[{\"a\":".repeat(127) + "[";
    String close = "]" + "}]".repeat(127);
    Jx.parse(open + close);
    Outcome<JsonNode> tooDeep = Jx.read("[" + open + close + "]");
    Diags.assertSingleError(tooDeep, DiagnosticCode.JSON_TOO_DEEP);
  }

  @Test
  void provisionalDepthIsPerBranchAndNotAccumulatedOverSiblings() {
    // spec: R57: closing a container gives its level back
    String text = "[".repeat(254) + "[],[],{}" + "]".repeat(254);
    Jx.parse(text);
    Jx.parse("[" + "[[[]]],".repeat(1000) + "[]]");
  }

  @Test
  void provisionalTooDeepIsReportedWithoutReadingTheRest() {
    // spec: R57 (the refusal happens at the opening, before the rest is read)
    Outcome<JsonNode> outcome = Jx.read("[".repeat(256) + "@@@ not json {{{ \"");
    Diags.assertSingleError(outcome, DiagnosticCode.JSON_TOO_DEEP);
  }

  @Test
  @Timeout(30)
  void provisionalHundredThousandLevelsGiveOneDiagnosticAndNoStackError() {
    // spec: VJ5, R57
    Outcome<JsonNode> outcome = Jx.read("[".repeat(100_000) + "]".repeat(100_000));
    Diags.assertSingleError(outcome, DiagnosticCode.JSON_TOO_DEEP);
  }

  @Test
  @Timeout(30)
  void provisionalHundredThousandUnclosedLevelsAreAlsoTooDeepNotAStackError() {
    // spec: R57 (no matter the depth of the text, closed or not)
    Outcome<JsonNode> outcome = Jx.read("{\"a\":[".repeat(100_000));
    Diags.assertSingleError(outcome, DiagnosticCode.JSON_TOO_DEEP);
  }

  @Test
  @Timeout(30)
  void aWideFileReadsInReasonableTime() {
    // spec: R49 (largest data file is 60159 bytes); well beyond that here
    StringBuilder text = new StringBuilder("[");
    for (int i = 0; i < 100_000; i++) {
      text.append(i == 0 ? "" : ",").append(i).append(".5");
    }
    text.append(']');
    JsonArray array = (JsonArray) Jx.parse(text.toString());
    assertThat(array.elements()).hasSize(100_000);
  }

  @Test
  void stringOverloadFollowsTheSameRulesAsTheByteOverload() {
    // spec: JsonReader.read(String, FileOrigin) "same rules as the byte overload"
    String text = "{\"a\":[1,2.5,\"x\"],\"b\":{\"c\":null}}";
    Outcome<JsonNode> fromString = JsonReader.read(text, Jx.ORIGIN);
    Outcome<JsonNode> fromBytes = Jx.read(text);
    assertThat(fromString.diagnostics()).isEmpty();
    assertThat(fromString.value().orElseThrow().sameValueAs(fromBytes.value().orElseThrow()))
        .isTrue();
    Diags.assertSingleError(JsonReader.read("{,}", Jx.ORIGIN), DiagnosticCode.JSON_SYNTAX);
    Diags.assertSingleError(JsonReader.read("", Jx.ORIGIN), DiagnosticCode.JSON_EMPTY);
  }

  @Test
  void provisionalStringOverloadRefusesALeadingByteOrderMarkCharacter() {
    // spec: JsonReader.read(String, FileOrigin) Javadoc (provisional): leading U+FEFF is JSON_BOM
    Diags.assertSingleError(JsonReader.read("\ufeff{}", Jx.ORIGIN), DiagnosticCode.JSON_BOM);
  }

  @Test
  void provisionalStringOverloadRefusesAnUnpairedSurrogateInTheText() {
    // spec: JsonReader.read(String, FileOrigin) Javadoc (provisional): JSON_UTF8, as the bytes
    // would be
    Diags.assertSingleError(
        JsonReader.read("{\"a\":\"\ud800\"}", Jx.ORIGIN), DiagnosticCode.JSON_UTF8);
    Diags.assertSingleError(
        JsonReader.read("[\"\ude00\ud83d\"]", Jx.ORIGIN), DiagnosticCode.JSON_UTF8);
    assertThat(JsonReader.read("[\"\ud83d\ude00\"]", Jx.ORIGIN).diagnostics()).isEmpty();
  }

  @Test
  void provisionalAValidReadHasOnlyTheFileOriginGiven() {
    // spec: JsonReader Javadoc ("attached to every node and diagnostic")
    Outcome<JsonNode> outcome = Jx.read("{\"a\":1,\"a\":2}");
    assertThat(outcome.diagnostics()).allSatisfy(d -> assertThat(d.origin()).isEqualTo(Jx.ORIGIN));
  }
}
