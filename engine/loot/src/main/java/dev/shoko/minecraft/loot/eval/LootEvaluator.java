package dev.shoko.minecraft.loot.eval;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootBudgetException;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.port.RandomSequenceStore;
import dev.shoko.minecraft.loot.random.ChosenRandom;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.random.Draws;
import dev.shoko.minecraft.loot.random.LootRandom;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.table.LootTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws a loot table. Hypothesis H-flow (LT-R19a to LT-R19d): for each pool in order, evaluate its
 * conditions (a failure skips the pool); draw the number of rolls; for each roll expand the
 * entries, choose one by weight, produce its stacks and apply the modifiers of the entry; then the
 * modifiers of the pool to the stacks of the pool; at the end the modifiers of the table to all
 * stacks (LT-R48), and split the stacks above the maximum (LT-R54). Random decisions are made in
 * that order (H-draw, LT-R66) and reported to the observer of the context.
 */
public final class LootEvaluator {

  /** Bound on the stacks a draw gives back, after the split (LT-R74e). */
  private static final long MAX_STACKS = 1_000_000L;

  private LootEvaluator() {}

  /**
   * Draws a table with the source of the context, as a nested table does (LT-R23, LT-R64).
   *
   * @param table the table
   * @param context the context; its source is consumed
   * @return the stacks and the runtime findings
   */
  public static LootResult evaluate(LootTable table, LootContext context) {
    TableDraw draw = new TableDraw(context);
    context
        .data()
        .tables()
        .tables()
        .forEach(
            (id, loaded) -> {
              if (loaded.equals(table)) {
                draw.enter(id);
              }
            });
    return result(draw.table(table, 0, "$"), draw);
  }

  /**
   * Draws a loaded table by identifier with the source of the context.
   *
   * @param id a table identifier
   * @param context the context
   * @return the stacks; empty with a {@code TABLE_NOT_FOUND} finding when the table is not loaded
   */
  public static LootResult evaluate(Identifier id, LootContext context) {
    Optional<LootTable> table = context.data().tables().find(id);
    if (table.isEmpty()) {
      return notFound(id);
    }
    TableDraw draw = new TableDraw(context);
    draw.enter(id);
    return result(draw.table(table.get(), 0, "$"), draw);
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
    ChosenRandom chosen =
        LootRandom.choose(explicitSeed, table.randomSequence(), sequences, context.random());
    return evaluate(table, context.withRandom(chosen.source()));
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
    return Draws.observe(
        context,
        DrawReason.POOL_ROLLS,
        "rolls",
        () -> {
          long rolls = pool.rolls().sample(context);
          if (rolls > TableDraw.MAX_ROLLS) {
            throw new LootBudgetException("a pool asks for " + rolls + " rolls");
          }
          // The bonus comes from the luck of the context, an input and not data: it is bounded to
          // the cap on its own, without an error, and the sum with the rolls may reach twice the
          // cap (LT-R74a).
          float bonus = pool.bonusRolls().sample(context) * context.luck();
          if (!Float.isNaN(bonus)) {
            rolls +=
                (long)
                    Math.floor(
                        Math.max(-TableDraw.MAX_ROLLS, Math.min(TableDraw.MAX_ROLLS, bonus)));
          }
          return (int) Math.max(0, rolls);
        });
  }

  private static LootResult notFound(Identifier id) {
    return new LootResult(
        List.of(),
        List.of(
            new LootFinding(LootFindingCode.TABLE_NOT_FOUND, "$", "table is not loaded: " + id)));
  }

  private static LootResult result(List<ItemStack> drawn, TableDraw draw) {
    long needed = 0;
    for (ItemStack stack : drawn) {
      if (!stack.isEmpty()) {
        needed += ((long) stack.count() + stack.maxStackSize() - 1) / stack.maxStackSize();
      }
    }
    if (needed > MAX_STACKS) {
      throw new LootBudgetException("a draw gives " + needed + " stacks, above " + MAX_STACKS);
    }
    List<ItemStack> stacks = new ArrayList<>();
    for (ItemStack stack : drawn) {
      if (!stack.isEmpty()) {
        stacks.addAll(split(stack));
      }
    }
    return new LootResult(List.copyOf(stacks), List.copyOf(draw.findings()));
  }

  /** Splits a stack above its maximum into stacks of the maximum and a rest (LT-R54). */
  static List<ItemStack> split(ItemStack stack) {
    if (stack.count() <= stack.maxStackSize()) {
      return List.of(stack);
    }
    return stack.stacksFor(stack.count());
  }
}
