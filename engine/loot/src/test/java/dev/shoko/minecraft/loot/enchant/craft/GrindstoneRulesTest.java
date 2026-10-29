package dev.shoko.minecraft.loot.enchant.craft;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.LootItems;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: EN-R44a (one item: non-curse enchantments removed, penalty reset), EN-R47 (two items of
// the same type: durability = min(max, remaining + remaining + 5 percent of max rounded down);
// H-meule-a: the output carries the curses of both entries; experience in [ceil(S / 2), S] where S
// is
// the sum of the minimal costs of the non-curse enchantments, only when an entry was enchanted),
// V-E21, probes PE4 and PE15. The experience formula has no draw here, so only its range is tested.
class GrindstoneRulesTest {

  private static final Identifier BINDING = Identifier.minecraft("binding_curse");

  private static LootData data() {
    return Ctx.data();
  }

  private static ItemStack chest(Map<String, Integer> levels) {
    return LootItems.enchanted(LootItems.stack("diamond_chestplate"), levels);
  }

  private static GrindResult grind(ItemStack first, ItemStack second) {
    return GrindstoneCalculator.grind(first, second, data()).orElseThrow();
  }

  @Test
  void provisionalTheOutputCarriesTheCursesOfTheSecondItemToo() {
    // spec: EN-R47 (H-meule-a): first plain, second cursed
    GrindResult result = grind(chest(Map.of()), chest(Map.of("minecraft:binding_curse", 1)));
    assertThat(LootItems.enchantmentsOf(result.output())).containsEntry(BINDING, 1);
  }

  @Test
  void provisionalTheOutputCarriesTheCursesOfTheFirstItemToo() {
    // spec: EN-R47 (H-meule-a): first cursed, second plain
    GrindResult result = grind(chest(Map.of("minecraft:binding_curse", 1)), chest(Map.of()));
    assertThat(LootItems.enchantmentsOf(result.output())).containsEntry(BINDING, 1);
  }

  @Test
  void provisionalTheSameCurseOnBothItemsIsKeptOnce() {
    // spec: EN-R47 (union par identifiant, niveau 1)
    ItemStack cursed = chest(Map.of("minecraft:binding_curse", 1, "minecraft:protection", 3));
    GrindResult result = grind(cursed, cursed);
    assertThat(LootItems.enchantmentsOf(result.output())).containsExactly(Map.entry(BINDING, 1));
  }

  @Test
  void provisionalTheNonCurseEnchantmentsOfBothItemsAreRemoved() {
    // spec: EN-R47, EN-R44a
    GrindResult result =
        grind(
            chest(Map.of("minecraft:protection", 3)),
            chest(Map.of("minecraft:fire_protection", 2, "minecraft:binding_curse", 1)));
    assertThat(LootItems.enchantmentsOf(result.output())).containsOnlyKeys(BINDING);
  }

  @Test
  void provisionalTwoPickaxesAddTheirRemainingDurabilityAndFivePercent() {
    // spec: EN-R47: 50 + 50 + floor(12.5) = 112 of 250, so damage 138
    ItemStack first = LootItems.damaged(LootItems.stack("iron_pickaxe"), 200);
    ItemStack second = LootItems.damaged(LootItems.stack("iron_pickaxe"), 200);
    assertThat(grind(first, second).output().damage()).isEqualTo(138);
  }

  @Test
  void provisionalTwoSwordsAddTheirRemainingDurabilityAndFivePercent() {
    // spec: EN-R47: 61 + 161 + floor(78.05) = 300 of 1561, so damage 1261
    ItemStack first = LootItems.damaged(LootItems.stack("diamond_sword"), 1500);
    ItemStack second = LootItems.damaged(LootItems.stack("diamond_sword"), 1400);
    assertThat(grind(first, second).output().damage()).isEqualTo(1261);
  }

  @Test
  void provisionalTheDurabilityIsCappedAtTheMaximum() {
    // spec: EN-R47 (min(max_damage, ...))
    ItemStack first = LootItems.damaged(LootItems.stack("diamond_sword"), 10);
    ItemStack second = LootItems.damaged(LootItems.stack("diamond_sword"), 10);
    assertThat(grind(first, second).output().damage()).isZero();
  }

  @Test
  void provisionalTheOutputKeepsTheNameOfTheTopItemAndResetsThePenalty() {
    // spec: EN-R47 (garde le nom du slot du haut, penalite a 0)
    Identifier name = Identifier.minecraft("custom_name");
    ItemStack top =
        LootItems.withRepairCost(chest(Map.of()), 7)
            .withComponent(name, new ComponentValue(new StringTag("Top")));
    ItemStack bottom =
        LootItems.withRepairCost(chest(Map.of()), 3)
            .withComponent(name, new ComponentValue(new StringTag("Bottom")));
    GrindResult result = grind(top, bottom);
    assertThat(result.output().component(name)).isEqualTo(top.component(name));
    assertThat(LootItems.repairCostOf(result.output())).isZero();
  }

  @Test
  void provisionalSharpnessFiveGivesBetweenTwentyThreeAndFortyFiveExperience() {
    // spec: EN-R47, V-E21 (S = min_cost(5) = 45: from 23 to 45)
    ItemStack sword =
        LootItems.enchanted(LootItems.stack("diamond_sword"), Map.of("minecraft:sharpness", 5));
    assertThat(grind(sword, ItemStack.empty()).experience()).isBetween(23, 45);
  }

  @Test
  void provisionalTheExperienceCountsOnlyTheNonCurseEnchantments() {
    // spec: EN-R47 (somme des enchantements non maudits): a curse alone gives nothing
    assertThat(grind(chest(Map.of("minecraft:binding_curse", 1)), ItemStack.empty()).experience())
        .isZero();
    int plain = grind(chest(Map.of("minecraft:protection", 4)), ItemStack.empty()).experience();
    int withCurse =
        grind(
                chest(Map.of("minecraft:protection", 4, "minecraft:binding_curse", 1)),
                ItemStack.empty())
            .experience();
    assertThat(withCurse).isEqualTo(plain);
  }

  @Test
  void provisionalNoExperienceWhenNoEntryWasEnchanted() {
    // spec: EN-R47 (rendue seulement si au moins une entree etait enchantee)
    ItemStack first = LootItems.damaged(LootItems.stack("diamond_sword"), 100);
    ItemStack second = LootItems.damaged(LootItems.stack("diamond_sword"), 100);
    assertThat(grind(first, second).experience()).isZero();
  }

  @Test
  void provisionalTheExperienceSumsBothEntries() {
    // spec: EN-R47 (somme des niveaux minimaux modifies de toutes les entrees)
    ItemStack one =
        LootItems.enchanted(LootItems.stack("diamond_sword"), Map.of("minecraft:sharpness", 5));
    ItemStack two =
        LootItems.enchanted(LootItems.stack("diamond_sword"), Map.of("minecraft:sharpness", 5));
    // S = 90: from 45 to 90
    assertThat(grind(one, two).experience()).isBetween(45, 90);
  }
}
