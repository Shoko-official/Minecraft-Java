package dev.shoko.minecraft.loot.context;

import dev.shoko.minecraft.loot.LootBudgetException;

/**
 * The counters of the work of one call of an entry point (LT-R74d to LT-R74f): the nodes evaluated
 * (conditions, number providers, pool draws, selections, modifiers, entries), the units walked one
 * by one, and the depth of the chain of named references being followed. The counters are plain
 * fields: no allocation per node.
 *
 * <p>A context carries one budget, shared by the contexts derived from it. Every public entry point
 * calls {@link #enter()} before its work and {@link #exit()} after it, in a {@code finally}: the
 * outermost entry starts the counters from zero, an entry nested in a call in progress counts on
 * the same counters. The budget is therefore per call of the entry point, never per context: a
 * context serves any number of small calls, and a call that failed leaves nothing behind. A budget
 * is not thread-safe, like the source of the context.
 */
public final class NodeBudget {

  /** Cap of the nodes of a call (LT-R74f). */
  public static final long MAX_NODES = 1_000_000L;

  /** Cap of the units walked one by one over a call: decay, binomial trials (LT-R74e). */
  public static final long MAX_UNITS = 10_000_000L;

  /** Cap of the links of a chain of named references (LT-R74d). */
  public static final int MAX_REFERENCE_DEPTH = 64;

  private int entries;
  private long nodes;
  private long units;
  private int depth;

  /** Starts an entry point: the outermost one resets the counters, a nested one shares them. */
  public void enter() {
    if (entries++ == 0) {
      nodes = 0;
      units = 0;
      depth = 0;
    }
  }

  /** Ends the entry point started last. */
  public void exit() {
    entries--;
  }

  /** Counts one evaluated node. */
  public void node() {
    if (++nodes > MAX_NODES) {
      throw new LootBudgetException("a call evaluated more than " + MAX_NODES + " nodes");
    }
  }

  /**
   * Counts units walked one by one, before the walk. One walk above the cap fails whatever the rest
   * of the call; the sum of the walks of a call is bounded too.
   *
   * @param count the units about to be walked
   */
  public void units(long count) {
    if (count > MAX_UNITS) {
      throw new LootBudgetException("a walk of " + count + " units, above " + MAX_UNITS);
    }
    units += count;
    if (units > MAX_UNITS) {
      throw new LootBudgetException("a call walked more than " + MAX_UNITS + " units");
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
