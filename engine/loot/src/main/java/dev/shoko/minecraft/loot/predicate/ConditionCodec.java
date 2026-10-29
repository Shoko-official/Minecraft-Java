package dev.shoko.minecraft.loot.predicate;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.Fields;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.enchant.effect.EffectCodec;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.number.NumberProviderCodec;
import dev.shoko.minecraft.loot.predicate.match.DamageSourcePredicate;
import dev.shoko.minecraft.loot.predicate.match.EntityPredicate;
import dev.shoko.minecraft.loot.predicate.match.ItemMatch;
import dev.shoko.minecraft.loot.predicate.match.LocationPredicate;
import dev.shoko.minecraft.loot.predicate.match.MatchCodecs;
import dev.shoko.minecraft.loot.predicate.match.NumberRange;
import dev.shoko.minecraft.loot.predicate.match.Tristate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Decodes conditions from their JSON form (LT-R3, LT-R4a). The discriminant is {@code type}; a
 * string is a reference to a named predicate; {@code terms} accepts objects and references. The
 * forms of earlier versions ({@code condition} as discriminant) are not decoded (LT-R3).
 */
public final class ConditionCodec {

  private ConditionCodec() {}

  /**
   * Decodes one condition.
   *
   * @param node an object or a string
   * @param access the loaded registries and tags
   * @return the condition; {@code UNKNOWN_TYPE} for a type outside the 20, {@code MISSING_FIELD} or
   *     {@code TYPE_MISMATCH} for a wrong shape. A reference is not checked here
   */
  public static LootDecoding<LootCondition> decode(JsonNode node, RegistryAccess access) {
    Findings findings = new Findings();
    LootCondition condition = read(node, access, findings);
    return findings.result(condition);
  }

  /**
   * Decodes the value of a {@code condition} key. A single condition is a list of one; a list is
   * kept in order and means {@code all_of} (LT-R4b, provisional).
   *
   * @param node an object, a string or an array
   * @param access the loaded registries and tags
   * @return the conditions in file order
   */
  public static LootDecoding<List<LootCondition>> decodeList(JsonNode node, RegistryAccess access) {
    Findings findings = new Findings();
    List<LootCondition> conditions = new ArrayList<>();
    if (node instanceof JsonArray array) {
      for (JsonNode element : array.elements()) {
        conditions.add(read(element, access, findings));
      }
    } else {
      conditions.add(read(node, access, findings));
    }
    return findings.failed() ? findings.failure() : findings.result(List.copyOf(conditions));
  }

  /**
   * Decodes the {@code predicate} registry.
   *
   * @param elements the elements found by the loader
   * @param access the loaded registries and tags
   * @return the named predicates; one that fails is absent and listed in the findings, in
   *     identifier order
   */
  public static LootDecoding<NamedPredicates> decodeNamed(
      ElementRegistryData elements, RegistryAccess access) {
    Findings findings = new Findings();
    SequencedMap<Identifier, LootCondition> predicates = new LinkedHashMap<>();
    for (Map.Entry<Identifier, JsonNode> element : elements.elements().entrySet()) {
      LootDecoding<LootCondition> decoded = decode(element.getValue(), access);
      for (var finding : decoded.findings()) {
        findings.add(finding.code(), finding.path(), element.getKey() + ": " + finding.message());
      }
      if (decoded instanceof LootDecoding.Success<LootCondition> success) {
        predicates.put(element.getKey(), success.value());
      }
    }
    return findings.result(new NamedPredicates(predicates));
  }

  /** Reads a condition, recording what is wrong with it. */
  static LootCondition read(JsonNode node, RegistryAccess access, Findings findings) {
    if (node instanceof JsonString string) {
      Identifier id = Fields.idOf(string, findings);
      return id == null ? null : new LootCondition.Reference(id);
    }
    if (!(node instanceof JsonObject object)) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected an object or a string");
      return null;
    }
    Fields fields = Fields.of(object, findings);
    Identifier type = fields.requiredId("type");
    if (type == null) {
      return null;
    }
    LootCondition condition = typed(type, object, fields, access, findings);
    fields.finish();
    return condition;
  }

  private static LootCondition typed(
      Identifier type, JsonObject object, Fields fields, RegistryAccess access, Findings findings) {
    if (!type.namespace().equals("minecraft")) {
      findings.add(LootFindingCode.UNKNOWN_TYPE, object, "unknown condition type " + type);
      return null;
    }
    return switch (type.path()) {
      case "all_of" -> new LootCondition.AllOf(terms(fields, access, findings));
      case "any_of" -> new LootCondition.AnyOf(terms(fields, access, findings));
      case "inverted" -> inverted(fields, access, findings);
      case "killed_by_player" -> new LootCondition.KilledByPlayer();
      case "survives_explosion" -> new LootCondition.SurvivesExplosion();
      case "random_chance" -> randomChance(fields, access, findings);
      case "random_chance_with_enchanted_bonus" -> enchantedBonus(fields, findings);
      case "table_bonus" -> tableBonus(fields, findings);
      case "match_block" -> matchBlock(object, fields, access, findings);
      case "match_tool" -> matchTool(fields, access, findings);
      case "entity_properties" -> entityProperties(fields, access, findings);
      case "damage_source_properties" -> damageSource(fields, access, findings);
      case "location_check" -> locationCheck(fields, access, findings);
      default -> other(type.path(), object, fields, access, findings);
    };
  }

  private static LootCondition other(
      String type, JsonObject object, Fields fields, RegistryAccess access, Findings findings) {
    return switch (type) {
      case "weather_check" ->
          new LootCondition.WeatherCheck(
              tristate(fields, "raining", findings), tristate(fields, "thundering", findings));
      case "time_check" -> timeCheck(fields, findings);
      case "enchantment_active_check" -> {
        JsonNode active = fields.required("active");
        yield new LootCondition.EnchantmentActiveCheck(
            active != null && Fields.boolOf(active, findings));
      }
      case "int_value_check" -> intValueCheck(fields, access, findings);
      case "float_value_check" -> floatValueCheck(fields, access, findings);
      case "entity_scores" -> entityScores(fields, findings);
      case "environment_attribute_check" -> {
        // The shape is unknown: every member is accepted and kept as read.
        for (String key : object.members().keySet()) {
          fields.optional(key);
        }
        yield new LootCondition.EnvironmentAttributeCheck(withoutType(object));
      }
      default -> {
        findings.add(LootFindingCode.UNKNOWN_TYPE, object, "unknown condition type " + type);
        yield null;
      }
    };
  }

  private static JsonObject withoutType(JsonObject object) {
    SequencedMap<String, JsonNode> members = new LinkedHashMap<>(object.members());
    members.remove("type");
    return new JsonObject(object.origin(), object.path(), members);
  }

  private static List<LootCondition> terms(
      Fields fields, RegistryAccess access, Findings findings) {
    List<LootCondition> terms = new ArrayList<>();
    JsonArray array = fields.requiredArray("terms");
    if (array != null) {
      for (JsonNode element : array.elements()) {
        terms.add(term(element, access, findings));
      }
    }
    return terms;
  }

  private static LootCondition term(JsonNode element, RegistryAccess access, Findings findings) {
    if (element instanceof JsonArray list) {
      List<LootCondition> nested = new ArrayList<>();
      for (JsonNode inner : list.elements()) {
        nested.add(term(inner, access, findings));
      }
      return new LootCondition.AllOf(nested);
    }
    return read(element, access, findings);
  }

  private static LootCondition inverted(Fields fields, RegistryAccess access, Findings findings) {
    JsonNode term = fields.required("term");
    return new LootCondition.Inverted(term == null ? null : read(term, access, findings));
  }

  private static LootCondition randomChance(
      Fields fields, RegistryAccess access, Findings findings) {
    JsonNode chance = fields.required("chance");
    FloatProvider provider =
        chance == null ? null : findings.take(NumberProviderCodec.decodeFloat(chance, access));
    return new LootCondition.RandomChance(provider);
  }

  private static LootCondition enchantedBonus(Fields fields, Findings findings) {
    Identifier enchantment = fields.requiredId("enchantment");
    float unenchanted = fields.requiredFloat("unenchanted_chance");
    JsonNode enchantedNode = fields.required("enchanted_chance");
    LevelBasedValue enchanted =
        enchantedNode == null ? null : findings.take(EffectCodec.levelBased(enchantedNode));
    return new LootCondition.RandomChanceWithEnchantedBonus(enchantment, unenchanted, enchanted);
  }

  private static LootCondition tableBonus(Fields fields, Findings findings) {
    Identifier enchantment = fields.requiredId("enchantment");
    List<Float> chances = new ArrayList<>();
    JsonArray array = fields.requiredArray("chances");
    if (array != null) {
      for (JsonNode element : array.elements()) {
        chances.add(Fields.floatOf(element, findings));
      }
    }
    return new LootCondition.TableBonus(enchantment, List.copyOf(chances));
  }

  private static LootCondition matchBlock(
      JsonObject object, Fields fields, RegistryAccess access, Findings findings) {
    // The members of the condition are the members of the block predicate.
    object.members().keySet().forEach(fields::optional);
    return new LootCondition.MatchBlock(
        findings.take(MatchCodecs.block(withoutType(object), access)));
  }

  private static LootCondition matchTool(Fields fields, RegistryAccess access, Findings findings) {
    JsonNode predicate = fields.required("predicate");
    ItemMatch match = predicate == null ? null : findings.take(MatchCodecs.item(predicate, access));
    return new LootCondition.MatchTool(match);
  }

  private static LootCondition entityProperties(
      Fields fields, RegistryAccess access, Findings findings) {
    EntityTarget target = target(fields, findings);
    JsonNode predicate = fields.optional("predicate");
    EntityPredicate decoded =
        predicate == null
            ? new EntityPredicate(List.of())
            : findings.take(MatchCodecs.entity(predicate, access));
    return new LootCondition.EntityProperties(target, decoded);
  }

  private static EntityTarget target(Fields fields, Findings findings) {
    JsonNode node = fields.required("entity");
    if (node == null) {
      return null;
    }
    String name = Fields.stringOf(node, findings);
    for (EntityTarget target : EntityTarget.values()) {
      if (target.key().equals(name)) {
        return target;
      }
    }
    if (node instanceof JsonString) {
      findings.add(LootFindingCode.VALUE_RANGE, node, "unknown entity target " + name);
    }
    return null;
  }

  private static LootCondition damageSource(
      Fields fields, RegistryAccess access, Findings findings) {
    JsonNode predicate = fields.required("predicate");
    DamageSourcePredicate decoded =
        predicate == null ? null : findings.take(MatchCodecs.damageSource(predicate, access));
    return new LootCondition.DamageSourceProperties(decoded);
  }

  private static LootCondition locationCheck(
      Fields fields, RegistryAccess access, Findings findings) {
    JsonNode predicate = fields.required("predicate");
    LocationPredicate decoded =
        predicate == null ? null : findings.take(MatchCodecs.location(predicate, access));
    int x = fields.optionalInt("offsetX", 0, -Integer.MAX_VALUE, Integer.MAX_VALUE);
    int y = fields.optionalInt("offsetY", 0, -Integer.MAX_VALUE, Integer.MAX_VALUE);
    int z = fields.optionalInt("offsetZ", 0, -Integer.MAX_VALUE, Integer.MAX_VALUE);
    return new LootCondition.LocationCheck(decoded, x, y, z);
  }

  private static Tristate tristate(Fields fields, String key, Findings findings) {
    JsonNode node = fields.optional(key);
    if (node == null) {
      return Tristate.UNSET;
    }
    return Fields.boolOf(node, findings) ? Tristate.TRUE : Tristate.FALSE;
  }

  private static LootCondition timeCheck(Fields fields, Findings findings) {
    Identifier clock = fields.optionalId("clock");
    JsonNode value = fields.required("value");
    NumberRange range = value == null ? null : findings.take(MatchCodecs.range(value));
    int period = fields.optionalInt("period", 0, 0, Integer.MAX_VALUE);
    return new LootCondition.TimeCheck(clock, range, period);
  }

  private static LootCondition intValueCheck(
      Fields fields, RegistryAccess access, Findings findings) {
    JsonNode value = fields.required("value");
    IntProvider provider =
        value == null ? null : findings.take(NumberProviderCodec.decodeInt(value, access));
    JsonNode test = fields.required("test");
    NumberRange range = test == null ? null : findings.take(MatchCodecs.range(test));
    return new LootCondition.IntValueCheck(provider, range);
  }

  private static LootCondition floatValueCheck(
      Fields fields, RegistryAccess access, Findings findings) {
    JsonNode value = fields.required("value");
    FloatProvider provider =
        value == null ? null : findings.take(NumberProviderCodec.decodeFloat(value, access));
    JsonNode test = fields.required("test");
    NumberRange range = test == null ? null : findings.take(MatchCodecs.range(test));
    return new LootCondition.FloatValueCheck(provider, range);
  }

  private static LootCondition entityScores(Fields fields, Findings findings) {
    EntityTarget target = target(fields, findings);
    JsonNode scores = fields.required("scores");
    return new LootCondition.EntityScores(target, scores);
  }
}
