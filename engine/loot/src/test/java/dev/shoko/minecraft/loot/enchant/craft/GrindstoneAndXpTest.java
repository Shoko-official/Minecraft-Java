package dev.shoko.minecraft.loot.enchant.craft;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.EnchantmentLevels;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.EnchantmentProvider;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Sets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: EN-R44a (grindstone), EN-R44b (repair by experience, mending multiplies by 2), EN-R34
// (providers), probe PE4. The formula of the experience of the grindstone and the merging of two
// items are open points: only what the spec states is asserted.
class GrindstoneAndXpTest {

  private static LootData data() {
    return Ctx.data();
  }

  private static final Identifier CUSTOM_NAME = Identifier.minecraft("custom_name");

  private static ItemStack named(ItemStack stack) {
    return stack.withComponent(CUSTOM_NAME, new ComponentValue(new StringTag("Keep")));
  }

  private static LootContext context() {
    return Ctx.context(Lcg48Random.ofSeed(1));
  }

  @Test
  void provisionalTheGrindstoneRemovesTheEnchantmentsThatAreNotCurses() {
    // spec: EN-R44a
    ItemStack input =
        LootItems.enchanted(
            LootItems.stack("diamond_sword"),
            Map.of("minecraft:sharpness", 3, "minecraft:unbreaking", 2));
    GrindResult result = GrindstoneCalculator.grind(input, ItemStack.empty(), data()).orElseThrow();
    assertThat(LootItems.enchantmentsOf(result.output())).isEmpty();
    assertThat(result.output().type().id()).isEqualTo(Identifier.minecraft("diamond_sword"));
  }

  @Test
  void provisionalTheGrindstoneKeepsTheCurses() {
    // spec: EN-R44a, EN-R9a
    ItemStack input =
        LootItems.enchanted(
            LootItems.stack("diamond_chestplate"),
            Map.of("minecraft:protection", 2, "minecraft:binding_curse", 1));
    GrindResult result = GrindstoneCalculator.grind(input, ItemStack.empty(), data()).orElseThrow();
    assertThat(LootItems.enchantmentsOf(result.output()))
        .containsEntry(Identifier.minecraft("binding_curse"), 1)
        .doesNotContainKey(Identifier.minecraft("protection"));
  }

  @Test
  void provisionalTheGrindstoneResetsThePenaltyAndKeepsTheName() {
    // spec: EN-R44a
    ItemStack input =
        named(
            LootItems.withRepairCost(
                LootItems.enchanted(
                    LootItems.stack("diamond_sword"), Map.of("minecraft:sharpness", 3)),
                7));
    GrindResult result = GrindstoneCalculator.grind(input, ItemStack.empty(), data()).orElseThrow();
    assertThat(LootItems.repairCostOf(result.output())).isZero();
    assertThat(result.output().component(CUSTOM_NAME)).isEqualTo(input.component(CUSTOM_NAME));
  }

  @Test
  void provisionalTheGrindstoneGivesExperienceForTheRemovedEnchantments() {
    // spec: EN-R44a
    ItemStack input =
        LootItems.enchanted(LootItems.stack("diamond_sword"), Map.of("minecraft:sharpness", 3));
    GrindResult result = GrindstoneCalculator.grind(input, ItemStack.empty(), data()).orElseThrow();
    assertThat(result.experience()).isPositive();
  }

  @Test
  void provisionalTheGrindstoneGivesNothingWhenBothSlotsAreEmpty() {
    // spec: GrindstoneCalculator#grind contract
    assertThat(GrindstoneCalculator.grind(ItemStack.empty(), ItemStack.empty(), data())).isEmpty();
  }

  @Test
  void provisionalTheGrindstoneDoesNotChangeItsInput() {
    // spec: GrindstoneCalculator contract (a pure function)
    ItemStack input =
        LootItems.enchanted(LootItems.stack("diamond_sword"), Map.of("minecraft:sharpness", 3));
    Optional<GrindResult> first = GrindstoneCalculator.grind(input, ItemStack.empty(), data());
    Optional<GrindResult> second = GrindstoneCalculator.grind(input, ItemStack.empty(), data());
    assertThat(first).isEqualTo(second);
    assertThat(LootItems.enchantmentsOf(input)).containsEntry(Identifier.minecraft("sharpness"), 3);
  }

  private static ItemStack mendingPick(int damage) {
    return LootItems.damaged(
        LootItems.enchanted(LootItems.stack("iron_pickaxe"), Map.of("minecraft:mending", 1)),
        damage);
  }

  @Test
  void provisionalEachPointOfExperienceRepairsTheFinalValueOfRepairWithXp() {
    // spec: EN-R44b, EN-R19c, V-E19: 1 x 2.0 = 2 durability points per point, not 2 x 2.0
    RepairResult result = XpRepair.repair(mendingPick(100), 10, context());
    assertThat(result.stack().damage()).isEqualTo(80);
    assertThat(result.experienceLeft()).isZero();
  }

  @Test
  void provisionalTheExperienceNotUsedIsGivenBack() {
    // spec: RepairResult contract (experienceLeft), EN-R44b, EN-R48: 100 points repair 100 at 2 per
    // point, so 50 are consumed and 50 are given back
    RepairResult result = XpRepair.repair(mendingPick(100), 100, context());
    assertThat(result.stack().damage()).isZero();
    assertThat(result.experienceLeft()).isEqualTo(50);
  }

  @Test
  void provisionalAnItemWithoutMendingIsNotRepaired() {
    // spec: XpRepair#repair contract (unchanged when the item has no such effect)
    ItemStack plain = LootItems.damaged(LootItems.stack("iron_pickaxe"), 100);
    RepairResult result = XpRepair.repair(plain, 10, context());
    assertThat(result.stack()).isEqualTo(plain);
    assertThat(result.experienceLeft()).isEqualTo(10);
  }

  @Test
  void provisionalAnItemWithoutDamageIsNotRepaired() {
    // spec: XpRepair#repair contract (unchanged when the item has no damage)
    ItemStack fresh = mendingPick(0);
    RepairResult result = XpRepair.repair(fresh, 10, context());
    assertThat(result.stack()).isEqualTo(fresh);
    assertThat(result.experienceLeft()).isEqualTo(10);
  }

  @Test
  void provisionalNoExperienceRepairsNothing() {
    // spec: EN-R44b
    ItemStack worn = mendingPick(100);
    RepairResult result = XpRepair.repair(worn, 0, context());
    assertThat(result.stack()).isEqualTo(worn);
    assertThat(result.experienceLeft()).isZero();
  }

  @Test
  void provisionalASingleProviderGivesItsEnchantmentWithoutDrawing() {
    // spec: EN-R34 (single: enchantment and level)
    EnchantmentProvider single =
        new EnchantmentProvider.Single(Identifier.minecraft("silk_touch"), 1);
    ScriptedRandom random = ScriptedRandom.of();
    EnchantmentLevels levels =
        single.select(
            LootItems.stack("iron_pickaxe"),
            data().enchantments(),
            EnchantFixtures.access(),
            random,
            1.0);
    assertThat(levels.levels()).containsExactly(Map.entry(Identifier.minecraft("silk_touch"), 1));
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalACostProviderDrawsACostThenSelectsLikeTheTable() {
    // spec: EN-R34
    EnchantmentProvider provider =
        new EnchantmentProvider.ByCostWithDifficulty(Sets.enchantmentTag("non_treasure"), 5, 17);
    for (long seed = 1; seed <= 60; seed++) {
      EnchantmentLevels levels =
          provider.select(
              LootItems.stack("diamond_sword"),
              data().enchantments(),
              EnchantFixtures.access(),
              Lcg48Random.ofSeed(seed),
              1.0);
      for (Map.Entry<Identifier, Integer> entry : levels.levels().entrySet()) {
        int max = data().enchantments().find(entry.getKey()).orElseThrow().maxLevel();
        assertThat(entry.getValue()).isBetween(1, max);
        assertThat(data().enchantments().isCurse(entry.getKey(), EnchantFixtures.access()))
            .isFalse();
      }
    }
  }

  @Test
  void provisionalACostProviderIsDeterministicForASource() {
    // spec: EN-R34, determinism
    EnchantmentProvider provider =
        new EnchantmentProvider.ByCost(Sets.enchantmentTag("non_treasure"), 5, 17);
    for (long seed = 1; seed <= 15; seed++) {
      assertThat(
              provider.select(
                  LootItems.stack("diamond_sword"),
                  data().enchantments(),
                  EnchantFixtures.access(),
                  Lcg48Random.ofSeed(seed),
                  1.0))
          .isEqualTo(
              provider.select(
                  LootItems.stack("diamond_sword"),
                  data().enchantments(),
                  EnchantFixtures.access(),
                  Lcg48Random.ofSeed(seed),
                  1.0));
    }
  }
}
