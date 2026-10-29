package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.modifier.ItemModifier;
import dev.shoko.minecraft.loot.modifier.ModifierAction;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.random.SequenceRef;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.table.LootTable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Short constructors for tables, pools, entries and modifiers, built from the plain records. */
public final class Tables {

  private Tables() {}

  public static LootEntry entry(LootEntry.Kind kind, int weight, int quality) {
    return new LootEntry(List.of(), weight, quality, List.of(), kind);
  }

  public static LootEntry item(String path) {
    return item(path, 1);
  }

  public static LootEntry item(String path, int weight) {
    return entry(new LootEntry.Kind.Item(LootItems.type(path)), weight, 0);
  }

  public static LootEntry item(String path, int weight, int quality) {
    return entry(new LootEntry.Kind.Item(LootItems.type(path)), weight, quality);
  }

  public static LootEntry empty(int weight) {
    return entry(new LootEntry.Kind.Empty(), weight, 0);
  }

  public static LootEntry ref(String table) {
    return entry(
        new LootEntry.Kind.LootTableRef(new LootEntry.TableRef.Named(Identifier.minecraft(table))),
        1,
        0);
  }

  public static LootEntry ref(String table, int weight, int quality) {
    return entry(
        new LootEntry.Kind.LootTableRef(new LootEntry.TableRef.Named(Identifier.minecraft(table))),
        weight,
        quality);
  }

  public static LootEntry inline(LootTable table) {
    return entry(new LootEntry.Kind.LootTableRef(new LootEntry.TableRef.Inline(table)), 1, 0);
  }

  public static LootEntry dynamic(String name) {
    return entry(new LootEntry.Kind.Dynamic(Identifier.minecraft(name)), 1, 0);
  }

  public static LootEntry tag(String tag, boolean expand, int weight) {
    return entry(new LootEntry.Kind.Tag(Identifier.minecraft(tag), expand), weight, 0);
  }

  public static LootEntry alternatives(LootEntry... children) {
    return entry(new LootEntry.Kind.Alternatives(List.of(children)), 1, 0);
  }

  public static LootEntry sequence(LootEntry... children) {
    return entry(new LootEntry.Kind.Sequence(List.of(children)), 1, 0);
  }

  public static LootEntry group(LootEntry... children) {
    return entry(new LootEntry.Kind.Group(List.of(children)), 1, 0);
  }

  public static LootEntry when(LootEntry entry, LootCondition... conditions) {
    return new LootEntry(
        List.of(conditions), entry.weight(), entry.quality(), entry.modifiers(), entry.kind());
  }

  public static LootEntry modified(LootEntry entry, ItemModifier... modifiers) {
    return new LootEntry(
        entry.conditions(), entry.weight(), entry.quality(), List.of(modifiers), entry.kind());
  }

  public static LootPool pool(int rolls, LootEntry... entries) {
    return pool(new IntProvider.Constant(rolls), entries);
  }

  public static LootPool pool(IntProvider rolls, LootEntry... entries) {
    return new LootPool(
        List.of(), rolls, new FloatProvider.Constant(0), List.of(entries), List.of());
  }

  public static LootPool poolWith(
      List<LootCondition> conditions,
      IntProvider rolls,
      float bonusRolls,
      List<ItemModifier> modifiers,
      LootEntry... entries) {
    return new LootPool(
        conditions, rolls, new FloatProvider.Constant(bonusRolls), List.of(entries), modifiers);
  }

  public static LootTable table(LootPool... pools) {
    return new LootTable(Ctx.ANY, List.of(pools), List.of(), new SequenceRef.Unnamed());
  }

  public static LootTable named(String sequence, LootPool... pools) {
    return new LootTable(
        Ctx.ANY, List.of(pools), List.of(), new SequenceRef.Named(Identifier.minecraft(sequence)));
  }

  public static LootTable withModifiers(LootTable table, ItemModifier... modifiers) {
    return new LootTable(table.type(), table.pools(), List.of(modifiers), table.randomSequence());
  }

  public static ItemModifier modifier(ModifierAction action, LootCondition... conditions) {
    return new ItemModifier(List.of(conditions), action);
  }

  public static ItemModifier setCount(int count) {
    return modifier(new ModifierAction.SetCount(new IntProvider.Constant(count), false));
  }

  public static ItemModifier addCount(int count) {
    return modifier(new ModifierAction.SetCount(new IntProvider.Constant(count), true));
  }

  public static ItemModifier uniformCount(int min, int max) {
    return modifier(
        new ModifierAction.SetCount(
            new IntProvider.Uniform(new IntProvider.Constant(min), new IntProvider.Constant(max)),
            false));
  }

  public static ItemModifier limitMax(int max) {
    return modifier(
        new ModifierAction.LimitCount(
            new IntProvider.Constant(Integer.MIN_VALUE), new IntProvider.Constant(max)));
  }

  public static ItemModifier limit(int min, int max) {
    return modifier(
        new ModifierAction.LimitCount(
            new IntProvider.Constant(min), new IntProvider.Constant(max)));
  }

  public static ItemModifier discard() {
    return modifier(new ModifierAction.Discard());
  }

  public static IntProvider.Uniform uniform(int min, int max) {
    return new IntProvider.Uniform(new IntProvider.Constant(min), new IntProvider.Constant(max));
  }

  public static List<LootCondition> conditions(LootCondition... conditions) {
    return new ArrayList<>(Arrays.asList(conditions));
  }

  public static LootCondition chance(float probability) {
    return new LootCondition.RandomChance(new FloatProvider.Constant(probability));
  }
}
