package dev.shoko.minecraft.loot.modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootBudgetException;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.Tables;
import dev.shoko.minecraft.loot.testing.WatchedRandom;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: LT-R74e (the loops that walk a quantity unit by unit are bounded while computing, in
// units of work per call: 10000000), LT-R42 (apply_bonus binomial_with_bonus_count draws one
// number per trial, level + extra trials), LT-R74g (the typed error). Without a tool the level is
// 0, so an extra of 99999 makes 99999 trials, 100 of them 9999900 units and 101 of them 10099899.
class BonusUnitsTest {

  private static List<ItemModifier> bonuses(int copies) {
    ItemModifier bonus =
        Tables.modifier(
            new ModifierAction.ApplyBonus(
                Identifier.minecraft("fortune"),
                new BonusFormula.BinomialWithBonusCount(99_999, 0f)));
    return Collections.nCopies(copies, bonus);
  }

  private static LootContext context(WatchedRandom random) {
    return Ctx.builder(random).build();
  }

  @Test
  void provisionalOneHundredBinomialBonusesOfTheSameCallPass() {
    // spec: LT-R74e: 100 x 99999 = 9999900 units, under the cap of 10000000
    WatchedRandom random = WatchedRandom.limited(Lcg48Random.ofSeed(1), 20_000_000L);
    List<ItemStack> result =
        ModifierApplier.applyAll(bonuses(100), List.of(LootItems.stack("stone")), context(random));
    assertThat(result).hasSize(1);
    assertThat(random.draws()).isEqualTo(100L * 99_999);
  }

  @Test
  void provisionalOneHundredAndOneBinomialBonusesOfTheSameCallFailTheCall() {
    // spec: LT-R74e, LT-R74g: 101 x 99999 = 10099899 units, over the cap, the error comes before
    // the loop that would pass the cap
    WatchedRandom random = WatchedRandom.limited(Lcg48Random.ofSeed(1), 20_000_000L);
    LootContext context = context(random);
    List<ItemModifier> bonuses = bonuses(101);
    List<ItemStack> stacks = List.of(LootItems.stack("stone"));
    assertThatThrownBy(() -> ModifierApplier.applyAll(bonuses, stacks, context))
        .isInstanceOf(LootBudgetException.class);
    assertThat(random.draws()).isLessThanOrEqualTo(100L * 99_999);
  }

  @Test
  void provisionalTheUnitsOfBinomialBonusesDoNotAddUpBetweenCalls() {
    // spec: LT-R74e (per call): two calls of 100 bonuses with one context pass
    WatchedRandom random = WatchedRandom.limited(Lcg48Random.ofSeed(1), 40_000_000L);
    LootContext context = context(random);
    for (int call = 0; call < 2; call++) {
      assertThat(ModifierApplier.applyAll(bonuses(100), List.of(LootItems.stack("stone")), context))
          .hasSize(1);
    }
  }

  @Test
  void provisionalABinomialBonusOverTheCapOfTrialsFailsAlone() {
    // spec: LT-R74e: 100001 trials in one modifier, over the cap of 100000 draws
    ItemModifier over =
        Tables.modifier(
            new ModifierAction.ApplyBonus(
                Identifier.minecraft("fortune"),
                new BonusFormula.BinomialWithBonusCount(100_001, 0f)));
    LootContext context = context(WatchedRandom.limited(Lcg48Random.ofSeed(1), 1_000_000L));
    assertThatThrownBy(() -> ModifierApplier.apply(over, LootItems.stack("stone"), context))
        .isInstanceOf(LootBudgetException.class);
  }
}
