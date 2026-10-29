package dev.shoko.minecraft.loot;

/**
 * Raised when an evaluation exceeds one of the design budgets of LT-R74 (rolls of a pool, trials of
 * a binomial, nesting of tables, stacks of a draw, global work of a draw). The draw is abandoned:
 * no partial list of stacks is given back and nothing is truncated silently (LT-R74g).
 */
public final class LootBudgetException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message what was exceeded, for humans
   */
  public LootBudgetException(String message) {
    super(message);
  }
}
