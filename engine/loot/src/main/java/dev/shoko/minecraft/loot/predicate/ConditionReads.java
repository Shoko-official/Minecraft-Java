package dev.shoko.minecraft.loot.predicate;

import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.context.ContextKey;
import dev.shoko.minecraft.loot.context.LootContextType;
import java.util.List;
import java.util.Set;

/**
 * Which context parameters a condition reads (LT-R14b, LT-R15), for the load-time warning and for
 * tests of the parameter sets. A named reference is followed through the loaded predicates.
 */
public final class ConditionReads {

  private ConditionReads() {}

  /**
   * Lists the parameters a condition reads, nested terms included.
   *
   * @param condition a condition
   * @param named the named predicates, to follow references
   * @return the keys read, in enum order
   */
  public static Set<ContextKey> of(LootCondition condition, NamedPredicates named) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reports the conditions of a table that read a parameter its type does not provide (LT-R15). The
   * table still loads.
   *
   * @param conditions the conditions of the table, in the order of the table
   * @param type the context type of the table
   * @param named the named predicates, to follow references
   * @return one {@code UNAVAILABLE_PARAMETER} warning per condition and missing parameter
   */
  public static List<LootFinding> unavailable(
      List<LootCondition> conditions, LootContextType type, NamedPredicates named) {
    return Skeleton.throwNotImplemented();
  }
}
