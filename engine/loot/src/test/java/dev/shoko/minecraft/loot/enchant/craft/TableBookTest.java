package dev.shoko.minecraft.loot.enchant.craft;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.CustomEnchants;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.Sets;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

// spec: EN-R45 (a book of the table loses one enchantment only when several were selected: N=1
// gives 1, N=2 gives 1, N=3 gives 2), V-E18, probes PE13 and PE1, EN-R36 (H-livre-loot-a: the
// removal belongs to the table, not to enchant_with_levels), EN-R35 (enchant_randomly).
class TableBookTest {

  private static final int SEEDS = 4000;

  private static LootData triple() {
    return new CustomEnchants()
        .window("t_a", 1, 1, 100)
        .window("t_b", 1, 1, 100)
        .window("t_c", 1, 1, 100)
        .data();
  }

  private static LootData pair() {
    return new CustomEnchants().window("t_a", 1, 1, 100).window("t_b", 1, 1, 100).data();
  }

  private static LootData single() {
    return new CustomEnchants().window("t_a", 1, 1, 100).data();
  }

  private static Map<Integer, Integer> offerSizes(LootData data, ItemStack stack) {
    Map<Integer, Integer> sizes = new TreeMap<>();
    for (long seed = 1; seed <= SEEDS; seed++) {
      EnchantOffer offer = EnchantingTable.offer(stack, 30, 2, Lcg48Random.ofSeed(seed), data);
      sizes.merge(offer.enchantments().levels().size(), 1, Integer::sum);
    }
    return sizes;
  }

  private static Map<Integer, Integer> levelsSizes(LootData data, ItemStack stack) {
    Map<Integer, Integer> sizes = new TreeMap<>();
    for (long seed = 1; seed <= SEEDS; seed++) {
      LootContext context = LootContext.builder(data, Ctx.ANY, Lcg48Random.ofSeed(seed)).build();
      ItemStack result =
          EnchantmentSelection.enchantWithLevels(stack, 30, Sets.anyEnchantment(), context);
      int size = LootItems.enchantmentsOf(result).size() + LootItems.storedOf(result).size();
      sizes.merge(size, 1, Integer::sum);
    }
    return sizes;
  }

  @Test
  void provisionalABookOfTheTableNeverHoldsThreeEnchantmentsButSometimesTwo() {
    // spec: EN-R45, V-E18, PE13 (never 3 for the book, 2 with a positive probability)
    Map<Integer, Integer> book = offerSizes(triple(), LootItems.stack("book"));
    assertThat(book.keySet()).containsExactly(1, 2);
    assertThat(book.get(2)).isGreaterThan(SEEDS / 20);
  }

  @Test
  void provisionalASwordOfTheTableReachesThreeEnchantments() {
    // spec: EN-R45 (the removal concerns books only), PE13 (the sword reaches 3)
    Map<Integer, Integer> sword = offerSizes(triple(), LootItems.stack("diamond_sword"));
    assertThat(sword).containsKey(3);
    assertThat(sword.get(3)).isGreaterThan(SEEDS / 20);
  }

  @Test
  void provisionalABookWithTwoSelectedEnchantmentsHoldsOne() {
    // spec: EN-R45 (N = 2 gives 1), V-E18
    assertThat(offerSizes(pair(), LootItems.stack("book")).keySet()).containsExactly(1);
    assertThat(offerSizes(pair(), LootItems.stack("diamond_sword")).keySet()).contains(1, 2);
  }

  @Test
  void provisionalABookWithOneSelectedEnchantmentHoldsOne() {
    // spec: EN-R45 (N = 1 gives 1: nothing is removed), V-E18
    assertThat(offerSizes(single(), LootItems.stack("book")).keySet()).containsExactly(1);
  }

  @Test
  void provisionalTheBookSizesAreTheSelectionSizesMinusOneWhenSeveral() {
    // spec: EN-R45 (N-1), PE13: P(book = 2) is P(N = 3)
    Map<Integer, Integer> book = offerSizes(triple(), LootItems.stack("book"));
    Map<Integer, Integer> sword = offerSizes(triple(), LootItems.stack("diamond_sword"));
    double bookTwo = book.getOrDefault(2, 0) / (double) SEEDS;
    double swordThree = sword.getOrDefault(3, 0) / (double) SEEDS;
    assertThat(bookTwo).isBetween(swordThree - 0.06, swordThree + 0.06);
  }

  @Test
  void provisionalEnchantWithLevelsKeepsEveryEnchantmentOfABook() {
    // spec: EN-R36, H-livre-loot-a (the removal of EN-R45 is the table's), PE1
    Map<Integer, Integer> book = levelsSizes(triple(), LootItems.stack("book"));
    assertThat(book).containsKey(3);
    assertThat(book.get(3)).isGreaterThan(SEEDS / 20);
  }

  @Test
  void provisionalEnchantRandomlyGivesABookExactlyOneEnchantment() {
    // spec: EN-R35 (one enchantment, no removal)
    for (long seed = 1; seed <= 300; seed++) {
      LootContext context =
          LootContext.builder(triple(), Ctx.ANY, Lcg48Random.ofSeed(seed)).build();
      ItemStack result =
          EnchantmentSelection.enchantRandomly(
              LootItems.stack("book"), Sets.anyEnchantment(), true, context);
      assertThat(LootItems.storedOf(result)).hasSize(1);
    }
  }
}
