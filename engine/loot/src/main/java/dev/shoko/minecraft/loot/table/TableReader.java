package dev.shoko.minecraft.loot.table;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.loot.Fields;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.LootContextType;
import dev.shoko.minecraft.loot.context.LootContextTypes;
import dev.shoko.minecraft.loot.modifier.ItemModifier;
import dev.shoko.minecraft.loot.modifier.ModifierCodec;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.number.NumberProviderCodec;
import dev.shoko.minecraft.loot.predicate.ConditionCodec;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.random.SequenceRef;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a table, its pools and its entries, recording what is wrong with them (LT-R1 to LT-R10).
 */
final class TableReader {

  private static final Identifier ITEM = Identifier.minecraft("item");

  /** Bound on the rolls of a pool (LT-R74a). */
  static final int MAX_ROLLS = 4096;

  /** Bound on the weight of an entry (LT-R74c). */
  static final int MAX_WEIGHT = 1 << 20;

  private final RegistryAccess access;
  private final ItemCatalog items;
  private final Findings findings;

  TableReader(RegistryAccess access, ItemCatalog items, Findings findings) {
    this.access = access;
    this.items = items;
    this.findings = findings;
  }

  LootTable table(JsonNode node) {
    Fields fields = Fields.of(node, findings);
    LootContextType type = type(fields);
    SequenceRef sequence = sequence(fields);
    List<LootPool> pools = pools(fields);
    List<ItemModifier> modifiers = modifiers(fields);
    fields.finish();
    return new LootTable(type, pools, modifiers, sequence);
  }

  private LootContextType type(Fields fields) {
    JsonNode node = fields.optional("type");
    if (node == null) {
      return LootContextTypes.generic();
    }
    Identifier id = Fields.idOf(node, findings);
    if (id == null) {
      return null;
    }
    LootContextType type = LootContextTypes.find(id).orElse(null);
    if (type == null) {
      findings.add(LootFindingCode.UNKNOWN_TYPE, node, "unknown table type " + id);
    }
    return type;
  }

  private SequenceRef sequence(Fields fields) {
    Identifier id = fields.optionalId("random_sequence");
    return id == null ? new SequenceRef.Unnamed() : new SequenceRef.Named(id);
  }

  private List<LootPool> pools(Fields fields) {
    List<LootPool> pools = new ArrayList<>();
    JsonArray array = fields.optionalArray("pools");
    if (array != null) {
      for (JsonNode element : array.elements()) {
        pools.add(pool(element));
      }
    }
    return pools;
  }

  private List<ItemModifier> modifiers(Fields fields) {
    JsonNode node = fields.optional("modifier");
    if (node == null) {
      return List.of();
    }
    List<ItemModifier> modifiers = findings.take(ModifierCodec.decodeList(node, access, items));
    return modifiers == null ? List.of() : modifiers;
  }

  private List<LootCondition> conditions(Fields fields) {
    JsonNode node = fields.optional("condition");
    if (node == null) {
      return List.of();
    }
    List<LootCondition> conditions = findings.take(ConditionCodec.decodeList(node, access));
    return conditions == null ? List.of() : conditions;
  }

  private LootPool pool(JsonNode node) {
    Fields fields = Fields.of(node, findings);
    JsonNode rollsNode = fields.required("rolls");
    IntProvider rolls =
        rollsNode == null ? null : findings.take(NumberProviderCodec.decodeInt(rollsNode, access));
    if (rolls instanceof IntProvider.Constant constant && constant.value() > MAX_ROLLS) {
      findings.add(LootFindingCode.VALUE_RANGE, rollsNode, "rolls above the cap of " + MAX_ROLLS);
    }
    JsonNode bonusNode = fields.optional("bonus_rolls");
    FloatProvider bonus =
        bonusNode == null
            ? new FloatProvider.Constant(0f)
            : findings.take(NumberProviderCodec.decodeFloat(bonusNode, access));
    List<LootCondition> conditions = conditions(fields);
    List<LootEntry> entries = entries(fields.requiredArray("entries"));
    List<ItemModifier> modifiers = modifiers(fields);
    fields.finish();
    return new LootPool(conditions, rolls, bonus, entries, modifiers);
  }

  private List<LootEntry> entries(JsonArray array) {
    List<LootEntry> entries = new ArrayList<>();
    if (array != null) {
      for (JsonNode element : array.elements()) {
        entries.add(entry(element));
      }
    }
    return entries;
  }

  private LootEntry entry(JsonNode node) {
    Fields fields = Fields.of(node, findings);
    Identifier type = fields.requiredId("type");
    if (type == null) {
      return null;
    }
    LootEntry.Kind kind = kind(type, node, fields);
    List<LootCondition> conditions = conditions(fields);
    int weight = fields.optionalInt("weight", 1, 0, Integer.MAX_VALUE);
    if (weight > MAX_WEIGHT) {
      findings.add(LootFindingCode.WEIGHT_CAPPED, node, "weight above the cap of 2^20");
    }
    int quality = fields.optionalInt("quality", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
    List<ItemModifier> modifiers = modifiers(fields);
    fields.finish();
    return new LootEntry(conditions, weight, quality, modifiers, kind);
  }

  private LootEntry.Kind kind(Identifier type, JsonNode node, Fields fields) {
    String name = type.namespace().equals("minecraft") ? type.path() : "";
    return switch (name) {
      case "empty" -> new LootEntry.Kind.Empty();
      case "item" -> item(fields);
      case "loot_table" -> lootTable(fields);
      case "dynamic" -> new LootEntry.Kind.Dynamic(fields.requiredId("name"));
      case "tag" -> tag(fields);
      case "slots" -> new LootEntry.Kind.Slots(fields.required("slot_source"));
      case "alternatives" ->
          new LootEntry.Kind.Alternatives(entries(fields.requiredArray("children")));
      case "sequence" -> new LootEntry.Kind.Sequence(entries(fields.requiredArray("children")));
      case "group" -> new LootEntry.Kind.Group(entries(fields.requiredArray("children")));
      default -> {
        findings.add(LootFindingCode.UNKNOWN_TYPE, node, "unknown entry type " + type);
        yield null;
      }
    };
  }

  private LootEntry.Kind item(Fields fields) {
    JsonNode node = fields.required("name");
    Identifier id = node == null ? null : Fields.idOf(node, findings);
    if (id == null) {
      return null;
    }
    ItemType item = items.find(id).orElse(null);
    if (item == null) {
      findings.add(LootFindingCode.INVALID_IDENTIFIER, node, "unknown item " + id);
    }
    return new LootEntry.Kind.Item(item);
  }

  private LootEntry.Kind lootTable(Fields fields) {
    JsonNode value = fields.required("value");
    if (value == null) {
      return null;
    }
    if (value instanceof JsonString) {
      return new LootEntry.Kind.LootTableRef(
          new LootEntry.TableRef.Named(Fields.idOf(value, findings)));
    }
    if (value instanceof JsonObject) {
      return new LootEntry.Kind.LootTableRef(new LootEntry.TableRef.Inline(table(value)));
    }
    findings.add(LootFindingCode.TYPE_MISMATCH, value, "expected a table identifier or a table");
    return null;
  }

  private LootEntry.Kind tag(Fields fields) {
    JsonNode node = fields.required("items");
    boolean expand = fields.optionalBool("expand", false);
    String text = node == null ? "" : Fields.stringOf(node, findings);
    if (node == null || !text.startsWith("#")) {
      if (node != null && node instanceof JsonString) {
        findings.add(LootFindingCode.INVALID_IDENTIFIER, node, "expected a #tag");
      }
      return null;
    }
    Identifier tag = Identifier.tryParse(text.substring(1)).orElse(null);
    if (tag == null) {
      findings.add(LootFindingCode.INVALID_IDENTIFIER, node, "invalid identifier");
      return null;
    }
    if (access.tagMembers(new TagKey<Object>(new RegistryKey<>(ITEM), tag)).isEmpty()) {
      findings.add(LootFindingCode.UNRESOLVED_TAG, node, "tag is not loaded: " + tag);
    }
    return new LootEntry.Kind.Tag(tag, expand);
  }
}
