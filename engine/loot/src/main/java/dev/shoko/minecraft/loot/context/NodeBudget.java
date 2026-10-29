package dev.shoko.minecraft.loot.context;

import dev.shoko.minecraft.loot.LootBudgetException;

/**
 * The counters of the work of one root draw (LT-R74d to LT-R74f): the nodes evaluated (conditions,
 * number providers, pool draws, selections, modifiers, entries), the units walked one by one, and
 * the depth of the chain of named references being followed. The counters are plain fields: no
 * allocation per node. A budget made for a draw ends it with a {@link LootBudgetException} when a
 * cap is passed; the budget of a context built by a caller counts only the depth of references, so
 * that direct uses of a condition or a provider are not limited.
 */
public final class NodeBudget {

  /** Cap of the nodes of a root draw (LT-R74f). */
  public static final long MAX_NODES = 1_000_000L;

  /** Cap of the units walked one by one (explosion decay) over a root draw (LT-R41, LT-R74e). */
  public static final long MAX_UNITS = 10_000_000L;

  /** Cap of the links of a chain of named references (LT-R74d). */
  public static final int MAX_REFERENCE_DEPTH = 64;

  private final boolean limited;
  private long nodes;
  private long units;
  private int depth;

  private NodeBudget(boolean limited) {
    this.limited = limited;
  }

  /**
   * Makes the budget of a context that is not drawing a table: only the chain of references is
   * bounded.
   *
   * @return a budget that counts no work
   */
  public static NodeBudget unlimited() {
    return new NodeBudget(false);
  }

  /**
   * Makes the budget of one root draw.
   *
   * @return a budget with every counter at zero
   */
  public static NodeBudget forDraw() {
    return new NodeBudget(true);
  }

  /** Counts one evaluated node. */
  public void node() {
    if (limited && ++nodes > MAX_NODES) {
      throw new LootBudgetException("a draw evaluated more than " + MAX_NODES + " nodes");
    }
  }

  /**
   * Counts units walked one by one, before the walk. One walk above the cap fails whatever the
   * budget; a draw also bounds the sum of its walks.
   *
   * @param count the units about to be walked
   */
  public void units(long count) {
    if (count > MAX_UNITS) {
      throw new LootBudgetException("a walk of " + count + " units, above " + MAX_UNITS);
    }
    if (limited) {
      units += count;
      if (units > MAX_UNITS) {
        throw new LootBudgetException("a draw walked more than " + MAX_UNITS + " units");
      }
    }
  }

  /** Enters one named reference; the chain is bounded whatever the family of the elements. */
  public void enterReference() {
    if (depth >= MAX_REFERENCE_DEPTH) {
      throw new LootBudgetException(
          "a chain of named references is longer than " + MAX_REFERENCE_DEPTH);
    }
    depth++;
  }

  /** Leaves the named reference entered last. */
  public void exitReference() {
    depth--;
  }
}
