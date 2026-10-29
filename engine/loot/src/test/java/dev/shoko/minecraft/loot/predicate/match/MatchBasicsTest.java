package dev.shoko.minecraft.loot.predicate.match;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.testing.Blocks;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.Sets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: LT-R35 (ranges), LT-R34 (block predicate), LT-R58 (item predicate), IdOrTagSet contract
class MatchBasicsTest {

  private static RegistryAccess access() {
    return EnchantFixtures.access();
  }

  private static ItemMatch itemMatch(IdOrTagSet items, NumberRange count) {
    return new ItemMatch(items, count, new LinkedHashMap<>(), new LinkedHashMap<>());
  }

  private static ItemMatch withPredicate(String component, String relaxedJson) {
    SequencedMap<Identifier, JsonNode> predicates = new LinkedHashMap<>();
    predicates.put(Identifier.parse(component), Json.parse(relaxedJson));
    return new ItemMatch(Sets.anyItem(), Sets.anyNumber(), predicates, new LinkedHashMap<>());
  }

  private static BlockMatch block(IdOrTagSet blocks, String property, String value) {
    SequencedMap<String, String> state = new LinkedHashMap<>();
    state.put(property, value);
    return new BlockMatch(blocks, state);
  }

  @Test
  void aRangeIsInclusiveAtBothEnds() {
    // spec: LT-R35, LT-R58 (min and max inclusive)
    NumberRange range = Sets.between(1, 3);
    assertThat(range.contains(1)).isTrue();
    assertThat(range.contains(3)).isTrue();
    assertThat(range.contains(2.5)).isTrue();
    assertThat(range.contains(0.999)).isFalse();
    assertThat(range.contains(3.001)).isFalse();
  }

  @Test
  void anAbsentBoundIsInfinite() {
    // spec: NumberRange contract ({"min": 1} has no upper bound)
    assertThat(Sets.atLeast(1).contains(1_000_000)).isTrue();
    assertThat(Sets.atLeast(1).contains(0)).isFalse();
    assertThat(Sets.anyNumber().contains(-1e300)).isTrue();
  }

  @Test
  void aSetContainsItsListedIdentifiers() {
    // spec: IdOrTagSet contract
    IdOrTagSet set = Sets.items("diamond", "stick");
    assertThat(set.contains(Identifier.minecraft("diamond"), access())).isTrue();
    assertThat(set.contains(Identifier.minecraft("stick"), access())).isTrue();
    assertThat(set.contains(Identifier.minecraft("dirt"), access())).isFalse();
  }

  @Test
  void aSetContainsTheMembersOfItsTags() {
    // spec: IdOrTagSet contract, LT-R22 (tags are looked up through the registries)
    IdOrTagSet set = Sets.itemTag("test_gems");
    assertThat(set.contains(Identifier.minecraft("emerald"), access())).isTrue();
    assertThat(set.contains(Identifier.minecraft("iron_ingot"), access())).isTrue();
    assertThat(set.contains(Identifier.minecraft("stick"), access())).isFalse();
  }

  @Test
  void anUnrestrictedSetContainsEverything() {
    // spec: IdOrTagSet contract (key absent: everything of the registry matches)
    assertThat(Sets.anyItem().contains(Identifier.minecraft("dirt"), access())).isTrue();
  }

  @Test
  void membersListTheRegistryOrderWithTagsExpandedAndNoDuplicate() {
    // spec: IdOrTagSet#members contract, LT-R22 (expand: a tag inside a set)
    IdOrTagSet set =
        new IdOrTagSet(
            Identifier.minecraft("item"),
            List.of(Identifier.minecraft("emerald"), Identifier.minecraft("stick")),
            List.of(Identifier.minecraft("test_gems")),
            false);
    assertThat(set.members(access()))
        .containsExactly(
            Identifier.minecraft("stick"),
            Identifier.minecraft("diamond"),
            Identifier.minecraft("emerald"),
            Identifier.minecraft("iron_ingot"));
  }

  @Test
  void membersOfAnEmptyTagAreEmpty() {
    // spec: LT-R22 (tag expand: empty tag), cas limite
    assertThat(Sets.itemTag("empty_tag").members(access())).isEmpty();
  }

  @Test
  void membersOfAnUnrestrictedSetAreTheWholeRegistry() {
    // spec: IdOrTagSet#members contract
    assertThat(Sets.anyItem().members(access())).hasSize(LootItems.catalog().size());
  }

  @Test
  void provisionalABlockPredicateMatchesTheBlockAndEachListedProperty() {
    // spec: LT-R34
    BlockMatch ripe = block(Sets.blocks("wheat"), "age", "7");
    assertThat(ripe.matches(Blocks.wheat(7), access())).isTrue();
    assertThat(ripe.matches(Blocks.wheat(6), access())).isFalse();
    assertThat(ripe.matches(Blocks.stone(), access())).isFalse();
  }

  @Test
  void provisionalABlockPredicateWithoutStateOnlyChecksTheBlock() {
    // spec: LT-R34
    BlockMatch anyWheat = new BlockMatch(Sets.blocks("wheat"), new LinkedHashMap<>());
    assertThat(anyWheat.matches(Blocks.wheat(0), access())).isTrue();
    assertThat(anyWheat.matches(Blocks.wheat(7), access())).isTrue();
    assertThat(anyWheat.matches(Blocks.pot(false), access())).isFalse();
  }

  @Test
  void provisionalAListedPropertyThatTheBlockDoesNotHaveDoesNotMatch() {
    // spec: BlockMatch#matches contract, cas limite
    BlockMatch odd = block(Sets.blocks("wheat"), "cracked", "true");
    assertThat(odd.matches(Blocks.wheat(7), access())).isFalse();
  }

  @Test
  void provisionalACrackedPotIsToldFromAnIntactOne() {
    // spec: V-L8 (decorated_pot: match_block cracked "true")
    BlockMatch cracked = block(Sets.blocks("decorated_pot"), "cracked", "true");
    assertThat(cracked.matches(Blocks.pot(true), access())).isTrue();
    assertThat(cracked.matches(Blocks.pot(false), access())).isFalse();
  }

  @Test
  void provisionalAnItemPredicateComparesTheItem() {
    // spec: LT-R58
    ItemMatch shears = itemMatch(Sets.items("shears"), Sets.anyNumber());
    assertThat(shears.matches(LootItems.stack("shears"), access())).isTrue();
    assertThat(shears.matches(LootItems.stack("stick"), access())).isFalse();
  }

  @Test
  void provisionalAnItemPredicateAcceptsATagOfItems() {
    // spec: LT-R58 (items: identifier, tag or list)
    ItemMatch gems = itemMatch(Sets.itemTag("test_gems"), Sets.anyNumber());
    assertThat(gems.matches(LootItems.stack("emerald"), access())).isTrue();
    assertThat(gems.matches(LootItems.stack("stick"), access())).isFalse();
  }

  @Test
  void anEmptyStackMatchesNothing() {
    // spec: ItemMatch#matches contract
    assertThat(itemMatch(Sets.anyItem(), Sets.anyNumber()).matches(ItemStack.empty(), access()))
        .isFalse();
  }

  @Test
  void provisionalAnItemPredicateChecksTheCountRange() {
    // spec: LT-R58 (count: interval)
    ItemMatch three = itemMatch(Sets.anyItem(), Sets.between(3, 7));
    assertThat(three.matches(LootItems.stack("stick", 5), access())).isTrue();
    assertThat(three.matches(LootItems.stack("stick", 2), access())).isFalse();
    assertThat(three.matches(LootItems.stack("stick", 8), access())).isFalse();
  }

  @Test
  void provisionalAnEnchantmentsPredicateChecksTheLevels() {
    // spec: LT-R58, LT-R61a (tool/can_silk_touch: enchantments with levels min 1)
    ItemMatch silk =
        withPredicate(
            "minecraft:enchantments",
            "[{'enchantments':'minecraft:silk_touch','levels':{'min':1}}]");
    ItemStack silkPick =
        LootItems.enchanted(LootItems.stack("iron_pickaxe"), Map.of("minecraft:silk_touch", 1));
    ItemStack fortunePick =
        LootItems.enchanted(LootItems.stack("iron_pickaxe"), Map.of("minecraft:fortune", 3));
    assertThat(silk.matches(silkPick, access())).isTrue();
    assertThat(silk.matches(fortunePick, access())).isFalse();
    assertThat(silk.matches(LootItems.stack("iron_pickaxe"), access())).isFalse();
  }

  @Test
  void provisionalAnEnchantmentsPredicateCanDemandAMinimumLevel() {
    // spec: LT-R58 (levels: interval)
    ItemMatch atLeastTwo =
        withPredicate(
            "minecraft:enchantments", "[{'enchantments':'minecraft:fortune','levels':{'min':2}}]");
    ItemStack one =
        LootItems.enchanted(LootItems.stack("iron_pickaxe"), Map.of("minecraft:fortune", 1));
    ItemStack three =
        LootItems.enchanted(LootItems.stack("iron_pickaxe"), Map.of("minecraft:fortune", 3));
    assertThat(atLeastTwo.matches(one, access())).isFalse();
    assertThat(atLeastTwo.matches(three, access())).isTrue();
  }

  @Test
  void provisionalAComponentPredicateNeedsThePresenceOfTheComponent() {
    // spec: LT-R58, probe PL15
    ItemMatch hasMap = withPredicate("minecraft:map_id", "{}");
    assertThat(hasMap.matches(LootItems.stack("stick"), access())).isFalse();
  }
}
