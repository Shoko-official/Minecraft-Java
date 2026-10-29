package dev.shoko.minecraft.loot.enchant;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.Fields;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.enchant.effect.EffectCodec;
import dev.shoko.minecraft.loot.enchant.effect.EffectComponents;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import dev.shoko.minecraft.loot.predicate.match.MatchCodecs;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Decodes the enchantment registries (EN-R1, EN-R2, EN-R34). Findings carry the JSON path inside
 * the file.
 */
public final class EnchantmentCodec {

  private static final Identifier ITEM = Identifier.minecraft("item");

  private EnchantmentCodec() {}

  /**
   * Decodes one definition.
   *
   * @param id the identifier of the file
   * @param node the file content
   * @param access the loaded registries and tags
   * @return the definition; {@code MISSING_FIELD} for a required field of EN-R2 that is absent,
   *     {@code VALUE_RANGE} for a non-positive {@code max_level}, {@code UNKNOWN_TYPE} for an
   *     effect component outside the 31
   */
  public static LootDecoding<EnchantmentDefinition> decode(
      Identifier id, JsonNode node, RegistryAccess access) {
    Findings findings = new Findings();
    Fields fields = Fields.of(node, findings);
    JsonNode description = fields.required("description");
    int maxLevel = fields.requiredInt("max_level", 1, 255);
    int weight = fields.requiredInt("weight", 1, Integer.MAX_VALUE);
    int anvilCost = fields.requiredInt("anvil_cost", 0, Integer.MAX_VALUE);
    LevelBasedValue.Linear minCost = cost(fields, "min_cost", findings);
    LevelBasedValue.Linear maxCost = cost(fields, "max_cost", findings);
    List<SlotGroup> slots = slots(fields, findings);
    IdOrTagSet supported = items(fields, "supported_items", true, access, findings);
    IdOrTagSet primary = items(fields, "primary_items", false, access, findings);
    IdOrTagSet exclusive = exclusive(fields, access, findings);
    EffectComponents effects = effects(fields, access, findings);
    fields.finish();
    if (findings.failed()) {
      return findings.failure();
    }
    boolean hasPrimary = primary != null;
    EnchantmentItems items =
        new EnchantmentItems(supported, hasPrimary ? primary : supported, hasPrimary);
    EnchantmentCosts costs = new EnchantmentCosts(weight, anvilCost, minCost, maxCost);
    return findings.result(
        new EnchantmentDefinition(
            id, description, maxLevel, costs, slots, items, exclusive, effects));
  }

  /**
   * Decodes one enchantment provider.
   *
   * @param node the file content
   * @param access the loaded registries and tags
   * @return the provider; {@code UNKNOWN_TYPE} for a type outside {@code single}, {@code by_cost}
   *     and {@code by_cost_with_difficulty}
   */
  public static LootDecoding<EnchantmentProvider> decodeProvider(
      JsonNode node, RegistryAccess access) {
    Findings findings = new Findings();
    Fields fields = Fields.of(node, findings);
    Identifier type = fields.requiredId("type");
    EnchantmentProvider provider = null;
    if (type != null) {
      provider = typedProvider(type, node, fields, access, findings);
      fields.finish();
    }
    return findings.result(provider);
  }

  /**
   * Decodes both registries.
   *
   * @param enchantments the elements of the enchantment registry
   * @param providers the {@code enchantment_provider} elements
   * @param access the loaded registries and tags
   * @return the catalog; an element that fails is absent and listed in the findings, in identifier
   *     order
   */
  public static LootDecoding<EnchantmentCatalog> decodeCatalog(
      ElementRegistryData enchantments, ElementRegistryData providers, RegistryAccess access) {
    Findings findings = new Findings();
    SequencedMap<Identifier, EnchantmentDefinition> definitions = new LinkedHashMap<>();
    for (Map.Entry<Identifier, JsonNode> element : enchantments.elements().entrySet()) {
      LootDecoding<EnchantmentDefinition> decoded =
          decode(element.getKey(), element.getValue(), access);
      relay(findings, element.getKey(), decoded);
      if (decoded instanceof LootDecoding.Success<EnchantmentDefinition> success) {
        definitions.put(element.getKey(), success.value());
      }
    }
    SequencedMap<Identifier, EnchantmentProvider> byId = new LinkedHashMap<>();
    for (Map.Entry<Identifier, JsonNode> element : providers.elements().entrySet()) {
      LootDecoding<EnchantmentProvider> decoded = decodeProvider(element.getValue(), access);
      relay(findings, element.getKey(), decoded);
      if (decoded instanceof LootDecoding.Success<EnchantmentProvider> success) {
        byId.put(element.getKey(), success.value());
      }
    }
    return findings.result(new EnchantmentCatalog(definitions, byId));
  }

  private static void relay(Findings findings, Identifier element, LootDecoding<?> decoded) {
    for (var finding : decoded.findings()) {
      findings.add(finding.code(), finding.path(), element + ": " + finding.message());
    }
  }

  private static EnchantmentProvider typedProvider(
      Identifier type, JsonNode node, Fields fields, RegistryAccess access, Findings findings) {
    String name = type.namespace().equals("minecraft") ? type.path() : "";
    return switch (name) {
      case "single" -> {
        Identifier enchantment = fields.requiredId("enchantment");
        yield new EnchantmentProvider.Single(enchantment, fields.requiredInt("level", 1, 255));
      }
      case "by_cost", "by_cost_with_difficulty" -> {
        JsonNode tag = fields.required("enchantments");
        IdOrTagSet set =
            tag == null
                ? null
                : findings.take(
                    MatchCodecs.idOrTagSet(tag, EnchantmentCatalog.REGISTRY.name(), access));
        int minCost = fields.requiredInt("min_cost", 0, Integer.MAX_VALUE);
        int span = fields.requiredInt("max_cost_span", 0, Integer.MAX_VALUE);
        yield name.equals("by_cost")
            ? new EnchantmentProvider.ByCost(set, minCost, span)
            : new EnchantmentProvider.ByCostWithDifficulty(set, minCost, span);
      }
      default -> {
        findings.add(LootFindingCode.UNKNOWN_TYPE, node, "unknown provider type " + type);
        yield null;
      }
    };
  }

  private static LevelBasedValue.Linear cost(Fields fields, String key, Findings findings) {
    JsonNode node = fields.required(key);
    if (node == null) {
      return null;
    }
    Fields cost = Fields.of(node, findings);
    float base = cost.requiredFloat("base");
    float perLevel = cost.requiredFloat("per_level_above_first");
    cost.finish();
    return new LevelBasedValue.Linear(base, perLevel);
  }

  private static List<SlotGroup> slots(Fields fields, Findings findings) {
    List<SlotGroup> slots = new ArrayList<>();
    JsonArray array = fields.requiredArray("slots");
    if (array != null) {
      if (array.elements().isEmpty()) {
        findings.add(LootFindingCode.VALUE_RANGE, array, "an enchantment needs a slot group");
      }
      for (JsonNode element : array.elements()) {
        slots.add(slot(element, findings));
      }
    }
    return slots;
  }

  private static SlotGroup slot(JsonNode node, Findings findings) {
    String name = node instanceof JsonString ? Fields.stringOf(node, findings) : null;
    if (name == null) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected a slot group name");
      return null;
    }
    for (SlotGroup group : SlotGroup.values()) {
      if (group.name().toLowerCase(Locale.ROOT).equals(name)) {
        return group;
      }
    }
    findings.add(LootFindingCode.VALUE_RANGE, node, "unknown slot group " + name);
    return null;
  }

  private static IdOrTagSet items(
      Fields fields, String key, boolean required, RegistryAccess access, Findings findings) {
    JsonNode node = required ? fields.required(key) : fields.optional(key);
    return node == null ? null : findings.take(MatchCodecs.idOrTagSet(node, ITEM, access));
  }

  private static IdOrTagSet exclusive(Fields fields, RegistryAccess access, Findings findings) {
    JsonNode node = fields.optional("exclusive_set");
    if (node == null) {
      return new IdOrTagSet(EnchantmentCatalog.REGISTRY.name(), List.of(), List.of(), false);
    }
    return findings.take(MatchCodecs.idOrTagSet(node, EnchantmentCatalog.REGISTRY.name(), access));
  }

  private static EffectComponents effects(Fields fields, RegistryAccess access, Findings findings) {
    JsonNode node = fields.optional("effects");
    if (node == null) {
      return new EffectComponents(new LinkedHashMap<>());
    }
    return findings.take(EffectCodec.components(node, access));
  }
}
