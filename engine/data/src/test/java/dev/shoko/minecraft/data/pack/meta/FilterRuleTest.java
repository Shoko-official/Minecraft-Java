package dev.shoko.minecraft.data.pack.meta;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.testing.Expect;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R47 (filter.block rules), probe P15
class FilterRuleTest {

  @Test
  void provisionalMatchAllIsTheExpressionOfAnAbsentField() {
    // spec: R47 ("une expression absente correspond a tout")
    assertThat(FilterRule.MATCH_ALL).isEqualTo(".*");
  }

  @Test
  void theExpressionsAreKeptAsWritten() {
    FilterRule rule = FilterRule.of("minecraft", "loot_table/.*");
    assertThat(rule.namespacePattern()).isEqualTo("minecraft");
    assertThat(rule.pathPattern()).isEqualTo("loot_table/.*");
  }

  @Test
  void provisionalBothExpressionsMustMatch() {
    // spec: R47
    FilterRule rule = FilterRule.of("minecraft", "loot_table/blocks/dirt\\.json");
    assertThat(rule.matches("minecraft", "loot_table/blocks/dirt.json")).isTrue();
    assertThat(rule.matches("other", "loot_table/blocks/dirt.json")).isFalse();
    assertThat(rule.matches("minecraft", "loot_table/blocks/stone.json")).isFalse();
    assertThat(rule.matches("other", "loot_table/blocks/stone.json")).isFalse();
  }

  @Test
  void provisionalTheWholeTextMustMatch() {
    // spec: R47 ("correspondance complete de l'expression"), probe P15 variants
    FilterRule withExtension = FilterRule.of("minecraft", "loot_table/blocks/dirt.json");
    FilterRule withoutExtension = FilterRule.of("minecraft", "loot_table/blocks/dirt");
    FilterRule startsAtBlocks = FilterRule.of("minecraft", "blocks/dirt");
    assertThat(withExtension.matches("minecraft", "loot_table/blocks/dirt.json")).isTrue();
    assertThat(withoutExtension.matches("minecraft", "loot_table/blocks/dirt.json")).isFalse();
    assertThat(startsAtBlocks.matches("minecraft", "loot_table/blocks/dirt.json")).isFalse();
    assertThat(FilterRule.of("min", ".*").matches("minecraft", "x")).isFalse();
    assertThat(FilterRule.of("craft", ".*").matches("minecraft", "x")).isFalse();
  }

  @Test
  void provisionalWildcardsMatchSubFolders() {
    // spec: R47, probe P15 variant "loot_table/.*"
    FilterRule rule = FilterRule.of("minecraft", "loot_table/.*");
    assertThat(rule.matches("minecraft", "loot_table/blocks/dirt.json")).isTrue();
    assertThat(rule.matches("minecraft", "loot_table/a.json")).isTrue();
    assertThat(rule.matches("minecraft", "recipe/a.json")).isFalse();
  }

  @Test
  void provisionalAnAbsentExpressionMatchesAnything() {
    // spec: R47
    FilterRule onlyNamespace = FilterRule.of("minecraft", FilterRule.MATCH_ALL);
    FilterRule onlyPath = FilterRule.of(FilterRule.MATCH_ALL, "tags/.*");
    FilterRule both = FilterRule.of(FilterRule.MATCH_ALL, FilterRule.MATCH_ALL);
    assertThat(onlyNamespace.matches("minecraft", "any/thing.json")).isTrue();
    assertThat(onlyNamespace.matches("other", "any/thing.json")).isFalse();
    assertThat(onlyPath.matches("whatever", "tags/block/logs.json")).isTrue();
    assertThat(onlyPath.matches("whatever", "loot_table/a.json")).isFalse();
    assertThat(both.matches("a", "b")).isTrue();
    assertThat(both.matches("a", "")).isTrue();
  }

  @Test
  void provisionalRegularExpressionSyntaxIsSupported() {
    // spec: R4 ("expressions regulieres"), R47
    FilterRule rule = FilterRule.of("(minecraft|sonde)", "[a-z_]+/[0-9]+\\.json");
    assertThat(rule.matches("sonde", "recipe/12.json")).isTrue();
    assertThat(rule.matches("minecraft", "recipe/x.json")).isFalse();
    assertThat(rule.matches("other", "recipe/12.json")).isFalse();
  }

  @Test
  void anInvalidExpressionIsRefusedWhenTheRuleIsBuilt() {
    // spec: FilterRule.of Javadoc
    assertThat(Expect.thrown(() -> FilterRule.of("(", ".*")))
        .isInstanceOf(PatternSyntaxException.class);
    assertThat(Expect.thrown(() -> FilterRule.of(".*", "[a-")))
        .isInstanceOf(PatternSyntaxException.class);
  }
}
