package dev.shoko.minecraft.loot.enchant.craft;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.DrawLog;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.Sets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: EN-R35 to EN-R39 (candidates, weighted choice, continuation), probe PE1. The costs of the
// made-up enchantments are chosen so that the expected candidates are computed by hand.
class EnchantmentSelectionTest {

  private static LootData data() {
    return Ctx.data();
  }

  private static Map<String, Integer> byName(List<EnchantmentInstance> instances) {
    Map<String, Integer> map = new LinkedHashMap<>();
    for (EnchantmentInstance instance : instances) {
      map.put(instance.enchantment().id().path(), instance.level());
    }
    return map;
  }

  private static List<EnchantmentInstance> possible(
      ItemStack stack, int cost, IdOrTagSet options, boolean primaryOnly) {
    return EnchantmentSelection.possible(stack, cost, options, primaryOnly, data());
  }

  private static LootContext context(long seed) {
    return Ctx.builder(Lcg48Random.ofSeed(seed)).build();
  }

  @Test
  void provisionalABookAcceptsEveryEnchantmentAtTheHighestLevelOfTheCostRange() {
    // spec: EN-R38 (highest level L with min_cost(L) <= cost <= max_cost(L); a book is primary for
    // all)
    Map<String, Integer> candidates =
        byName(possible(LootItems.stack("book"), 30, Sets.anyEnchantment(), true));
    Map<String, Integer> expected = new LinkedHashMap<>();
    expected.put("sharpness", 3);
    expected.put("smite", 4);
    expected.put("bane_of_arthropods", 4);
    expected.put("protection", 3);
    expected.put("fire_protection", 3);
    expected.put("unbreaking", 3);
    expected.put("mending", 1);
    expected.put("infinity", 1);
    expected.put("fortune", 2);
    expected.put("silk_touch", 1);
    expected.put("looting", 2);
    expected.put("fire_aspect", 2);
    expected.put("efficiency", 3);
    expected.put("binding_curse", 1);
    expected.put("riptide", 2);
    expected.put("loyalty", 3);
    expected.put("channeling", 1);
    assertThat(candidates).containsExactlyInAnyOrderEntriesOf(expected);
  }

  @Test
  void provisionalACandidateWithNoValidLevelIsDropped() {
    // spec: EN-R38
    Map<String, Integer> low =
        byName(possible(LootItems.stack("book"), 1, Sets.anyEnchantment(), true));
    assertThat(low)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of("sharpness", 1, "protection", 1, "efficiency", 1));
    assertThat(possible(LootItems.stack("book"), 100, Sets.anyEnchantment(), true)).isEmpty();
    assertThat(possible(LootItems.stack("book"), 0, Sets.anyEnchantment(), true)).isEmpty();
  }

  @Test
  void provisionalTheOptionsRestrictTheCandidates() {
    // spec: EN-R38, EN-R35
    Map<String, Integer> only =
        byName(possible(LootItems.stack("book"), 30, Sets.enchantments("smite"), true));
    assertThat(only).containsExactly(Map.entry("smite", 4));
    Map<String, Integer> tag =
        byName(possible(LootItems.stack("book"), 30, Sets.enchantmentTag("curse"), true));
    assertThat(tag).containsExactly(Map.entry("binding_curse", 1));
  }

  @Test
  void provisionalTheTablePrimaryItemsRestrictWhatTheTableOffers() {
    // spec: EN-R7b, EN-R38 (primary_items for the table, supported_items otherwise)
    ItemStack axe = LootItems.stack("diamond_axe");
    assertThat(byName(possible(axe, 1, Sets.anyEnchantment(), true))).isEmpty();
    assertThat(byName(possible(axe, 1, Sets.anyEnchantment(), false)))
        .containsExactly(Map.entry("sharpness", 1));
    assertThat(byName(possible(LootItems.stack("diamond_sword"), 1, Sets.anyEnchantment(), true)))
        .containsExactly(Map.entry("sharpness", 1));
  }

  @Test
  void provisionalAnItemOnlyGetsWhatItSupports() {
    // spec: EN-R7a
    Map<String, Integer> sword =
        byName(possible(LootItems.stack("diamond_sword"), 30, Sets.anyEnchantment(), false));
    assertThat(sword.keySet())
        .containsExactlyInAnyOrder(
            "sharpness",
            "smite",
            "bane_of_arthropods",
            "unbreaking",
            "mending",
            "looting",
            "fire_aspect");
    Map<String, Integer> bow =
        byName(possible(LootItems.stack("bow"), 30, Sets.anyEnchantment(), false));
    assertThat(bow.keySet()).containsExactlyInAnyOrder("unbreaking", "mending", "infinity");
  }

  @Test
  void provisionalTheCandidatesFollowTheRegistryOrder() {
    // spec: EnchantmentSelection#possible contract (in registry order)
    List<String> order =
        byName(possible(LootItems.stack("book"), 30, Sets.anyEnchantment(), true)).keySet().stream()
            .toList();
    List<String> registry =
        data().enchantments().definitions().keySet().stream().map(Identifier::path).toList();
    assertThat(registry).containsSubsequence(order);
  }

  @Test
  void provisionalChoosingAmongNoCandidateGivesNothing() {
    // spec: EnchantmentSelection#choose contract (empty when there is no candidate)
    assertThat(EnchantmentSelection.choose(List.of(), 30, Lcg48Random.ofSeed(1), data())).isEmpty();
  }

  @Test
  void provisionalOneCandidateIsChosenWhatTheSource() {
    // spec: EN-R39
    List<EnchantmentInstance> only =
        possible(LootItems.stack("book"), 30, Sets.enchantments("smite"), true);
    for (long seed = 1; seed <= 20; seed++) {
      assertThat(byName(EnchantmentSelection.choose(only, 30, Lcg48Random.ofSeed(seed), data())))
          .containsExactly(Map.entry("smite", 4));
    }
  }

  @Test
  void provisionalIncompatibleCandidatesNeverAppearTogether() {
    // spec: EN-R39, EN-R8
    List<EnchantmentInstance> damage =
        possible(LootItems.stack("book"), 49, Sets.enchantmentTag("exclusive_set/damage"), true);
    assertThat(damage).hasSize(3);
    for (long seed = 1; seed <= 100; seed++) {
      assertThat(EnchantmentSelection.choose(damage, 49, Lcg48Random.ofSeed(seed), data()))
          .hasSize(1);
    }
  }

  @Test
  void provisionalAHighCostKeepsChoosingWhileCompatibleCandidatesRemain() {
    // spec: EN-R39 (continuation with (cost + 1) / 50 = 1 at cost 49: provisional, to be confirmed
    // by PE1)
    List<EnchantmentInstance> all =
        possible(
            LootItems.stack("diamond_sword"),
            49,
            Sets.enchantments("sharpness", "unbreaking", "fire_aspect"),
            false);
    assertThat(all).hasSize(3);
    for (long seed = 1; seed <= 50; seed++) {
      assertThat(EnchantmentSelection.choose(all, 49, Lcg48Random.ofSeed(seed), data()).size())
          .isGreaterThanOrEqualTo(2);
    }
  }

  @Test
  void provisionalACostOfZeroRarelyContinues() {
    // spec: EN-R39 ((0 + 1) / 50 = 2 percent: provisional, to be confirmed by PE1)
    List<EnchantmentInstance> all =
        possible(
            LootItems.stack("diamond_sword"),
            30,
            Sets.enchantments("sharpness", "unbreaking", "fire_aspect"),
            false);
    int single = 0;
    for (long seed = 1; seed <= 300; seed++) {
      if (EnchantmentSelection.choose(all, 0, Lcg48Random.ofSeed(seed), data()).size() == 1) {
        single++;
      }
    }
    assertThat(single).isGreaterThan(270);
  }

  @Test
  void provisionalTheChoiceNeverRepeatsAnEnchantmentNorChangesItsLevel() {
    // spec: EN-R39
    List<EnchantmentInstance> all =
        possible(LootItems.stack("diamond_sword"), 40, Sets.anyEnchantment(), false);
    Map<String, Integer> levels = byName(all);
    for (long seed = 1; seed <= 100; seed++) {
      List<EnchantmentInstance> chosen =
          EnchantmentSelection.choose(all, 40, Lcg48Random.ofSeed(seed), data());
      List<String> names = chosen.stream().map(i -> i.enchantment().id().path()).toList();
      assertThat(names).doesNotHaveDuplicates();
      for (EnchantmentInstance instance : chosen) {
        assertThat(instance.level()).isEqualTo(levels.get(instance.enchantment().id().path()));
      }
    }
  }

  @Test
  void provisionalTheFirstChoiceFollowsTheWeights() {
    // spec: EN-R39: sharpness 10 against fire_aspect 2
    List<EnchantmentInstance> pair =
        possible(
            LootItems.stack("diamond_sword"),
            30,
            Sets.enchantments("sharpness", "fire_aspect"),
            false);
    int first = 0;
    int samples = 3000;
    for (long seed = 1; seed <= samples; seed++) {
      List<EnchantmentInstance> chosen =
          EnchantmentSelection.choose(pair, 0, Lcg48Random.ofSeed(seed), data());
      if (chosen.getFirst().enchantment().id().path().equals("sharpness")) {
        first++;
      }
    }
    assertThat(first / (double) samples).isBetween(0.79, 0.87);
  }

  @Test
  void provisionalTheSameSourceStateGivesTheSameChoice() {
    // spec: EN-R40, determinism
    List<EnchantmentInstance> all =
        possible(LootItems.stack("diamond_sword"), 40, Sets.anyEnchantment(), false);
    for (long seed = 1; seed <= 30; seed++) {
      assertThat(EnchantmentSelection.choose(all, 40, Lcg48Random.ofSeed(seed), data()))
          .isEqualTo(EnchantmentSelection.choose(all, 40, Lcg48Random.ofSeed(seed), data()));
    }
  }

  @Test
  void provisionalEnchantWithLevelsTurnsABookIntoAnEnchantedBook() {
    // spec: EN-R36, LT-R45
    ItemStack result =
        EnchantmentSelection.enchantWithLevels(
            LootItems.stack("book"), 30, Sets.anyEnchantment(), context(1));
    assertThat(result.type().id()).isEqualTo(Identifier.minecraft("enchanted_book"));
    assertThat(LootItems.storedOf(result)).isNotEmpty();
    assertThat(LootItems.enchantmentsOf(result)).isEmpty();
    assertThat(result.count()).isEqualTo(1);
  }

  @Test
  void provisionalEnchantWithLevelsEnchantsAnItemWithWhatItSupports() {
    // spec: EN-R36, EN-R38, EN-R8, EN-R10 (levels 1 to max_level)
    List<String> supported =
        List.of(
            "sharpness",
            "smite",
            "bane_of_arthropods",
            "unbreaking",
            "mending",
            "looting",
            "fire_aspect");
    for (long seed = 1; seed <= 60; seed++) {
      ItemStack result =
          EnchantmentSelection.enchantWithLevels(
              LootItems.stack("diamond_sword"), 30, Sets.anyEnchantment(), context(seed));
      Map<Identifier, Integer> levels = LootItems.enchantmentsOf(result);
      assertThat(levels).as("seed %d", seed).isNotEmpty();
      for (Map.Entry<Identifier, Integer> entry : levels.entrySet()) {
        assertThat(supported).contains(entry.getKey().path());
        int max = data().enchantments().find(entry.getKey()).orElseThrow().maxLevel();
        assertThat(entry.getValue()).isBetween(1, max);
      }
      assertThat(
              levels.keySet().stream()
                  .filter(
                      id -> List.of("sharpness", "smite", "bane_of_arthropods").contains(id.path()))
                  .count())
          .isLessThanOrEqualTo(1);
    }
  }

  @Test
  void provisionalEnchantWithLevelsLeavesAnItemWithoutEnchantableUnchanged() {
    // spec: EnchantmentSelection#enchantWithLevels contract
    ItemStack stone = LootItems.stack("stone", 3);
    assertThat(EnchantmentSelection.enchantWithLevels(stone, 30, Sets.anyEnchantment(), context(1)))
        .isEqualTo(stone);
  }

  @Test
  void provisionalEnchantWithLevelsHonoursTheOptions() {
    // spec: LT-R45 (options: restricts the candidates)
    for (long seed = 1; seed <= 30; seed++) {
      ItemStack result =
          EnchantmentSelection.enchantWithLevels(
              LootItems.stack("diamond_sword"), 30, Sets.enchantments("smite"), context(seed));
      assertThat(LootItems.enchantmentsOf(result).keySet())
          .containsOnly(Identifier.minecraft("smite"));
    }
  }

  @Test
  void provisionalEnchantWithLevelsReportsItsDraws() {
    // spec: DrawReason#ENCHANTMENT_SELECTION, EN-R35
    DrawLog log = new DrawLog();
    LootContext observed = Ctx.builder(Lcg48Random.ofSeed(4)).observer(log).build();
    EnchantmentSelection.enchantWithLevels(
        LootItems.stack("diamond_sword"), 30, Sets.anyEnchantment(), observed);
    assertThat(log.reasons()).contains(DrawReason.ENCHANTMENT_SELECTION);
  }

  @Test
  void provisionalEnchantWithLevelsIsDeterministicForASeed() {
    // spec: EN-R40, determinism
    List<ItemStack> seen = new ArrayList<>();
    for (int run = 0; run < 2; run++) {
      seen.add(
          EnchantmentSelection.enchantWithLevels(
              LootItems.stack("diamond_sword"), 30, Sets.anyEnchantment(), context(123)));
    }
    assertThat(seen.get(0)).isEqualTo(seen.get(1));
  }

  @Test
  void provisionalEnchantWithLevelsToleratesALevelFarAboveAnyCost() {
    // spec: EN-R36 (levels 50 are used in the data)
    ItemStack result =
        EnchantmentSelection.enchantWithLevels(
            LootItems.stack("diamond_sword"), 1000, Sets.anyEnchantment(), context(1));
    assertThat(result.type().id()).isEqualTo(Identifier.minecraft("diamond_sword"));
  }
}
