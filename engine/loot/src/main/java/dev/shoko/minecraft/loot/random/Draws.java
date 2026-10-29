package dev.shoko.minecraft.loot.random;

import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.List;
import java.util.function.Supplier;

/**
 * Reports the random decisions of an evaluation to the observer of the context: the steps of the
 * source are read before and after the decision, so the code that draws never counts its own draws.
 */
public final class Draws {

  private Draws() {}

  /**
   * Runs a decision and reports it.
   *
   * @param context the context, whose source is consumed by the decision
   * @param reason the kind of decision
   * @param path where, for the report
   * @param decision the decision
   * @param <T> what the decision yields
   * @return what the decision yielded
   */
  public static <T> T observe(
      LootContext context, DrawReason reason, String path, Supplier<T> decision) {
    long before = context.random().steps();
    T result = decision.get();
    context.observer().onDraw(new DrawEvent(reason, path, before, context.random().steps()));
    return result;
  }

  /**
   * Tests a condition and reports the draws it made.
   *
   * @param context the context
   * @param reason the kind of decision
   * @param path where, for the report
   * @param condition the condition
   * @return whether it holds
   */
  public static boolean holds(
      LootContext context, DrawReason reason, String path, LootCondition condition) {
    return observe(context, reason, path, () -> condition.test(context).passed());
  }

  /**
   * Tests conditions in order, stopping at the first that fails, and reports each one tested.
   *
   * @param context the context
   * @param reason the kind of decision
   * @param path where, for the report
   * @param conditions the conditions in order
   * @return whether every condition holds
   */
  public static boolean allHold(
      LootContext context, DrawReason reason, String path, List<LootCondition> conditions) {
    for (int i = 0; i < conditions.size(); i++) {
      if (!holds(context, reason, path + ".condition[" + i + "]", conditions.get(i))) {
        return false;
      }
    }
    return true;
  }
}
