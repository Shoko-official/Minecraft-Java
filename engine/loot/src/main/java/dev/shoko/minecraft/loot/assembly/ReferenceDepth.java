package dev.shoko.minecraft.loot.assembly;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.assembly.References.Kind;
import dev.shoko.minecraft.loot.assembly.References.Ref;
import dev.shoko.minecraft.loot.context.NodeBudget;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.table.LootTable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Measures the chains of named references (LT-R74d): the number of links from an element down to a
 * constant, across the three families. A table whose longest chain passes the bound of the
 * evaluation is refused at load, instead of failing at its first draw. The search is iterative, so
 * a very long chain cannot overflow the stack.
 */
final class ReferenceDepth {

  private final Map<Ref, List<Ref>> edges = new LinkedHashMap<>();
  private final Map<Ref, Integer> heights = new HashMap<>();

  /** One frame of the explicit call stack: a node, the position in its edges, the best height. */
  private static final class Frame {
    final Ref node;
    final List<Ref> targets;
    int position;
    int best;

    Frame(Ref node, List<Ref> targets) {
      this.node = node;
      this.targets = targets;
    }
  }

  /**
   * Measures the named elements that survived the pruning of cycles and missing references.
   *
   * @param predicates the named predicates
   * @param ints the named integer providers
   * @param floats the named float providers
   */
  ReferenceDepth(
      Map<Identifier, LootCondition> predicates,
      Map<Identifier, IntProvider> ints,
      Map<Identifier, FloatProvider> floats) {
    ints.forEach((id, p) -> edges.put(new Ref(Kind.INT_PROVIDER, id), References.of(p)));
    floats.forEach((id, p) -> edges.put(new Ref(Kind.FLOAT_PROVIDER, id), References.of(p)));
    predicates.forEach((id, c) -> edges.put(new Ref(Kind.PREDICATE, id), References.of(c)));
    for (Ref root : edges.keySet()) {
      if (!heights.containsKey(root)) {
        measure(root);
      }
    }
  }

  /**
   * Checks the longest chain a table starts, its first link being the reference the table makes.
   *
   * @param table a decoded table
   * @return a finding when the chain is longer than the bound of the evaluation, else empty
   */
  Optional<LootFinding> check(LootTable table) {
    int links = 0;
    for (Ref ref : References.of(table)) {
      if (ref.kind() != Kind.TABLE) {
        links = Math.max(links, 1 + heights.getOrDefault(ref, 0));
      }
    }
    if (links <= NodeBudget.MAX_REFERENCE_DEPTH) {
      return Optional.empty();
    }
    return Optional.of(
        new LootFinding(
            LootFindingCode.REFERENCE_TOO_DEEP,
            "$",
            "a chain of "
                + links
                + " named references, above the bound of "
                + NodeBudget.MAX_REFERENCE_DEPTH));
  }

  private void measure(Ref root) {
    Deque<Frame> calls = new ArrayDeque<>();
    Set<Ref> open = new HashSet<>();
    calls.push(new Frame(root, edges.get(root)));
    open.add(root);
    while (!calls.isEmpty()) {
      Frame frame = calls.peek();
      if (frame.position < frame.targets.size()) {
        Ref target = frame.targets.get(frame.position++);
        Integer known = heights.get(target);
        if (known != null) {
          frame.best = Math.max(frame.best, 1 + known);
        } else if (edges.containsKey(target) && open.add(target)) {
          calls.push(new Frame(target, edges.get(target)));
        }
      } else {
        calls.pop();
        open.remove(frame.node);
        heights.put(frame.node, frame.best);
        if (!calls.isEmpty()) {
          Frame parent = calls.peek();
          parent.best = Math.max(parent.best, 1 + frame.best);
        }
      }
    }
  }
}
