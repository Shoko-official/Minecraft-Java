package dev.shoko.minecraft.loot.table;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.LootContextType;
import dev.shoko.minecraft.loot.context.LootContextTypes;
import dev.shoko.minecraft.loot.modifier.ItemModifier;
import dev.shoko.minecraft.loot.modifier.ModifierAction;
import dev.shoko.minecraft.loot.predicate.ConditionReads;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.NamedPredicates;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks the references of a decoded table once the other registries are known (LT-R23, LT-R61b).
 */
final class TableReferences {

  private final Identifier self;
  private final LootTables known;
  private final NamedPredicates predicates;
  private final List<LootFinding> findings = new ArrayList<>();

  TableReferences(Identifier self, LootTables known, NamedPredicates predicates) {
    this.self = self;
    this.known = known;
    this.predicates = predicates;
  }

  List<LootFinding> check(LootTable table) {
    table(table, "$", table.type());
    return List.copyOf(findings);
  }

  private void table(LootTable table, String path, LootContextType parentType) {
    LootContextType type =
        table.type().equals(LootContextTypes.generic()) ? parentType : table.type();
    for (int i = 0; i < table.pools().size(); i++) {
      pool(table.pools().get(i), path + ".pools[" + i + "]", type);
    }
    modifiers(table.modifiers(), path + ".modifier", type);
  }

  private void pool(LootPool pool, String path, LootContextType type) {
    conditions(pool.conditions(), path + ".condition", type);
    for (int i = 0; i < pool.entries().size(); i++) {
      entry(pool.entries().get(i), path + ".entries[" + i + "]", type);
    }
    modifiers(pool.modifiers(), path + ".modifier", type);
  }

  private void entry(LootEntry entry, String path, LootContextType type) {
    conditions(entry.conditions(), path + ".condition", type);
    switch (entry.kind()) {
      case LootEntry.Kind.LootTableRef ref -> reference(ref.table(), path + ".value", type);
      case LootEntry.Kind.Alternatives group -> children(group.children(), path, type);
      case LootEntry.Kind.Sequence group -> children(group.children(), path, type);
      case LootEntry.Kind.Group group -> children(group.children(), path, type);
      default -> {}
    }
    modifiers(entry.modifiers(), path + ".modifier", type);
  }

  private void children(List<LootEntry> children, String path, LootContextType type) {
    for (int i = 0; i < children.size(); i++) {
      entry(children.get(i), path + ".children[" + i + "]", type);
    }
  }

  private void reference(LootEntry.TableRef ref, String path, LootContextType type) {
    switch (ref) {
      case LootEntry.TableRef.Inline inline -> table(inline.table(), path, type);
      case LootEntry.TableRef.Named named -> {
        if (named.id().equals(self)) {
          add(LootFindingCode.SELF_REFERENCE, path, "table references itself");
        } else if (known.find(named.id()).isEmpty()) {
          add(LootFindingCode.UNRESOLVED_REFERENCE, path, "table is not loaded: " + named.id());
        }
      }
    }
  }

  private void modifiers(List<ItemModifier> modifiers, String path, LootContextType type) {
    for (int i = 0; i < modifiers.size(); i++) {
      String here = path + "[" + i + "]";
      ItemModifier modifier = modifiers.get(i);
      conditions(modifier.conditions(), here + ".condition", type);
      if (modifier.action() instanceof ModifierAction.Filtered filtered) {
        modifiers(filtered.onPass(), here + ".on_pass", type);
        modifiers(filtered.onFail(), here + ".on_fail", type);
      }
    }
  }

  private void conditions(List<LootCondition> conditions, String path, LootContextType type) {
    for (int i = 0; i < conditions.size(); i++) {
      String here = path + "[" + i + "]";
      resolve(conditions.get(i), here);
      for (LootFinding unavailable :
          ConditionReads.unavailable(List.of(conditions.get(i)), type, predicates)) {
        add(unavailable.code(), here, unavailable.message());
      }
    }
  }

  private void resolve(LootCondition condition, String path) {
    switch (condition) {
      case LootCondition.Reference reference -> {
        if (predicates.find(reference.predicate()).isEmpty()) {
          add(
              LootFindingCode.UNRESOLVED_REFERENCE,
              path,
              "named predicate is not loaded: " + reference.predicate());
        }
      }
      case LootCondition.AllOf all -> all.terms().forEach(term -> resolve(term, path));
      case LootCondition.AnyOf any -> any.terms().forEach(term -> resolve(term, path));
      case LootCondition.Inverted inverted -> resolve(inverted.term(), path);
      default -> {}
    }
  }

  private void add(LootFindingCode code, String path, String message) {
    findings.add(new LootFinding(code, path, message));
  }
}
