package dev.shoko.minecraft.loot.enchant.craft;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.LootItems;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: EN-R41c (total = P + Rn + Rp + E), EN-R41d (above 39 refused), EN-R42a (material: one level
// per unit consumed, never a flat 2), EN-R42b (merge of two items: flat 2 when the target is not at
// full durability, durability capped), EN-R43d (item multiplier = anvil_cost, book multiplier =
// max(1, floor(anvil_cost / 2))), EN-R46 (limit cases), V-E20, probe PE14. Fixtures: anvil_cost is
// 1 for sharpness, 2 for unbreaking and 4 for mending; a diamond sword has 1561 durability.
class AnvilCostsTest {

  private static LootData data() {
    return Ctx.data();
  }

  private static AnvilResult combine(
      ItemStack target, ItemStack sacrifice, String name, boolean creative) {
    return AnvilCalculator.combine(new AnvilInput(target, sacrifice, name), creative, data());
  }

  private static AnvilResult.Output output(AnvilResult result) {
    assertThat(result).isInstanceOf(AnvilResult.Output.class);
    return (AnvilResult.Output) result;
  }

  private static ItemStack sword(int damage) {
    return LootItems.damaged(LootItems.stack("diamond_sword"), damage);
  }

  private static ItemStack enchantedSword(int damage, String enchantment, int level) {
    return LootItems.enchanted(sword(damage), Map.of("minecraft:" + enchantment, level));
  }

  private static ItemStack book(String enchantment, int level) {
    return LootItems.storing(
        LootItems.stack("enchanted_book"), Map.of("minecraft:" + enchantment, level));
  }

  private static int cost(ItemStack target, ItemStack sacrifice) {
    return output(combine(target, sacrifice, "", false)).levelCost();
  }

  @Test
  void provisionalTwoDamagedSwordsCostTheFlatTwo() {
    // spec: EN-R46 (1), V-E20 (2)
    assertThat(cost(sword(100), sword(50))).isEqualTo(2);
  }

  @Test
  void provisionalTheFlatTwoDoesNotDependOnTheRepairedAmount() {
    // spec: EN-R42b (forfait unique, quelle que soit la durabilite restauree)
    assertThat(cost(sword(1), sword(1))).isEqualTo(2);
    assertThat(cost(sword(1500), sword(1500))).isEqualTo(2);
  }

  @Test
  void provisionalPenaltiesAddToTheFlatTwo() {
    // spec: EN-R46 (2), V-E20 (6): 2 + 1 + 3
    ItemStack target = LootItems.withRepairCost(sword(100), 1);
    ItemStack sacrifice = LootItems.withRepairCost(sword(100), 3);
    assertThat(cost(target, sacrifice)).isEqualTo(6);
  }

  @Test
  void provisionalTwoIntactSwordsAreRefused() {
    // spec: EN-R46 (3), V-E20 (refus)
    assertThat(combine(sword(0), sword(0), "", false))
        .isEqualTo(new AnvilResult.Refused(AnvilRefusal.NOTHING_TO_DO));
  }

  @Test
  void provisionalAnIntactTargetWithAnEnchantedSwordPaysOnlyTheEnchantment() {
    // spec: EN-R46 (4): Rp = 0, cost P + E = 3 x anvil_cost 1
    assertThat(cost(sword(0), enchantedSword(0, "sharpness", 3))).isEqualTo(3);
  }

  @Test
  void provisionalADamagedTargetWithAnEnchantedSwordPaysTheFlatTwoAndTheEnchantment() {
    // spec: EN-R46 (5): P + 2 + E = 2 + 3
    assertThat(cost(sword(100), enchantedSword(0, "sharpness", 3))).isEqualTo(5);
  }

  @Test
  void provisionalADamagedTargetWithABookPaysNoRepairCost() {
    // spec: EN-R46 (6): Rp = 0 for a book, cost P + E (book multiplier max(1, 0) = 1)
    assertThat(cost(sword(100), book("sharpness", 3))).isEqualTo(3);
  }

  @Test
  void provisionalThreeUnitsOfMaterialCostThreeLevels() {
    // spec: EN-R42a, EN-R46 (7), V-E20 (3): 1100 durability lost needs 3 units of 390
    AnvilResult.Output out = output(combine(sword(1100), LootItems.stack("diamond", 5), "", false));
    assertThat(out.levelCost()).isEqualTo(3);
    assertThat(out.materialUsed()).isEqualTo(3);
    assertThat(out.result().damage()).isZero();
  }

  @Test
  void provisionalOneUnitOfMaterialCostsOneLevelNotTwo() {
    // spec: EN-R42a (jamais de forfait de 2)
    AnvilResult.Output out = output(combine(sword(100), LootItems.stack("diamond", 4), "", false));
    assertThat(out.levelCost()).isEqualTo(1);
    assertThat(out.materialUsed()).isEqualTo(1);
  }

  @Test
  void provisionalMaterialWithARenameAddsOneLevel() {
    // spec: EN-R46 (7), V-E20 (4): 3 + 1
    assertThat(output(combine(sword(1100), LootItems.stack("diamond", 5), "x", false)).levelCost())
        .isEqualTo(4);
  }

  @Test
  void provisionalMaterialWithAPenaltyAddsThePenalty() {
    // spec: EN-R46 (7): 3 + 2
    ItemStack target = LootItems.withRepairCost(sword(1100), 2);
    assertThat(cost(target, LootItems.stack("diamond", 5))).isEqualTo(5);
  }

  @Test
  void provisionalMaterialOnAnIntactTargetIsRefused() {
    // spec: EN-R42a (une matiere sur une cible intacte ne consomme aucune unite: refus)
    assertThat(combine(sword(0), LootItems.stack("diamond", 3), "", false))
        .isEqualTo(new AnvilResult.Refused(AnvilRefusal.NOTHING_TO_DO));
  }

  @Test
  void provisionalRenamingAloneCostsOnePlusThePenalty() {
    // spec: EN-R46 (8)
    ItemStack target = LootItems.withRepairCost(sword(0), 3);
    assertThat(output(combine(target, ItemStack.empty(), "x", false)).levelCost()).isEqualTo(4);
  }

  @Test
  void provisionalTheLimitIsExactlyThirtyNineInSurvival() {
    // spec: EN-R41d, EN-R46 (9): 38 + 1 is accepted, 39 + 1 is refused, creative lifts the limit
    ItemStack at39 = LootItems.withRepairCost(sword(0), 38);
    ItemStack at40 = LootItems.withRepairCost(sword(0), 39);
    assertThat(output(combine(at39, ItemStack.empty(), "x", false)).levelCost()).isEqualTo(39);
    assertThat(combine(at40, ItemStack.empty(), "x", false))
        .isEqualTo(new AnvilResult.Refused(AnvilRefusal.TOO_EXPENSIVE));
    assertThat(combine(at40, ItemStack.empty(), "x", true)).isInstanceOf(AnvilResult.Output.class);
  }

  @Test
  void provisionalAMergeNeverRepairsBeyondTheMaximumDurability() {
    // spec: EN-R42b: min(max_damage, remaining + remaining + 12 percent)
    AnvilResult.Output out = output(combine(sword(1), sword(1), "", false));
    assertThat(out.result().damage()).isZero();
    // 61 + 161 + floor(0.12 x 1561) = 409 left of 1561
    AnvilResult.Output wide = output(combine(sword(1500), sword(1400), "", false));
    assertThat(wide.result().damage()).isEqualTo(1561 - 409);
  }

  @Test
  void provisionalABookCostsHalfTheAnvilCostPerLevelAndAnItemTheFullCost() {
    // spec: EN-R43d, EN-R43e (Mending 4/2): mending has anvil_cost 4
    assertThat(cost(sword(0), book("mending", 1))).isEqualTo(2);
    assertThat(cost(sword(0), enchantedSword(0, "mending", 1))).isEqualTo(4);
  }

  @Test
  void provisionalUnbreakingCostsTwoPerLevelFromAnItemAndOnePerLevelFromABook() {
    // spec: EN-R43d, EN-R43e (Unbreaking 2/1): anvil_cost 2, book multiplier max(1, 1)
    assertThat(cost(sword(0), enchantedSword(0, "unbreaking", 3))).isEqualTo(6);
    assertThat(cost(sword(0), book("unbreaking", 3))).isEqualTo(3);
  }

  @Test
  void provisionalAnIncompatibleEnchantmentIsLostAndCostsOneLevel() {
    // spec: EN-R43a (perdu et coute 1 niveau): unbreaking I from an item (2) + smite lost (1)
    ItemStack target = enchantedSword(0, "sharpness", 3);
    ItemStack sacrifice =
        LootItems.enchanted(sword(0), Map.of("minecraft:smite", 2, "minecraft:unbreaking", 1));
    assertThat(cost(target, sacrifice)).isEqualTo(3);
  }

  @Test
  void provisionalABrokenTargetPastTheMaximumMergesToADamageBetweenZeroAndTheMaximum() {
    // spec: EN-R42b (remaining = max(0, max - damage), the result stays in [0, max_damage]): both
    // items 439 over the maximum of 1561 have nothing left, only the 12 percent bonus of 187 stays
    AnvilResult.Output out = output(combine(sword(2000), sword(2000), "", false));
    assertThat(out.result().damage()).isEqualTo(1561 - 187);
    assertThat(output(combine(sword(1562), sword(1561), "", false)).result().damage())
        .isEqualTo(1561 - 187);
  }

  @Test
  void provisionalABrokenTargetMergedWithAnIntactItemRestoresTheFullDurability() {
    // spec: EN-R42b: 0 + 1561 + 187 is capped at the maximum, so the damage is 0, never negative
    assertThat(output(combine(sword(5000), sword(0), "", false)).result().damage()).isZero();
    assertThat(output(combine(sword(1562), sword(0), "", false)).result().damage()).isZero();
  }

  @Test
  void provisionalASacrificeBrokenPastTheMaximumAddsOnlyTheBonus() {
    // spec: EN-R42b (a damage above the maximum counts for 0 remaining durability)
    int damage =
        output(combine(sword(1000), sword(Integer.MAX_VALUE), "", false)).result().damage();
    assertThat(damage).isBetween(0, 1561);
    assertThat(damage).isEqualTo(1561 - (561 + 187));
  }
}
