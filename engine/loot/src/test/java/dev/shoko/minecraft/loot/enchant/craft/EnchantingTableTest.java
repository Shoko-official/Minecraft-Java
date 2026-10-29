package dev.shoko.minecraft.loot.enchant.craft;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: EN-R37 (costs of the three lines), EN-R38 (effective cost and candidates), EN-R39 (choice),
// EN-R40 (same offers for the same source), V-E11, probes PE3 and PE9. The lapis cost and the
// source of the XpSeed are open points and are not tested.
class EnchantingTableTest {

  private static LootData data() {
    return Ctx.data();
  }

  private static final List<String> TABLE_SWORD =
      List.of("sharpness", "smite", "bane_of_arthropods", "unbreaking", "looting", "fire_aspect");

  private static LineCosts repeating(int shelves, double fraction) {
    return EnchantingTable.lineCosts(shelves, ScriptedRandom.repeating(fraction, 8));
  }

  @Test
  void provisionalWithNoShelfAndMinimalDrawsEveryLineCostsOne() {
    // spec: EN-R37 (base = 1 + 0 + 0 + 0: top max(1, 0), middle 0 + 1, bottom max(1, 0))
    assertThat(repeating(0, 0.0)).isEqualTo(new LineCosts(1, 1, 1));
  }

  @Test
  void provisionalWithNoShelfAndMaximalDrawsTheBaseIsEight() {
    // spec: EN-R37 (base = 1 + R(7) = 8: top 2, middle 6, bottom 8)
    assertThat(repeating(0, 0.999)).isEqualTo(new LineCosts(2, 6, 8));
  }

  @Test
  void provisionalFifteenShelvesAndMaximalDrawsGiveThirty() {
    // spec: EN-R37 (base = 1 + 7 + 7 + 15 = 30: top 10, middle 21, bottom 30)
    assertThat(repeating(15, 0.999)).isEqualTo(new LineCosts(10, 21, 30));
  }

  @Test
  void provisionalFifteenShelvesAndMinimalDrawsStillCostThirtyOnTheBottomLine() {
    // spec: EN-R37 (base = 1 + 0 + 7 + 0 = 8: top 2, middle 6, bottom max(8, 30))
    assertThat(repeating(15, 0.0)).isEqualTo(new LineCosts(2, 6, 30));
  }

  @Test
  void provisionalSevenShelvesAndMiddleDraws() {
    // spec: EN-R37 (R(7) = 4, floor(7 / 2) = 3, R(7) = 4: base 12; bottom max(12, 14))
    assertThat(repeating(7, 0.5)).isEqualTo(new LineCosts(4, 9, 14));
  }

  @Test
  void provisionalMoreThanFifteenShelvesCountAsFifteen() {
    // spec: EN-R37, EnchantingTable#lineCosts contract (more are capped)
    assertThat(repeating(40, 0.999)).isEqualTo(repeating(15, 0.999));
    assertThat(repeating(40, 0.2)).isEqualTo(repeating(15, 0.2));
  }

  @Test
  void provisionalTheLineCostsStayInTheirRanges() {
    // spec: EN-R37 (distribution, V-E11: costs in [1, 30])
    for (long seed = 1; seed <= 300; seed++) {
      LineCosts full = EnchantingTable.lineCosts(15, Lcg48Random.ofSeed(seed));
      assertThat(full.top()).isBetween(2, 10);
      assertThat(full.middle()).isBetween(6, 21);
      assertThat(full.bottom()).isEqualTo(30);
      LineCosts none = EnchantingTable.lineCosts(0, Lcg48Random.ofSeed(seed));
      assertThat(none.top()).isBetween(1, 2);
      assertThat(none.middle()).isBetween(1, 6);
      assertThat(none.bottom()).isBetween(1, 8);
    }
  }

  @Test
  void provisionalTheSameSourceStateGivesTheSameLineCosts() {
    // spec: EN-R40, probe PE9
    for (long seed = 1; seed <= 20; seed++) {
      assertThat(EnchantingTable.lineCosts(9, Lcg48Random.ofSeed(seed)))
          .isEqualTo(EnchantingTable.lineCosts(9, Lcg48Random.ofSeed(seed)));
    }
  }

  @Test
  void provisionalAnOfferKeepsTheCostOfItsLine() {
    // spec: EnchantOffer contract, EN-R37
    EnchantOffer offer =
        EnchantingTable.offer(
            LootItems.stack("diamond_sword"), 30, 2, Lcg48Random.ofSeed(1), data());
    assertThat(offer.lineCost()).isEqualTo(30);
  }

  @Test
  void provisionalAnOfferForASwordHoldsOnlyEnchantmentsOfTheTableForThatItem() {
    // spec: EN-R38 (in_enchanting_table, primary_items), EN-R8, EN-R10
    for (long seed = 1; seed <= 150; seed++) {
      EnchantOffer offer =
          EnchantingTable.offer(
              LootItems.stack("diamond_sword"), 30, 2, Lcg48Random.ofSeed(seed), data());
      Map<Identifier, Integer> levels = offer.enchantments().levels();
      assertThat(levels).as("seed %d", seed).isNotEmpty();
      for (Map.Entry<Identifier, Integer> entry : levels.entrySet()) {
        assertThat(TABLE_SWORD).contains(entry.getKey().path());
        int max = data().enchantments().find(entry.getKey()).orElseThrow().maxLevel();
        assertThat(entry.getValue()).isBetween(1, max);
      }
      long damage =
          levels.keySet().stream()
              .filter(id -> List.of("sharpness", "smite", "bane_of_arthropods").contains(id.path()))
              .count();
      assertThat(damage).isLessThanOrEqualTo(1);
    }
  }

  @Test
  void provisionalAnAxeIsNotOfferedSharpnessByTheTable() {
    // spec: EN-R7b, EN-R38 (sharpness is primary for melee weapons only)
    for (long seed = 1; seed <= 150; seed++) {
      EnchantOffer offer =
          EnchantingTable.offer(
              LootItems.stack("diamond_axe"), 30, 2, Lcg48Random.ofSeed(seed), data());
      assertThat(offer.enchantments().levels().keySet())
          .doesNotContain(Identifier.minecraft("sharpness"));
    }
  }

  @Test
  void provisionalTheTableNeverOffersTreasureEnchantments() {
    // spec: EN-R38 (etiquette in_enchanting_table), EN-R9b (a channel reads its own tag)
    for (long seed = 1; seed <= 150; seed++) {
      for (String item : List.of("diamond_sword", "book")) {
        EnchantOffer offer =
            EnchantingTable.offer(LootItems.stack(item), 30, 2, Lcg48Random.ofSeed(seed), data());
        assertThat(offer.enchantments().levels().keySet())
            .doesNotContain(Identifier.minecraft("mending"), Identifier.minecraft("binding_curse"));
      }
    }
  }

  @Test
  void provisionalABookIsOfferedExactlyOneEnchantment() {
    // spec: EN-R39
    for (long seed = 1; seed <= 150; seed++) {
      EnchantOffer offer =
          EnchantingTable.offer(LootItems.stack("book"), 30, 2, Lcg48Random.ofSeed(seed), data());
      assertThat(offer.enchantments().levels()).as("seed %d", seed).hasSize(1);
    }
  }

  @Test
  void provisionalAnOfferIsDeterministicForASource() {
    // spec: EN-R40, probe PE9
    for (long seed = 1; seed <= 20; seed++) {
      assertThat(
              EnchantingTable.offer(
                  LootItems.stack("diamond_sword"), 20, 1, Lcg48Random.ofSeed(seed), data()))
          .isEqualTo(
              EnchantingTable.offer(
                  LootItems.stack("diamond_sword"), 20, 1, Lcg48Random.ofSeed(seed), data()));
    }
  }

  @Test
  void provisionalTheThreeOffersFollowTheThreeLineCosts() {
    // spec: EnchantingTable#offers contract (lineCosts then one offer per line), EN-R37
    ItemStack sword = LootItems.stack("diamond_sword");
    for (long seed = 1; seed <= 25; seed++) {
      List<EnchantOffer> offers =
          EnchantingTable.offers(sword, 15, Lcg48Random.ofSeed(seed), data());
      LineCosts costs = EnchantingTable.lineCosts(15, Lcg48Random.ofSeed(seed));
      assertThat(offers).hasSize(3);
      assertThat(offers.stream().map(EnchantOffer::lineCost).toList())
          .containsExactly(costs.top(), costs.middle(), costs.bottom());
    }
  }

  @Test
  void provisionalAnItemWithoutEnchantableGetsNoOffer() {
    // spec: EN-R40
    assertThat(EnchantingTable.offers(LootItems.stack("stone"), 15, Lcg48Random.ofSeed(1), data()))
        .isEmpty();
  }

  @Test
  void provisionalAnItemThatIsAlreadyEnchantedGetsNoOffer() {
    // spec: EN-R40
    ItemStack enchanted =
        LootItems.enchanted(LootItems.stack("diamond_sword"), Map.of("minecraft:sharpness", 1));
    assertThat(EnchantingTable.offers(enchanted, 15, Lcg48Random.ofSeed(1), data())).isEmpty();
  }

  @Test
  void provisionalABookGetsThreeOffers() {
    // spec: EN-R38, V-E11
    assertThat(EnchantingTable.offers(LootItems.stack("book"), 15, Lcg48Random.ofSeed(2), data()))
        .hasSize(3);
  }

  @Test
  void provisionalTheSameSourceStateGivesTheSameOffers() {
    // spec: EN-R40, V-E11
    for (long seed = 1; seed <= 15; seed++) {
      assertThat(
              EnchantingTable.offers(
                  LootItems.stack("diamond_sword"), 15, Lcg48Random.ofSeed(seed), data()))
          .isEqualTo(
              EnchantingTable.offers(
                  LootItems.stack("diamond_sword"), 15, Lcg48Random.ofSeed(seed), data()));
    }
  }
}
