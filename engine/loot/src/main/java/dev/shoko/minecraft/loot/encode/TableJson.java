package dev.shoko.minecraft.loot.encode;

import dev.shoko.minecraft.loot.context.LootContextTypes;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.random.SequenceRef;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootPool;
import dev.shoko.minecraft.loot.table.LootTable;
import java.util.ArrayList;
import java.util.List;
import java.util.SequencedMap;

/**
 * Encodes a decoded table back to JSON, for the round trip of plan L1. Keys come in a fixed order;
 * a field at its default is not written, as the 1447 vanilla tables do not write them.
 */
public final class TableJson {

  private TableJson() {}

  /**
   * Encodes a table.
   *
   * @param table a decoded table
   * @return compact JSON
   */
  public static String encode(LootTable table) {
    return JsonOut.write(table(table));
  }

  private static Object table(LootTable table) {
    SequencedMap<String, Object> object = JsonOut.object();
    if (!table.type().id().equals(LootContextTypes.generic().id())) {
      object.put("type", table.type().id().toString());
    }
    if (table.randomSequence() instanceof SequenceRef.Named named) {
      object.put("random_sequence", named.id().toString());
    }
    if (!table.pools().isEmpty()) {
      List<Object> pools = new ArrayList<>();
      table.pools().forEach(pool -> pools.add(pool(pool)));
      object.put("pools", pools);
    }
    if (!table.modifiers().isEmpty()) {
      object.put("modifier", ModifierJson.modifiers(table.modifiers()));
    }
    return object;
  }

  private static Object pool(LootPool pool) {
    SequencedMap<String, Object> object = JsonOut.object();
    object.put("rolls", ProviderJson.intProvider(pool.rolls()));
    if (!(pool.bonusRolls() instanceof FloatProvider.Constant constant && constant.value() == 0f)) {
      object.put("bonus_rolls", ProviderJson.floatProvider(pool.bonusRolls()));
    }
    if (!pool.conditions().isEmpty()) {
      object.put("condition", ConditionJson.conditions(pool.conditions()));
    }
    List<Object> entries = new ArrayList<>();
    pool.entries().forEach(entry -> entries.add(entry(entry)));
    object.put("entries", entries);
    if (!pool.modifiers().isEmpty()) {
      object.put("modifier", ModifierJson.modifiers(pool.modifiers()));
    }
    return object;
  }

  private static Object entry(LootEntry entry) {
    SequencedMap<String, Object> object = JsonOut.object();
    object.put("type", "minecraft:" + typeName(entry.kind()));
    kind(entry.kind(), object);
    if (entry.weight() != 1) {
      object.put("weight", JsonOut.number(entry.weight()));
    }
    if (entry.quality() != 0) {
      object.put("quality", JsonOut.number(entry.quality()));
    }
    if (!entry.conditions().isEmpty()) {
      object.put("condition", ConditionJson.conditions(entry.conditions()));
    }
    if (!entry.modifiers().isEmpty()) {
      object.put("modifier", ModifierJson.modifiers(entry.modifiers()));
    }
    return object;
  }

  private static String typeName(LootEntry.Kind kind) {
    return switch (kind) {
      case LootEntry.Kind.Empty empty -> "empty";
      case LootEntry.Kind.Item item -> "item";
      case LootEntry.Kind.LootTableRef ref -> "loot_table";
      case LootEntry.Kind.Dynamic dynamic -> "dynamic";
      case LootEntry.Kind.Tag tag -> "tag";
      case LootEntry.Kind.Slots slots -> "slots";
      case LootEntry.Kind.Alternatives alternatives -> "alternatives";
      case LootEntry.Kind.Sequence sequence -> "sequence";
      case LootEntry.Kind.Group group -> "group";
    };
  }

  private static void kind(LootEntry.Kind kind, SequencedMap<String, Object> out) {
    switch (kind) {
      case LootEntry.Kind.Empty empty -> {}
      case LootEntry.Kind.Item item -> out.put("name", item.item().id().toString());
      case LootEntry.Kind.LootTableRef ref ->
          out.put(
              "value",
              switch (ref.table()) {
                case LootEntry.TableRef.Named named -> named.id().toString();
                case LootEntry.TableRef.Inline inline -> table(inline.table());
              });
      case LootEntry.Kind.Dynamic dynamic -> out.put("name", dynamic.name().toString());
      case LootEntry.Kind.Tag tag -> {
        out.put("items", "#" + tag.tag());
        out.put("expand", tag.expand());
      }
      case LootEntry.Kind.Slots slots -> out.put("slot_source", JsonOut.tree(slots.slotSource()));
      case LootEntry.Kind.Alternatives alternatives ->
          out.put("children", children(alternatives.children()));
      case LootEntry.Kind.Sequence sequence -> out.put("children", children(sequence.children()));
      case LootEntry.Kind.Group group -> out.put("children", children(group.children()));
    }
  }

  private static List<Object> children(List<LootEntry> children) {
    List<Object> list = new ArrayList<>();
    children.forEach(child -> list.add(entry(child)));
    return list;
  }
}
