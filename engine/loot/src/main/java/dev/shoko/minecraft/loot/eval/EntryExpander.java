package dev.shoko.minecraft.loot.eval;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.random.Draws;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootPool;
import java.util.ArrayList;
import java.util.List;

/** Turns the entries of a pool into the candidates of one roll (LT-R20 to LT-R22). */
public final class EntryExpander {

  private static final Identifier ITEM_REGISTRY = Identifier.minecraft("item");

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
    List<Candidate> candidates = new ArrayList<>();
    for (LootEntry entry : pool.entries()) {
      for (LootEntry simple : expand(entry, context)) {
        int weight = WeightMath.effectiveWeight(simple.weight(), simple.quality(), context.luck());
        if (weight > 0) {
          candidates.add(new Candidate(simple, weight));
        }
      }
    }
    return candidates;
  }

  /**
   * Expands one entry to simple entries, as a roll would.
   *
   * @param entry any entry
   * @param context the context
   * @return the simple entries it stands for, empty when its conditions fail
   */
  public static List<LootEntry> expand(LootEntry entry, LootContext context) {
    List<LootEntry> out = new ArrayList<>();
    expand(entry, context, out);
    return out;
  }

  /** Expands an entry into {@code out}; tells whether the entry succeeded (LT-R21). */
  private static boolean expand(LootEntry entry, LootContext context, List<LootEntry> out) {
    if (entry.kind() instanceof LootEntry.Kind.Tag tag && tag.expand()) {
      return expandTag(entry, tag, context, out);
    }
    if (!Draws.allHold(context, DrawReason.ENTRY_CONDITION, "entry", entry.conditions())) {
      return false;
    }
    return switch (entry.kind()) {
      case LootEntry.Kind.Alternatives alternatives -> alternatives(alternatives, context, out);
      case LootEntry.Kind.Sequence sequence -> sequence(sequence, context, out);
      case LootEntry.Kind.Group group -> {
        for (LootEntry child : group.children()) {
          expand(child, context, out);
        }
        yield true;
      }
      default -> out.add(entry);
    };
  }

  private static boolean alternatives(
      LootEntry.Kind.Alternatives alternatives, LootContext context, List<LootEntry> out) {
    for (LootEntry child : alternatives.children()) {
      List<LootEntry> chosen = new ArrayList<>();
      if (expand(child, context, chosen)) {
        out.addAll(chosen);
        return true;
      }
    }
    return false;
  }

  private static boolean sequence(
      LootEntry.Kind.Sequence sequence, LootContext context, List<LootEntry> out) {
    for (LootEntry child : sequence.children()) {
      if (!expand(child, context, out)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Expands a tag into one child per item. Each child carries its own copy of the condition of the
   * entry, evaluated separately and in the order of the tag (LT-R22, H-tag-cond-b).
   */
  private static boolean expandTag(
      LootEntry entry, LootEntry.Kind.Tag tag, LootContext context, List<LootEntry> out) {
    IdOrTagSet members = new IdOrTagSet(ITEM_REGISTRY, List.of(), List.of(tag.tag()), false);
    boolean any = false;
    boolean empty = true;
    for (Identifier id : members.members(context.data().registries())) {
      ItemType item = context.data().items().find(id).orElse(null);
      if (item == null) {
        continue;
      }
      empty = false;
      if (Draws.allHold(context, DrawReason.ENTRY_CONDITION, "entry", entry.conditions())) {
        any = true;
        out.add(
            new LootEntry(
                List.of(),
                entry.weight(),
                entry.quality(),
                entry.modifiers(),
                new LootEntry.Kind.Item(item)));
      }
    }
    return any || empty;
  }
}
