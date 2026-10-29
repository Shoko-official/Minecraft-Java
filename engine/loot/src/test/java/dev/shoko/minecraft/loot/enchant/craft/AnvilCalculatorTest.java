package dev.shoko.minecraft.loot.enchant.craft;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.LootItems;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: EN-R41a to EN-R41d (work penalty, total cost, limit 39), EN-R42a to EN-R42c (repair, merge,
// rename), EN-R43a to EN-R43e (merging enchantments), probe PE2, vector V-E13. Every formula is
// S (documentation): provisional. The multiplier of a book (EN-R43d) is an open point: no cost is
// asserted for a book.
class AnvilCalculatorTest {

  private static LootData data() {
    return Ctx.data();
  }

  private static AnvilResult combine(
      ItemStack target, ItemStack sacrifice, String name, boolean creative) {
    return AnvilCalculator.combine(new AnvilInput(target, sacrifice, name), creative, data());
  }

  private static AnvilResult combine(ItemStack target, ItemStack sacrifice) {
    return combine(target, sacrifice, "", false);
  }

  private static AnvilResult.Output output(AnvilResult result) {
    assertThat(result).isInstanceOf(AnvilResult.Output.class);
    return (AnvilResult.Output) result;
  }

  private static ItemStack sword(Map<String, Integer> enchantments) {
    return LootItems.enchanted(LootItems.stack("diamond_sword"), enchantments);
  }

  private static void assertRefused(AnvilResult result, AnvilRefusal reason) {
    assertThat(result).isEqualTo(new AnvilResult.Refused(reason));
  }

  @Test
  void anEmptyTargetHasNothingToDo() {
    // spec: AnvilRefusal#NOTHING_TO_DO contract
    assertRefused(
        combine(ItemStack.empty(), LootItems.stack("diamond_sword")), AnvilRefusal.NOTHING_TO_DO);
  }

  @Test
  void aTargetAloneWithoutNameHasNothingToDo() {
    // spec: AnvilRefusal#NOTHING_TO_DO contract
    assertRefused(
        combine(LootItems.stack("diamond_sword"), ItemStack.empty()), AnvilRefusal.NOTHING_TO_DO);
  }

  @Test
  void provisionalTwoEqualSharpnessLevelsMakeTheNextLevel() {
    // spec: EN-R43b, V-E13 (Sharpness III + III = Sharpness IV)
    AnvilResult.Output out =
        output(
            combine(
                sword(Map.of("minecraft:sharpness", 3)), sword(Map.of("minecraft:sharpness", 3))));
    assertThat(LootItems.enchantmentsOf(out.result()))
        .containsEntry(Identifier.minecraft("sharpness"), 4);
    assertThat(out.materialUsed()).isEqualTo(1);
  }

  @Test
  void provisionalTheCostOfTwoSharpnessThreeIsTheLevelTimesTheAnvilCost() {
    // spec: EN-R43d, EN-R41c,
    // V-E13
    AnvilResult.Output out =
        output(
            combine(
                sword(Map.of("minecraft:sharpness", 3)), sword(Map.of("minecraft:sharpness", 3))));
    assertThat(out.levelCost()).isEqualTo(4);
  }

  @Test
  void provisionalTheLevelIsCappedAtTheMaximumLevel() {
    // spec: EN-R43b
    AnvilResult.Output out =
        output(
            combine(
                sword(Map.of("minecraft:sharpness", 5)),
                sword(Map.of("minecraft:sharpness", 5, "minecraft:unbreaking", 1))));
    assertThat(LootItems.enchantmentsOf(out.result()))
        .containsEntry(Identifier.minecraft("sharpness"), 5);
  }

  @Test
  void provisionalAHigherLevelOfTheSacrificeWins() {
    // spec: EN-R43b
    AnvilResult.Output out =
        output(
            combine(
                sword(Map.of("minecraft:sharpness", 2)), sword(Map.of("minecraft:sharpness", 4))));
    assertThat(LootItems.enchantmentsOf(out.result()))
        .containsEntry(Identifier.minecraft("sharpness"), 4);
  }

  @Test
  void provisionalALowerLevelOfTheSacrificeChangesNothing() {
    // spec: EN-R43b, AnvilRefusal#NOTHING_TO_DO
    assertRefused(
        combine(sword(Map.of("minecraft:sharpness", 4)), sword(Map.of("minecraft:sharpness", 2))),
        AnvilRefusal.NOTHING_TO_DO);
  }

  @Test
  void provisionalAnEnchantmentTheTargetLacksIsAddedAtItsFullLevel() {
    // spec: EN-R43a
    AnvilResult.Output out =
        output(combine(LootItems.stack("diamond_sword"), sword(Map.of("minecraft:unbreaking", 2))));
    assertThat(LootItems.enchantmentsOf(out.result()))
        .containsEntry(Identifier.minecraft("unbreaking"), 2);
  }

  @Test
  void provisionalAnIncompatibleEnchantmentIsLostWhileTheOthersMerge() {
    // spec: EN-R43a, EN-R8
    AnvilResult.Output out =
        output(
            combine(
                sword(Map.of("minecraft:sharpness", 3)),
                sword(Map.of("minecraft:smite", 2, "minecraft:unbreaking", 1))));
    Map<Identifier, Integer> levels = LootItems.enchantmentsOf(out.result());
    assertThat(levels)
        .containsEntry(Identifier.minecraft("sharpness"), 3)
        .containsEntry(Identifier.minecraft("unbreaking"), 1)
        .doesNotContainKey(Identifier.minecraft("smite"));
  }

  @Test
  void provisionalAnEnchantmentTheTargetDoesNotSupportIsIgnored() {
    // spec: EN-R43c, EN-R7a; AnvilRefusal#NOTHING_TO_DO
    ItemStack bookOfEfficiency =
        LootItems.storing(LootItems.stack("enchanted_book"), Map.of("minecraft:efficiency", 3));
    assertRefused(
        combine(LootItems.stack("diamond_sword"), bookOfEfficiency), AnvilRefusal.NOTHING_TO_DO);
  }

  @Test
  void provisionalABookGivesItsStoredEnchantmentsToTheItem() {
    // spec: EN-R43a (livre enchante), probe PE2
    ItemStack book =
        LootItems.storing(LootItems.stack("enchanted_book"), Map.of("minecraft:sharpness", 2));
    AnvilResult.Output out = output(combine(LootItems.stack("diamond_sword"), book));
    assertThat(LootItems.enchantmentsOf(out.result()))
        .containsEntry(Identifier.minecraft("sharpness"), 2);
    assertThat(out.levelCost()).isPositive();
  }

  @Test
  void provisionalThePenaltyOfTheOutputIsTwiceTheLargestPlusOne() {
    // spec: EN-R41b (suite 0, 1, 3, 7, 15, 31...)
    ItemStack target = LootItems.withRepairCost(sword(Map.of("minecraft:sharpness", 3)), 3);
    ItemStack sacrifice = LootItems.withRepairCost(sword(Map.of("minecraft:sharpness", 3)), 1);
    AnvilResult.Output out = output(combine(target, sacrifice));
    assertThat(LootItems.repairCostOf(out.result())).isEqualTo(7);
    ItemStack fresh =
        output(
                combine(
                    sword(Map.of("minecraft:sharpness", 3)),
                    sword(Map.of("minecraft:sharpness", 3))))
            .result();
    assertThat(LootItems.repairCostOf(fresh)).isEqualTo(1);
  }

  @Test
  void provisionalThePenaltiesOfBothInputsAddToTheCost() {
    // spec: EN-R41c
    ItemStack target = LootItems.withRepairCost(sword(Map.of("minecraft:sharpness", 3)), 3);
    ItemStack sacrifice = LootItems.withRepairCost(sword(Map.of("minecraft:sharpness", 3)), 1);
    assertThat(output(combine(target, sacrifice)).levelCost()).isEqualTo(3 + 1 + 4);
  }

  @Test
  void provisionalACostAbove39IsTooExpensiveUnlessCreative() {
    // spec: EN-R41d
    ItemStack target = LootItems.withRepairCost(sword(Map.of("minecraft:sharpness", 1)), 40);
    ItemStack book =
        LootItems.storing(LootItems.stack("enchanted_book"), Map.of("minecraft:sharpness", 2));
    assertRefused(combine(target, book), AnvilRefusal.TOO_EXPENSIVE);
    assertThat(combine(target, book, "", true)).isInstanceOf(AnvilResult.Output.class);
  }

  @Test
  void provisionalRenamingAloneCostsOneLevelAndKeepsThePenalty() {
    // spec: EN-R41b, EN-R41c (renommage 1), EN-R42c
    AnvilResult.Output out =
        output(combine(LootItems.stack("diamond_sword"), ItemStack.empty(), "Excalibur", false));
    assertThat(out.levelCost()).isEqualTo(1);
    assertThat(out.result().component(Identifier.minecraft("custom_name"))).isPresent();
    assertThat(LootItems.repairCostOf(out.result())).isZero();
  }

  @Test
  void provisionalRenamingKeepsTheEnchantments() {
    // spec: EN-R41c, EN-R42c
    ItemStack enchanted = sword(Map.of("minecraft:sharpness", 3));
    AnvilResult.Output out = output(combine(enchanted, ItemStack.empty(), "Excalibur", false));
    assertThat(LootItems.enchantmentsOf(out.result()))
        .containsEntry(Identifier.minecraft("sharpness"), 3);
  }

  @Test
  void provisionalAMaterialRepairsAQuarterOfTheMaximumDurabilityRoundedDown() {
    // spec: EN-R42a
    ItemStack worn = LootItems.damaged(LootItems.stack("diamond_sword"), 800);
    AnvilResult.Output out = output(combine(worn, LootItems.stack("diamond", 1)));
    assertThat(out.result().damage()).isEqualTo(800 - 390);
    assertThat(out.materialUsed()).isEqualTo(1);
  }

  @Test
  void provisionalOnlyTheMaterialNeededIsUsed() {
    // spec: EN-R42a, AnvilResult.Output#materialUsed contract
    ItemStack worn = LootItems.damaged(LootItems.stack("diamond_sword"), 300);
    AnvilResult.Output out = output(combine(worn, LootItems.stack("diamond", 3)));
    assertThat(out.result().damage()).isZero();
    assertThat(out.materialUsed()).isEqualTo(1);
  }

  @Test
  void provisionalMergingTwoDamagedItemsAddsTheRemainingDurabilityAndATwelvePercentBonus() {
    // spec: EN-R42b (50 + 50 + 12 percent of 250 = 130 left: damage 120)
    ItemStack first = LootItems.damaged(LootItems.stack("iron_pickaxe"), 200);
    ItemStack second = LootItems.damaged(LootItems.stack("iron_pickaxe"), 200);
    AnvilResult.Output out = output(combine(first, second));
    assertThat(out.result().damage()).isEqualTo(120);
    assertThat(out.materialUsed()).isEqualTo(1);
  }

  @Test
  void provisionalMergingTwoItemsNeverRepairsBeyondTheMaximum() {
    // spec: EN-R42b (plafonne)
    ItemStack first = LootItems.damaged(LootItems.stack("iron_pickaxe"), 100);
    ItemStack second = LootItems.damaged(LootItems.stack("iron_pickaxe"), 100);
    AnvilResult.Output out = output(combine(first, second));
    assertThat(out.result().damage()).isBetween(0, 100);
  }

  @Test
  void provisionalTwoUnrelatedUndamagedItemsHaveNothingToDo() {
    // spec: AnvilRefusal#NOTHING_TO_DO contract
    assertRefused(
        combine(LootItems.stack("diamond_sword"), LootItems.stack("iron_pickaxe")),
        AnvilRefusal.NOTHING_TO_DO);
  }

  @Test
  void theCalculationIsAPureFunction() {
    // spec: AnvilCalculator contract (no random draw)
    ItemStack target = sword(Map.of("minecraft:sharpness", 3));
    ItemStack sacrifice = sword(Map.of("minecraft:sharpness", 3));
    assertThat(combine(target, sacrifice)).isEqualTo(combine(target, sacrifice));
  }
}
