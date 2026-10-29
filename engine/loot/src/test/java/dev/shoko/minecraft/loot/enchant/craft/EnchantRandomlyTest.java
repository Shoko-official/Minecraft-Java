package dev.shoko.minecraft.loot.enchant.craft;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.modifier.ItemModifier;
import dev.shoko.minecraft.loot.modifier.ModifierAction;
import dev.shoko.minecraft.loot.modifier.ModifierApplier;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.Sets;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: EN-R35 (enchant_randomly: uniform choice among the compatible candidates, then a uniform
// level), LT-R45, probes PL13 and PE2. Fixed seeds, wide thresholds.
class EnchantRandomlyTest {

  private static final List<String> SWORD_OPTIONS =
      List.of(
          "sharpness",
          "smite",
          "bane_of_arthropods",
          "unbreaking",
          "mending",
          "looting",
          "fire_aspect");

  private static LootContext context(long seed) {
    return Ctx.builder(Lcg48Random.ofSeed(seed)).build();
  }

  private static ItemStack randomly(
      ItemStack stack, IdOrTagSet options, boolean compatible, long seed) {
    return EnchantmentSelection.enchantRandomly(stack, options, compatible, context(seed));
  }

  @Test
  void provisionalABookBecomesAnEnchantedBookWithOneEnchantment() {
    // spec: EN-R35
    for (long seed = 1; seed <= 40; seed++) {
      ItemStack result =
          randomly(LootItems.stack("book"), Sets.enchantmentTag("non_treasure"), true, seed);
      assertThat(result.type().id()).isEqualTo(Identifier.minecraft("enchanted_book"));
      Map<Identifier, Integer> stored = LootItems.storedOf(result);
      assertThat(stored).hasSize(1);
      Map.Entry<Identifier, Integer> only = stored.entrySet().iterator().next();
      int max = Ctx.data().enchantments().find(only.getKey()).orElseThrow().maxLevel();
      assertThat(only.getValue()).isBetween(1, max);
    }
  }

  @Test
  void provisionalAnItemOnlyGetsAnEnchantmentItSupports() {
    // spec: EN-R35
    for (long seed = 1; seed <= 200; seed++) {
      ItemStack result =
          randomly(LootItems.stack("diamond_sword"), Sets.anyEnchantment(), true, seed);
      Map<Identifier, Integer> levels = LootItems.enchantmentsOf(result);
      assertThat(levels).hasSize(1);
      assertThat(SWORD_OPTIONS).contains(levels.keySet().iterator().next().path());
    }
  }

  @Test
  void provisionalNoCandidateLeavesTheItemUnchanged() {
    // spec: EnchantmentSelection#enchantRandomly contract (unchanged when there is no candidate)
    ItemStack sword = LootItems.stack("diamond_sword");
    assertThat(randomly(sword, Sets.enchantments("protection"), true, 1)).isEqualTo(sword);
  }

  @Test
  void provisionalAnIncompatibleCandidateIsNeverChosenWhenOnlyCompatible() {
    // spec: EN-R35, EN-R8
    ItemStack sword =
        LootItems.enchanted(LootItems.stack("diamond_sword"), Map.of("minecraft:sharpness", 2));
    for (long seed = 1; seed <= 150; seed++) {
      ItemStack result = randomly(sword, Sets.enchantments("smite", "unbreaking"), true, seed);
      Map<Identifier, Integer> levels = LootItems.enchantmentsOf(result);
      assertThat(levels.keySet().stream().map(Identifier::path).toList())
          .doesNotContain("smite")
          .contains("sharpness");
    }
  }

  @Test
  void provisionalTheChoiceIgnoresTheWeightOfTheEnchantments() {
    // spec: EN-R35: sharpness 10 against mending 2
    Map<String, Integer> counts = new HashMap<>();
    int samples = 3000;
    for (long seed = 1; seed <= samples; seed++) {
      ItemStack result =
          randomly(
              LootItems.stack("diamond_sword"),
              Sets.enchantments("sharpness", "mending"),
              true,
              seed);
      String name = LootItems.enchantmentsOf(result).keySet().iterator().next().path();
      counts.merge(name, 1, Integer::sum);
    }
    assertThat(counts.get("sharpness") / (double) samples).isBetween(0.45, 0.55);
    assertThat(counts.get("mending") / (double) samples).isBetween(0.45, 0.55);
  }

  @Test
  void provisionalTheLevelIsUniformBetweenOneAndTheMaximum() {
    // spec: EN-R35
    Map<Integer, Integer> counts = new HashMap<>();
    int samples = 3000;
    for (long seed = 1; seed <= samples; seed++) {
      ItemStack result =
          randomly(LootItems.stack("diamond_sword"), Sets.enchantments("unbreaking"), true, seed);
      counts.merge(
          LootItems.enchantmentsOf(result).get(Identifier.minecraft("unbreaking")),
          1,
          Integer::sum);
    }
    assertThat(counts.keySet()).containsExactlyInAnyOrder(1, 2, 3);
    for (int level = 1; level <= 3; level++) {
      assertThat(counts.get(level) / (double) samples).as("level %d", level).isBetween(0.28, 0.39);
    }
  }

  @Test
  void provisionalTheModifierEnchantRandomlyAppliesTheSelection() {
    // spec: LT-R45 (enchant_randomly as an item modifier), probe PL13
    ItemModifier modifier =
        Tables.modifier(
            new ModifierAction.EnchantRandomly(Sets.enchantmentTag("non_treasure"), true, false));
    ItemStack result = ModifierApplier.apply(modifier, LootItems.stack("book"), context(5)).stack();
    assertThat(result.type().id()).isEqualTo(Identifier.minecraft("enchanted_book"));
    assertThat(LootItems.storedOf(result)).hasSize(1);
  }

  @Test
  void provisionalTheModifierEnchantWithLevelsDrawsItsLevelsFirst() {
    // spec: LT-R45 (levels: provider drawn first), EN-R36
    ItemModifier modifier =
        Tables.modifier(
            new ModifierAction.EnchantWithLevels(
                Tables.uniform(20, 39), Sets.anyEnchantment(), false));
    int enchanted = 0;
    for (long seed = 1; seed <= 40; seed++) {
      ItemStack result =
          ModifierApplier.apply(modifier, LootItems.stack("diamond_sword"), context(seed)).stack();
      if (!LootItems.enchantmentsOf(result).isEmpty()) {
        enchanted++;
      }
    }
    assertThat(enchanted).isEqualTo(40);
  }
}
