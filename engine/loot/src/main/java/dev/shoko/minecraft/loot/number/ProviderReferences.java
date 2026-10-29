package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootFinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Finds the references of named providers that no loaded provider answers (LT-R50). */
final class ProviderReferences {

  private ProviderReferences() {}

  static List<LootFinding> unresolved(NumberProviderSet set) {
    List<LootFinding> findings = new ArrayList<>();
    for (Map.Entry<Identifier, IntProvider> entry : set.ints().entrySet()) {
      List<Identifier> targets = new ArrayList<>();
      collect(entry.getValue(), targets);
      for (Identifier target : targets) {
        if (set.intProvider(target).isEmpty()) {
          findings.add(NumberProviderCodec.unresolved(entry.getKey(), target));
        }
      }
    }
    for (Map.Entry<Identifier, FloatProvider> entry : set.floats().entrySet()) {
      List<Identifier> targets = new ArrayList<>();
      collect(entry.getValue(), targets);
      for (Identifier target : targets) {
        if (set.floatProvider(target).isEmpty()) {
          findings.add(NumberProviderCodec.unresolved(entry.getKey(), target));
        }
      }
    }
    return findings;
  }

  /** The references of an integer provider tree whose identifiers sit in the int registry. */
  static void collect(IntProvider provider, List<Identifier> out) {
    switch (provider) {
      case IntProvider.Reference reference -> out.add(reference.id());
      case IntProvider.Uniform uniform -> {
        collect(uniform.min(), out);
        collect(uniform.max(), out);
      }
      case IntProvider.Conditional conditional -> {
        collect(conditional.onTrue(), out);
        collect(conditional.onFalse(), out);
      }
      case IntProvider.NumberDispatcher dispatcher -> {
        dispatcher.cases().forEach(c -> collect(c.value(), out));
        collect(dispatcher.fallback(), out);
      }
      case IntProvider.WeightedList list ->
          list.distribution().forEach(e -> collect(e.data(), out));
      case IntProvider.Arithmetic arithmetic -> arithmetic.operands().forEach(o -> collect(o, out));
      default -> {}
    }
  }

  /** The references of a float provider tree whose identifiers sit in the float registry. */
  static void collect(FloatProvider provider, List<Identifier> out) {
    switch (provider) {
      case FloatProvider.Reference reference -> out.add(reference.id());
      case FloatProvider.Uniform uniform -> {
        collect(uniform.min(), out);
        collect(uniform.max(), out);
      }
      case FloatProvider.Conditional conditional -> {
        collect(conditional.onTrue(), out);
        collect(conditional.onFalse(), out);
      }
      case FloatProvider.NumberDispatcher dispatcher -> {
        dispatcher.cases().forEach(c -> collect(c.value(), out));
        collect(dispatcher.fallback(), out);
      }
      case FloatProvider.WeightedList list ->
          list.distribution().forEach(e -> collect(e.data(), out));
      case FloatProvider.Arithmetic arithmetic ->
          arithmetic.operands().forEach(o -> collect(o, out));
      default -> {}
    }
  }
}
