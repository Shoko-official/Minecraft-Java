package dev.shoko.minecraft.loot.eval;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.port.RandomSequenceStore;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.table.LootTable;

/**
 * Draws a loot table. Hypothesis H-flow (LT-R19a to LT-R19d): for each pool in order, evaluate its
 * conditions (a failure skips the pool); draw the number of rolls; for each roll expand the
 * entries, choose one by weight, produce its stacks and apply the modifiers of the entry; then the
 * modifiers of the pool to the stacks of the pool; at the end the modifiers of the table to all
 * stacks (LT-R48), and split the stacks above the maximum (LT-R54). Random decisions are made in
 * that order (H-draw, LT-R66) and reported to the observer of the context.
 */
public final class LootEvaluator {

  private LootEvaluator() {}

  /**
   * Draws a table with the source of the context, as a nested table does (LT-R23, LT-R64).
   *
   * @param table the table
   * @param context the context; its source is consumed
   * @return the stacks and the runtime findings
   */
  public static LootResult evaluate(LootTable table, LootContext context) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Draws a loaded table by identifier with the source of the context.
   *
   * @param id a table identifier
   * @param context the context
   * @return the stacks; empty with a {@code TABLE_NOT_FOUND} finding when the table is not loaded
   */
  public static LootResult evaluate(Identifier id, LootContext context) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Draws a table as an entry point does (command, container, block, mob): the source is chosen
   * with LT-R63 from the explicit seed, the named sequence of the table and the source of the
   * context, then the table is drawn with it.
   *
   * @param table the table
   * @param context the context; its source is the source of the level
   * @param explicitSeed the seed of the command or of {@code LootTableSeed}, 0 when absent
   * @param sequences the persistent named sequences
   * @return the stacks and the runtime findings
   */
  public static LootResult evaluateTopLevel(
      LootTable table, LootContext context, long explicitSeed, RandomSequenceStore sequences) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Computes the number of rolls of a pool (LT-R19b): {@code rolls + floor(bonus_rolls * luck)}. A
   * value of 0 or less gives no roll (LT-R6, provisional). Draws for a random {@code rolls} and
   * reports it with the reason {@code POOL_ROLLS}.
   *
   * @param pool the pool
   * @param context the context
   * @return the number of rolls, 0 or more
   */
  public static int rolls(LootPool pool, LootContext context) {
    return Skeleton.throwNotImplemented();
  }
}
