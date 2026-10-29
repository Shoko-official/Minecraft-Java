package dev.shoko.minecraft.loot.modifier;

/**
 * The formula of an {@code apply_bonus} modifier (LT-R42). The draws are provisional (probes PL3
 * and PL4); {@code level} is the level of the enchantment on the tool.
 */
public sealed interface BonusFormula
    permits BonusFormula.OreDrops,
        BonusFormula.UniformBonusCount,
        BonusFormula.BinomialWithBonusCount {

  /**
   * {@code minecraft:ore_drops}: the count is multiplied by {@code max(1, nextInt(level + 2))};
   * unchanged at level 0.
   */
  record OreDrops() implements BonusFormula {}

  /**
   * {@code minecraft:uniform_bonus_count}: adds {@code nextInt(level * bonusMultiplier + 1)}.
   *
   * @param bonusMultiplier the multiplier of the level, 1 or 2 in the data
   */
  record UniformBonusCount(int bonusMultiplier) implements BonusFormula {}

  /**
   * {@code minecraft:binomial_with_bonus_count}: adds the successes among {@code level + extra}
   * float draws below {@code probability}.
   *
   * @param extra the trials added to the level, 3 in the data
   * @param probability the probability of one trial
   */
  record BinomialWithBonusCount(int extra, float probability) implements BonusFormula {}
}
