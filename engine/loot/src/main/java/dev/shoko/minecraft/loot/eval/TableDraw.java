package dev.shoko.minecraft.loot.eval;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootBudgetException;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.context.NodeBudget;
import dev.shoko.minecraft.loot.modifier.ModifierApplier;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.random.Draws;
import dev.shoko.minecraft.loot.random.Weights;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.table.LootTable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * One draw of a table with the source of a context (hypothesis H-flow, LT-R19a to LT-R19d). It
 * keeps the runtime findings and the named tables being drawn, so that a cycle of references ends.
 */
final class TableDraw {

  /** Bound on the nesting of tables, which JSON itself keeps below 256 levels. */
  private static final int MAX_DEPTH = 255;

  /** Bound on the rolls of a pool (LT-R74a). */
  static final int MAX_ROLLS = 4096;

  /** Bound on a weight, so that the sum of the weights of a roll cannot overflow. */
  private static final int MAX_WEIGHT = 1 << 20;

  private final LootContext context;
  private final List<LootFinding> findings = new ArrayList<>();
  private final Set<Identifier> active = new HashSet<>();

  TableDraw(LootContext caller) {
    this.context = caller;
  }

  List<LootFinding> findings() {
    return findings;
  }

  void enter(Identifier id) {
    active.add(id);
  }

  /** Draws a root table, within the budget of one call of the entry point. */
  List<ItemStack> root(LootTable table) {
    NodeBudget budget = context.budget();
    budget.enter();
    try {
      return table(table, 0, "$");
    } finally {
      budget.exit();
    }
  }

  /** Draws a table: its pools in order, then its modifiers on every stack. No split. */
  List<ItemStack> table(LootTable table, int depth, String path) {
    List<ItemStack> stacks = new ArrayList<>();
    for (int i = 0; i < table.pools().size(); i++) {
      pool(table.pools().get(i), stacks, depth, path + ".pools[" + i + "]");
    }
    return ModifierApplier.applyAll(table.modifiers(), stacks, context);
  }

  private void pool(LootPool pool, List<ItemStack> out, int depth, String path) {
    context.budget().node();
    if (!Draws.allHold(context, DrawReason.POOL_CONDITION, path, pool.conditions())) {
      return;
    }
    int rolls = LootEvaluator.rolls(pool, context);
    for (int roll = 0; roll < rolls; roll++) {
      List<ItemStack> produced = roll(pool, depth, path);
      out.addAll(ModifierApplier.applyAll(pool.modifiers(), produced, context));
    }
  }

  private List<ItemStack> roll(LootPool pool, int depth, String path) {
    context.budget().node();
    List<Candidate> candidates = EntryExpander.candidates(pool, context);
    Candidate chosen =
        Draws.observe(context, DrawReason.ENTRY_SELECTION, path, () -> select(candidates));
    if (chosen == null) {
      return List.of();
    }
    List<ItemStack> produced = produce(chosen.entry(), depth, path);
    return ModifierApplier.applyAll(chosen.entry().modifiers(), produced, context);
  }

  private Candidate select(List<Candidate> candidates) {
    int[] weights =
        candidates.stream().mapToInt(c -> Math.min(c.effectiveWeight(), MAX_WEIGHT)).toArray();
    RandomSource random = context.random();
    OptionalInt picked = Weights.pick(weights, random);
    return picked.isPresent() ? candidates.get(picked.getAsInt()) : null;
  }

  private List<ItemStack> produce(LootEntry entry, int depth, String path) {
    return switch (entry.kind()) {
      case LootEntry.Kind.Item item -> List.of(ItemStack.of(item.item(), 1));
      case LootEntry.Kind.Tag tag -> tagStacks(tag);
      case LootEntry.Kind.LootTableRef ref -> nested(ref.table(), depth, path);
      case LootEntry.Kind.Dynamic dynamic -> {
        findings.add(
            new LootFinding(
                LootFindingCode.DYNAMIC_SOURCE_MISSING,
                path,
                "no emitter for the dynamic source " + dynamic.name()));
        yield List.of();
      }
      default -> List.of();
    };
  }

  private List<ItemStack> tagStacks(LootEntry.Kind.Tag tag) {
    IdOrTagSet members =
        new IdOrTagSet(Identifier.minecraft("item"), List.of(), List.of(tag.tag()), false);
    List<ItemStack> stacks = new ArrayList<>();
    for (Identifier id : members.members(context.data().registries())) {
      ItemType item = context.data().items().find(id).orElse(null);
      if (item != null) {
        stacks.add(ItemStack.of(item, 1));
      }
    }
    return stacks;
  }

  private List<ItemStack> nested(LootEntry.TableRef ref, int depth, String path) {
    if (depth >= MAX_DEPTH) {
      throw new LootBudgetException("tables nested more than " + MAX_DEPTH + " levels at " + path);
    }
    return switch (ref) {
      case LootEntry.TableRef.Inline inline -> table(inline.table(), depth + 1, path);
      case LootEntry.TableRef.Named named -> named(named.id(), depth, path);
    };
  }

  private List<ItemStack> named(Identifier id, int depth, String path) {
    LootTable table = context.data().tables().find(id).orElse(null);
    if (table == null) {
      findings.add(
          new LootFinding(LootFindingCode.TABLE_NOT_FOUND, path, "table is not loaded: " + id));
      return List.of();
    }
    if (!active.add(id)) {
      findings.add(
          new LootFinding(LootFindingCode.SELF_REFERENCE, path, "table reference cycle at " + id));
      return List.of();
    }
    try {
      return table(table, depth + 1, path);
    } finally {
      active.remove(id);
    }
  }
}
