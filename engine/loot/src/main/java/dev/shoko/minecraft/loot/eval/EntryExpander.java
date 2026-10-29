package dev.shoko.minecraft.loot.eval;

import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootPool;
import java.util.List;

/** Turns the entries of a pool into the candidates of one roll (LT-R20 to LT-R22). */
public final class EntryExpander {

  private EntryExpander() {}

  /**
   * Evaluates the conditions of the entries of a pool and expands them, for one roll. Conditions
   * are re-evaluated at each roll and may draw (LT-R20); each draw is reported with the reason
   * {@code ENTRY_CONDITION}. Composites expand into simple entries before the choice, without a
   * weight of their own: {@code alternatives} keeps the first child whose condition holds, {@code
   * sequence} the children before the first failure, {@code group} all of them (LT-R21); {@code
   * tag} with {@code expand} gives one {@code item} child per member with the weight and conditions
   * of the entry (LT-R22). Entries of effective weight 0 are dropped (LT-R25).
   *
   * @param pool the pool
   * @param context the context; its source is consumed by random conditions
   * @return the candidates in the order of the pool
   */
  public static List<Candidate> candidates(LootPool pool, LootContext context) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Expands one entry to simple entries, as a roll would.
   *
   * @param entry any entry
   * @param context the context
   * @return the simple entries it stands for, empty when its conditions fail
   */
  public static List<LootEntry> expand(LootEntry entry, LootContext context) {
    return Skeleton.throwNotImplemented();
  }
}
