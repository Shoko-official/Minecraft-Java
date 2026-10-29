package dev.shoko.minecraft.data.diag;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.testing.Expect;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R61 a (format of the path of a diagnostic)
class JsonPathTest {

  @Test
  void provisionalRootIsDollar() {
    assertThat(JsonPath.root().toString()).isEqualTo("$");
  }

  @Test
  void provisionalMembersAreDotted() {
    assertThat(JsonPath.root().field("a").field("b").toString()).isEqualTo("$.a.b");
  }

  @Test
  void provisionalIndicesAreBracketedFromZero() {
    assertThat(JsonPath.root().index(0).toString()).isEqualTo("$[0]");
    assertThat(JsonPath.root().field("values").index(2).field("id").toString())
        .isEqualTo("$.values[2].id");
    assertThat(JsonPath.root().index(1).index(10).toString()).isEqualTo("$[1][10]");
  }

  @Test
  void provisionalAMemberNameIsWrittenAsDecodedWithoutQuotes() {
    // spec: R61 a: no escaping, no quotes
    assertThat(JsonPath.root().field("minecraft:block").toString()).isEqualTo("$.minecraft:block");
    assertThat(JsonPath.root().field("a b").toString()).isEqualTo("$.a b");
    assertThat(JsonPath.root().field("a\"b").toString()).isEqualTo("$.a\"b");
  }

  @Test
  void provisionalNegativeIndexIsRefused() {
    Expect.illegalArgument(() -> JsonPath.root().index(-1));
  }

  @Test
  void provisionalPathsAreImmutableValues() {
    JsonPath root = JsonPath.root();
    JsonPath child = root.field("a");
    assertThat(root.toString()).isEqualTo("$");
    assertThat(child.toString()).isEqualTo("$.a");
    assertThat(root.field("b").toString()).isEqualTo("$.b");
  }

  @Test
  void provisionalEqualityComparesTheSteps() {
    assertThat(JsonPath.root()).isEqualTo(JsonPath.root());
    assertThat(JsonPath.root().field("a").index(1)).isEqualTo(JsonPath.root().field("a").index(1));
    assertThat(JsonPath.root().field("a").index(1).hashCode())
        .isEqualTo(JsonPath.root().field("a").index(1).hashCode());
    assertThat(JsonPath.root().field("a")).isNotEqualTo(JsonPath.root().field("b"));
    assertThat(JsonPath.root().index(1)).isNotEqualTo(JsonPath.root().index(2));
    assertThat(JsonPath.root().field("a")).isNotEqualTo(JsonPath.root());
  }

  @Test
  void provisionalAMemberNamedLikeAnIndexDiffersFromTheIndex() {
    // the steps differ even though the texts are alike: "$[0]" is not the member "[0]" of the root
    JsonPath member = JsonPath.root().field("[0]");
    JsonPath element = JsonPath.root().index(0);
    assertThat(member).isNotEqualTo(element);
  }

  @Test
  void provisionalANameWithADotGivesTheTextOfTwoStepsButIsNotEqualToThem() {
    // spec: JsonPath Javadoc: equals compares the steps, not the text
    JsonPath single = JsonPath.root().field("a.b");
    JsonPath twoSteps = JsonPath.root().field("a").field("b");
    assertThat(single.toString()).isEqualTo(twoSteps.toString());
    assertThat(single).isNotEqualTo(twoSteps);
  }

  @Test
  void provisionalDeepPathsAreSupported() {
    JsonPath path = JsonPath.root();
    StringBuilder expected = new StringBuilder("$");
    for (int i = 0; i < 255; i++) {
      path = path.index(0);
      expected.append("[0]");
    }
    assertThat(path.toString()).isEqualTo(expected.toString());
  }
}
