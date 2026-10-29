package dev.shoko.minecraft.loot.assembly;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.assembly.References.Kind;
import dev.shoko.minecraft.loot.assembly.References.Ref;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;

/**
 * Removes the named predicates and providers that cannot be evaluated: a reference to an element
 * that does not exist, a cycle of references (LT-R76), and every element that depends on a removed
 * one. The check runs on the decoded elements, before any evaluation (LT-R77).
 */
final class NamedPruning {

  private static final Identifier PREDICATE = Identifier.minecraft("predicate");
  private static final Identifier INT_PROVIDER = Identifier.minecraft("context_int_provider");
  private static final Identifier FLOAT_PROVIDER = Identifier.minecraft("context_float_provider");

  private NamedPruning() {}

  /** Prunes the three maps in place and appends one finding per removed element. */
  static void prune(
      SequencedMap<Identifier, LootCondition> predicates,
      SequencedMap<Identifier, IntProvider> ints,
      SequencedMap<Identifier, FloatProvider> floats,
      List<LootLoadFinding> findings) {
    Map<Ref, List<Ref>> edges = new LinkedHashMap<>();
    ints.forEach((id, p) -> edges.put(new Ref(Kind.INT_PROVIDER, id), References.of(p)));
    floats.forEach((id, p) -> edges.put(new Ref(Kind.FLOAT_PROVIDER, id), References.of(p)));
    predicates.forEach((id, c) -> edges.put(new Ref(Kind.PREDICATE, id), References.of(c)));
    Map<Ref, LootFinding> removed = new LinkedHashMap<>();
    Set<Ref> cyclic = ReferenceGraph.cyclic(edges);
    for (Map.Entry<Ref, List<Ref>> entry : edges.entrySet()) {
      Ref node = entry.getKey();
      if (cyclic.contains(node)) {
        removed.put(node, finding(LootFindingCode.REFERENCE_CYCLE, "part of a reference cycle"));
      } else {
        entry.getValue().stream()
            .filter(target -> !edges.containsKey(target))
            .findFirst()
            .ifPresent(
                target ->
                    removed.put(
                        node,
                        finding(
                            LootFindingCode.UNRESOLVED_REFERENCE,
                            "references a missing element " + target.id())));
      }
    }
    propagate(edges, removed);
    for (Map.Entry<Ref, LootFinding> entry : removed.entrySet()) {
      Ref node = entry.getKey();
      findings.add(new LootLoadFinding(registry(node.kind()), node.id(), entry.getValue()));
      switch (node.kind()) {
        case INT_PROVIDER -> ints.remove(node.id());
        case FLOAT_PROVIDER -> floats.remove(node.id());
        default -> predicates.remove(node.id());
      }
    }
  }

  private static void propagate(Map<Ref, List<Ref>> edges, Map<Ref, LootFinding> removed) {
    boolean changed = true;
    Set<Ref> gone = new HashSet<>(removed.keySet());
    while (changed) {
      changed = false;
      for (Map.Entry<Ref, List<Ref>> entry : new ArrayList<>(edges.entrySet())) {
        if (gone.contains(entry.getKey())) {
          continue;
        }
        for (Ref target : entry.getValue()) {
          if (gone.contains(target)) {
            gone.add(entry.getKey());
            removed.put(
                entry.getKey(),
                finding(
                    LootFindingCode.UNRESOLVED_REFERENCE,
                    "depends on a refused element " + target.id()));
            changed = true;
            break;
          }
        }
      }
    }
  }

  private static LootFinding finding(LootFindingCode code, String message) {
    return new LootFinding(code, "$", message);
  }

  private static Identifier registry(Kind kind) {
    return switch (kind) {
      case INT_PROVIDER -> INT_PROVIDER;
      case FLOAT_PROVIDER -> FLOAT_PROVIDER;
      default -> PREDICATE;
    };
  }
}
