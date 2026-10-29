package dev.shoko.minecraft.loot.enchant.craft;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.testing.CustomEnchants;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Sets;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: EN-R39 (continuation with probability (eCost + 1) / 50, then eCost = floor(eCost / 2)),
// EN-R38 (k = floor(enchantability / 4), eCost = round((c + 1 + R(k) + R(k)) x (1 + T)), T within
// +-0.15), probe PE1. Scripted sources make each boundary exact: every value is a fraction, so
// nextInt(b) is floor(v x b) and nextFloat is v.
class EnchantCostBoundariesTest {

  private static LootData triple() {
    return new CustomEnchants()
        .window("t_a", 1, 1, 100)
        .window("t_b", 1, 1, 100)
        .window("t_c", 1, 1, 100)
        .data();
  }

  private static List<EnchantmentInstance> candidates(LootData data) {
    return EnchantmentSelection.possible(
        LootItems.stack("diamond_sword"), 30, Sets.anyEnchantment(), false, data);
  }

  @Test
  void provisionalTheContinuationDrawEqualToTheBoundIsNotAContinuation() {
    // spec: EN-R39: at cost 9 the bound is 10 of 50; a draw of 10 stops, a draw of 9 continues
    LootData data = triple();
    ScriptedRandom stops = ScriptedRandom.of(0.0, 0.2001, 0.0, 0.0, 0.0, 0.0);
    assertThat(EnchantmentSelection.choose(candidates(data), 9, stops, data)).hasSize(1);
    ScriptedRandom goes = ScriptedRandom.of(0.0, 0.1999, 0.0, 0.0, 0.0, 0.0);
    assertThat(EnchantmentSelection.choose(candidates(data), 9, goes, data).size())
        .isGreaterThanOrEqualTo(2);
  }

  @Test
  void provisionalTheCostIsHalvedAtEachContinuation() {
    // spec: EN-R39: cost 49 continues surely, then cost 24 continues when the draw is below 25
    LootData data = triple();
    ScriptedRandom third = ScriptedRandom.of(0.0, 0.9, 0.0, 0.45, 0.0, 0.0, 0.0);
    assertThat(EnchantmentSelection.choose(candidates(data), 49, third, data)).hasSize(3);
    ScriptedRandom second = ScriptedRandom.of(0.0, 0.9, 0.0, 0.55, 0.0, 0.0, 0.0);
    assertThat(EnchantmentSelection.choose(candidates(data), 49, second, data)).hasSize(2);
  }

  private static List<Identifier> offered(LootData data, double fraction, int lineCost) {
    EnchantOffer offer =
        EnchantingTable.offer(
            LootItems.stack("diamond_sword"),
            lineCost,
            1,
            ScriptedRandom.repeating(fraction, 40),
            data);
    return List.copyOf(offer.enchantments().levels().keySet());
  }

  @Test
  void provisionalTheEffectiveCostUsesTheMaximalSpreadAndAQuarterOfTheEnchantability() {
    // spec: EN-R38: line 10, enchantability 10 (k = 2), draws at the top:
    // (10 + 1 + 2 + 2) x 1.1497 = 17.2, so 17
    LootData data = new CustomEnchants().window("w17", 1, 17, 17).data();
    assertThat(offered(data, 0.999, 10)).containsExactly(Identifier.minecraft("w17"));
  }

  @Test
  void provisionalTheEffectiveCostUsesTheMinimalSpreadAtTheBottom() {
    // spec: EN-R38: draws at the bottom: (10 + 1 + 0 + 0) x 0.85 = 9.35, so 9
    LootData data = new CustomEnchants().window("w9", 1, 9, 9).data();
    assertThat(offered(data, 0.0, 10)).containsExactly(Identifier.minecraft("w9"));
  }

  @Test
  void provisionalNoNeighbouringCostIsOffered() {
    // spec: EN-R38: only the exact effective cost matches a window of one value
    LootData data = new CustomEnchants().window("w16", 1, 16, 16).window("w18", 1, 18, 18).data();
    assertThat(offered(data, 0.999, 10)).isEmpty();
  }
}
