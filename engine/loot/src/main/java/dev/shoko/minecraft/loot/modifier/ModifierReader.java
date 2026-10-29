package dev.shoko.minecraft.loot.modifier;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.loot.Fields;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.number.NumberProviderCodec;
import dev.shoko.minecraft.loot.predicate.ConditionCodec;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;
import dev.shoko.minecraft.loot.predicate.match.ItemMatch;
import dev.shoko.minecraft.loot.predicate.match.MatchCodecs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;

/** Reads one item modifier, recording what is wrong with it and the types left without behavior. */
final class ModifierReader {

  private static final Set<String> DEFERRED =
      Set.of(
          "copy_custom_data",
          "copy_name",
          "fill_player_head",
          "modify_contents",
          "sequence",
          "set_attributes",
          "set_banner_pattern",
          "set_book_cover",
          "set_contents",
          "set_custom_data",
          "set_custom_model_data",
          "set_firework_explosion",
          "set_fireworks",
          "set_item",
          "set_loot_table",
          "set_lore",
          "set_random_dyes",
          "set_random_potion",
          "set_writable_book_pages",
          "set_written_book_pages",
          "toggle_tooltips");

  private static final Identifier DEFAULT_DECORATION = Identifier.minecraft("mansion");
  private static final Identifier INSTRUMENT = Identifier.minecraft("instrument");

  private final RegistryAccess access;
  private final Findings findings;
  private final Set<String> warned = new HashSet<>();
  private final ComponentsReader components;

  ModifierReader(RegistryAccess access, ItemCatalog items, Findings findings) {
    this.access = access;
    this.findings = findings;
    this.components = new ComponentsReader(items, findings);
  }

  List<ItemModifier> list(JsonNode node) {
    return ModifierCodec.readList(this, node);
  }

  ItemModifier modifier(JsonNode node) {
    Fields fields = Fields.of(node, findings);
    Identifier type = fields.requiredId("type");
    JsonNode conditionNode = fields.optional("condition");
    List<LootCondition> conditions = List.of();
    if (conditionNode != null) {
      conditions = findings.take(ConditionCodec.decodeList(conditionNode, access));
    }
    if (type == null) {
      return null;
    }
    ModifierAction action = action(type, node, fields);
    fields.finish();
    return new ItemModifier(conditions, action);
  }

  private ModifierAction action(Identifier type, JsonNode node, Fields fields) {
    String name = type.namespace().equals("minecraft") ? type.path() : "";
    if (DEFERRED.contains(name)) {
      return deferred(type, (JsonObject) node, fields);
    }
    return switch (name) {
      case "set_count" -> setCount(fields);
      case "limit_count" -> limitCount(fields);
      case "explosion_decay" -> new ModifierAction.ExplosionDecay();
      case "discard" -> new ModifierAction.Discard();
      case "furnace_smelt" -> new ModifierAction.FurnaceSmelt();
      case "apply_bonus" -> applyBonus(fields);
      case "enchanted_count_increase" -> enchantedCountIncrease(fields);
      case "copy_components" -> copyComponents(fields);
      case "copy_state" -> copyState(fields);
      case "enchant_randomly" -> enchantRandomly(fields);
      case "enchant_with_levels" -> enchantWithLevels(fields);
      case "filtered" -> filtered(fields);
      default -> other(name, type, node, fields);
    };
  }

  private ModifierAction other(String name, Identifier type, JsonNode node, Fields fields) {
    return switch (name) {
      case "exploration_map" -> explorationMap(fields);
      case "set_components" -> components.setComponents(fields);
      case "set_name" -> setName(fields);
      case "set_potion" -> new ModifierAction.SetPotion(fields.requiredId("id"));
      case "set_damage" ->
          new ModifierAction.SetDamage(
              floatProvider(fields, "damage"), fields.optionalBool("add", false));
      case "set_enchantments" -> setEnchantments(fields);
      case "set_instrument" ->
          new ModifierAction.SetInstrument(set(fields, "options", INSTRUMENT, true));
      case "set_ominous_bottle_amplifier" ->
          new ModifierAction.SetOminousBottleAmplifier(intProvider(fields, "amplifier"));
      case "set_stew_effect" -> setStewEffect(fields);
      default -> {
        findings.add(LootFindingCode.UNKNOWN_TYPE, node, "unknown modifier type " + type);
        yield null;
      }
    };
  }

  private ModifierAction deferred(Identifier type, JsonObject node, Fields fields) {
    if (warned.add(type.path())) {
      findings.add(
          LootFindingCode.NOT_IMPLEMENTED_MODIFIER, node, "modifier has no behavior yet: " + type);
    }
    SequencedMap<String, JsonNode> rest = new LinkedHashMap<>();
    for (Map.Entry<String, JsonNode> member : node.members().entrySet()) {
      fields.optional(member.getKey());
      if (!member.getKey().equals("type") && !member.getKey().equals("condition")) {
        rest.put(member.getKey(), member.getValue());
      }
    }
    return new ModifierAction.Deferred(type, new JsonObject(node.origin(), node.path(), rest));
  }

  private IntProvider intProvider(Fields fields, String key) {
    JsonNode node = fields.required(key);
    return node == null ? null : findings.take(NumberProviderCodec.decodeInt(node, access));
  }

  private FloatProvider floatProvider(Fields fields, String key) {
    JsonNode node = fields.required(key);
    return node == null ? null : findings.take(NumberProviderCodec.decodeFloat(node, access));
  }

  private IdOrTagSet set(Fields fields, String key, Identifier registry, boolean required) {
    JsonNode node = required ? fields.required(key) : fields.optional(key);
    if (node == null) {
      return required ? null : new IdOrTagSet(registry, List.of(), List.of(), true);
    }
    return findings.take(MatchCodecs.idOrTagSet(node, registry, access));
  }

  private Identifier enchantment(Fields fields, String key) {
    Identifier id = fields.requiredId(key);
    checkEnchantment(id, fields.node());
    return id;
  }

  private void checkEnchantment(Identifier id, JsonNode at) {
    if (id != null && !access.contains(EnchantmentCatalog.REGISTRY, id)) {
      findings.add(LootFindingCode.UNKNOWN_ENCHANTMENT, at, "enchantment is not loaded: " + id);
    }
  }

  private ModifierAction setCount(Fields fields) {
    return new ModifierAction.SetCount(
        intProvider(fields, "count"), fields.optionalBool("add", false));
  }

  private ModifierAction limitCount(Fields fields) {
    JsonNode limit = fields.required("limit");
    IntProvider min = new IntProvider.Constant(Integer.MIN_VALUE);
    IntProvider max = new IntProvider.Constant(Integer.MAX_VALUE);
    if (limit instanceof JsonNumber) {
      IntProvider exact = findings.take(NumberProviderCodec.decodeInt(limit, access));
      return new ModifierAction.LimitCount(exact, exact);
    }
    if (limit != null) {
      Fields bounds = Fields.of(limit, findings);
      JsonNode minNode = bounds.optional("min");
      JsonNode maxNode = bounds.optional("max");
      if (minNode != null) {
        min = findings.take(NumberProviderCodec.decodeInt(minNode, access));
      }
      if (maxNode != null) {
        max = findings.take(NumberProviderCodec.decodeInt(maxNode, access));
      }
      bounds.finish();
    }
    return new ModifierAction.LimitCount(min, max);
  }

  private ModifierAction applyBonus(Fields fields) {
    Identifier enchantment = enchantment(fields, "enchantment");
    Identifier formula = fields.requiredId("formula");
    JsonNode parameters = fields.optional("parameters");
    BonusFormula decoded = formula == null ? null : bonusFormula(formula, parameters);
    return new ModifierAction.ApplyBonus(enchantment, decoded);
  }

  private BonusFormula bonusFormula(Identifier formula, JsonNode parameters) {
    String name = formula.namespace().equals("minecraft") ? formula.path() : "";
    return switch (name) {
      case "ore_drops" -> new BonusFormula.OreDrops();
      case "uniform_bonus_count" -> {
        Fields params = parameterFields(parameters);
        int multiplier =
            params == null ? 0 : params.requiredInt("bonusMultiplier", 0, Integer.MAX_VALUE);
        if (params != null) {
          params.finish();
        }
        yield new BonusFormula.UniformBonusCount(multiplier);
      }
      case "binomial_with_bonus_count" -> {
        Fields params = parameterFields(parameters);
        int extra = params == null ? 0 : params.requiredInt("extra", 0, Integer.MAX_VALUE);
        float probability = params == null ? 0f : params.requiredFloat("probability");
        if (params != null) {
          params.finish();
        }
        yield new BonusFormula.BinomialWithBonusCount(extra, probability);
      }
      default -> {
        findings.add(LootFindingCode.UNKNOWN_TYPE, "$", "unknown formula " + formula);
        yield null;
      }
    };
  }

  private Fields parameterFields(JsonNode parameters) {
    if (parameters == null) {
      findings.add(LootFindingCode.MISSING_FIELD, "$", "missing field parameters");
      return null;
    }
    return Fields.of(parameters, findings);
  }

  private ModifierAction enchantedCountIncrease(Fields fields) {
    FloatProvider count = floatProvider(fields, "count");
    Identifier enchantment = enchantment(fields, "enchantment");
    int limit = fields.optionalInt("limit", 0, 0, Integer.MAX_VALUE);
    return new ModifierAction.EnchantedCountIncrease(count, enchantment, limit);
  }

  private ModifierAction copyComponents(Fields fields) {
    String source = fields.requiredString("source");
    if (!source.equals("block_entity") && fields.valid() && fields.has("source")) {
      findings.add(LootFindingCode.VALUE_RANGE, fields.node(), "unknown source " + source);
    }
    List<Identifier> include = identifiers(fields.optionalArray("include"));
    List<Identifier> exclude = identifiers(fields.optionalArray("exclude"));
    return new ModifierAction.CopyComponents(
        ModifierAction.ComponentSource.BLOCK_ENTITY, include, exclude);
  }

  private List<Identifier> identifiers(JsonArray array) {
    List<Identifier> ids = new ArrayList<>();
    if (array != null) {
      for (JsonNode element : array.elements()) {
        ids.add(Fields.idOf(element, findings));
      }
    }
    return ids;
  }

  private ModifierAction copyState(Fields fields) {
    Identifier block = fields.requiredId("block");
    List<String> properties = new ArrayList<>();
    JsonArray array = fields.requiredArray("properties");
    if (array != null) {
      for (JsonNode element : array.elements()) {
        properties.add(Fields.stringOf(element, findings));
      }
    }
    return new ModifierAction.CopyState(block, properties);
  }

  private ModifierAction enchantRandomly(Fields fields) {
    IdOrTagSet options = set(fields, "options", EnchantmentCatalog.REGISTRY.name(), false);
    return new ModifierAction.EnchantRandomly(
        options,
        fields.optionalBool("only_compatible", true),
        fields.optionalBool("include_additional_cost_component", false));
  }

  private ModifierAction enchantWithLevels(Fields fields) {
    IntProvider levels = intProvider(fields, "levels");
    IdOrTagSet options = set(fields, "options", EnchantmentCatalog.REGISTRY.name(), false);
    return new ModifierAction.EnchantWithLevels(
        levels, options, fields.optionalBool("include_additional_cost_component", false));
  }

  private ModifierAction filtered(Fields fields) {
    JsonNode filter = fields.required("item_filter");
    ItemMatch match = filter == null ? null : findings.take(MatchCodecs.item(filter, access));
    return new ModifierAction.Filtered(match, branch(fields, "on_pass"), branch(fields, "on_fail"));
  }

  private List<ItemModifier> branch(Fields fields, String key) {
    JsonNode node = fields.optional(key);
    return node == null ? List.of() : list(node);
  }

  private ModifierAction explorationMap(Fields fields) {
    String destination = fields.requiredString("destination");
    Identifier tag =
        Identifier.tryParse(destination.startsWith("#") ? destination.substring(1) : destination)
            .orElse(null);
    if (tag == null && fields.valid() && fields.has("destination")) {
      findings.add(LootFindingCode.INVALID_IDENTIFIER, fields.node(), "invalid destination");
    }
    return new ModifierAction.ExplorationMap(
        tag,
        decoration(fields),
        fields.optionalInt("zoom", 2, 0, Integer.MAX_VALUE),
        fields.optionalInt("search_radius", 50, 0, Integer.MAX_VALUE),
        fields.optionalBool("skip_existing_chunks", true));
  }

  private Identifier decoration(Fields fields) {
    Identifier id = fields.optionalId("decoration");
    return id == null && !fields.has("decoration") ? DEFAULT_DECORATION : id;
  }

  private ModifierAction setName(Fields fields) {
    JsonNode name = fields.required("name");
    String target = fields.optionalString("target");
    ModifierAction.NameTarget kind = ModifierAction.NameTarget.CUSTOM_NAME;
    if ("item_name".equals(target)) {
      kind = ModifierAction.NameTarget.ITEM_NAME;
    } else if (target != null && !target.equals("custom_name")) {
      findings.add(LootFindingCode.VALUE_RANGE, fields.node(), "unknown name target " + target);
    }
    return new ModifierAction.SetName(name, kind);
  }

  private ModifierAction setEnchantments(Fields fields) {
    SequencedMap<Identifier, IntProvider> levels = new LinkedHashMap<>();
    JsonNode tableNode = fields.required("enchantments");
    Fields table = tableNode == null ? null : Fields.of(tableNode, findings);
    if (table != null && table.valid()) {
      for (Map.Entry<String, JsonNode> entry : table.node().members().entrySet()) {
        table.optional(entry.getKey());
        Identifier id = Identifier.tryParse(entry.getKey()).orElse(null);
        if (id == null) {
          findings.add(LootFindingCode.INVALID_IDENTIFIER, entry.getValue(), "invalid identifier");
          continue;
        }
        checkEnchantment(id, entry.getValue());
        levels.put(id, findings.take(NumberProviderCodec.decodeInt(entry.getValue(), access)));
      }
    }
    return new ModifierAction.SetEnchantments(levels, fields.optionalBool("add", false));
  }

  private ModifierAction setStewEffect(Fields fields) {
    List<ModifierAction.SetStewEffect.StewEffect> effects = new ArrayList<>();
    JsonArray array = fields.requiredArray("effects");
    if (array != null) {
      for (JsonNode element : array.elements()) {
        Fields effect = Fields.of(element, findings);
        Identifier id = effect.requiredId("type");
        IntProvider duration = intProvider(effect, "duration");
        effect.finish();
        effects.add(new ModifierAction.SetStewEffect.StewEffect(id, duration));
      }
    }
    return new ModifierAction.SetStewEffect(effects);
  }
}
