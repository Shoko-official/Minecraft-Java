package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.Fields;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Decodes the effect data of enchantments from JSON (EN-R11 to EN-R20, EN-R28). The 31 effect
 * components, 6 value effects and 6 level-based types are the registered ones; any other {@code
 * type} or component is {@code UNKNOWN_TYPE}.
 */
public final class EffectCodec {

  private EffectCodec() {}

  /**
   * Decodes a level-based value: a bare number is a constant, an object has a {@code type}. The
   * cost shape {@code {base, per_level_above_first}} without type is read as {@code linear} by
   * {@code EnchantmentCodec}, not here.
   *
   * @param node the JSON value
   * @return the value
   */
  public static LootDecoding<LevelBasedValue> levelBased(JsonNode node) {
    Findings findings = new Findings();
    LevelBasedValue value = readLevelBased(node, findings);
    return findings.result(value);
  }

  /**
   * Decodes a value effect.
   *
   * @param node the JSON object
   * @return the effect; an object with no {@code type} that is empty reads as {@code Empty}
   */
  public static LootDecoding<ValueEffect> valueEffect(JsonNode node) {
    Findings findings = new Findings();
    ValueEffect effect = readValueEffect(node, findings);
    return findings.result(effect);
  }

  /**
   * Decodes the {@code effects} table of an enchantment.
   *
   * @param node the JSON object, component to value
   * @param access the loaded registries and tags, for the conditions of the requirements
   * @return the components in file order
   */
  public static LootDecoding<EffectComponents> components(JsonNode node, RegistryAccess access) {
    Findings findings = new Findings();
    SequencedMap<Identifier, EffectComponentValue> components = new LinkedHashMap<>();
    Fields fields = Fields.of(node, findings);
    if (fields.valid()) {
      for (Map.Entry<String, JsonNode> member : fields.node().members().entrySet()) {
        Identifier id = Identifier.tryParse(member.getKey()).orElse(null);
        if (id == null || !ComponentForms.isRegistered(id)) {
          findings.add(
              LootFindingCode.UNKNOWN_TYPE, member.getValue(), "unknown effect " + member.getKey());
          continue;
        }
        components.put(id, new ComponentReader(access, findings).read(id, member.getValue()));
      }
    }
    return findings.result(new EffectComponents(components));
  }

  /** Reads a level-based value, recording what is wrong with it. */
  static LevelBasedValue readLevelBased(JsonNode node, Findings findings) {
    if (node instanceof JsonNumber number) {
      return new LevelBasedValue.Constant(Fields.floatOf(number, findings));
    }
    if (!(node instanceof JsonObject)) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected a number or an object");
      return null;
    }
    Fields fields = Fields.of(node, findings);
    Identifier type = fields.requiredId("type");
    if (type == null) {
      return null;
    }
    LevelBasedValue value = typedLevelBased(type, node, fields, findings);
    fields.finish();
    return value;
  }

  private static LevelBasedValue typedLevelBased(
      Identifier type, JsonNode node, Fields fields, Findings findings) {
    if (!type.namespace().equals("minecraft")) {
      findings.add(LootFindingCode.UNKNOWN_TYPE, node, "unknown level-based type " + type);
      return null;
    }
    return switch (type.path()) {
      case "linear" ->
          new LevelBasedValue.Linear(
              fields.requiredFloat("base"), fields.requiredFloat("per_level_above_first"));
      case "levels_squared" -> new LevelBasedValue.LevelsSquared(fields.requiredFloat("added"));
      case "fraction" ->
          new LevelBasedValue.Fraction(
              child(fields, "numerator", findings), child(fields, "denominator", findings));
      case "clamped" ->
          new LevelBasedValue.Clamped(
              child(fields, "value", findings),
              fields.requiredFloat("min"),
              fields.requiredFloat("max"));
      case "lookup" -> lookup(fields, findings);
      case "exponent" ->
          new LevelBasedValue.Exponent(
              child(fields, "base", findings), child(fields, "power", findings));
      default -> {
        findings.add(LootFindingCode.UNKNOWN_TYPE, node, "unknown level-based type " + type);
        yield null;
      }
    };
  }

  private static LevelBasedValue lookup(Fields fields, Findings findings) {
    List<Float> values = new ArrayList<>();
    JsonArray array = fields.requiredArray("values");
    if (array != null) {
      for (JsonNode element : array.elements()) {
        values.add(Fields.floatOf(element, findings));
      }
    }
    return new LevelBasedValue.Lookup(List.copyOf(values), child(fields, "fallback", findings));
  }

  private static LevelBasedValue child(Fields fields, String key, Findings findings) {
    JsonNode node = fields.required(key);
    return node == null ? null : readLevelBased(node, findings);
  }

  /** Reads a value effect, recording what is wrong with it. */
  static ValueEffect readValueEffect(JsonNode node, Findings findings) {
    if (node instanceof JsonObject object && object.members().isEmpty()) {
      return new ValueEffect.Empty();
    }
    Fields fields = Fields.of(node, findings);
    Identifier type = fields.requiredId("type");
    if (type == null) {
      return null;
    }
    ValueEffect effect = typedValueEffect(type, node, fields, findings);
    fields.finish();
    return effect;
  }

  private static ValueEffect typedValueEffect(
      Identifier type, JsonNode node, Fields fields, Findings findings) {
    String name = type.namespace().equals("minecraft") ? type.path() : "";
    return switch (name) {
      case "add" -> new ValueEffect.Add(child(fields, "value", findings));
      case "set" -> new ValueEffect.Set(child(fields, "value", findings));
      case "multiply" -> new ValueEffect.Multiply(child(fields, "factor", findings));
      case "remove_binomial" -> new ValueEffect.RemoveBinomial(child(fields, "chance", findings));
      case "exponential" -> new ValueEffect.Exponential(child(fields, "factor", findings));
      case "all_of" -> allOf(fields, findings);
      default -> {
        findings.add(LootFindingCode.UNKNOWN_TYPE, node, "unknown value effect " + type);
        yield null;
      }
    };
  }

  private static ValueEffect allOf(Fields fields, Findings findings) {
    List<ValueEffect> effects = new ArrayList<>();
    JsonArray array = fields.requiredArray("effects");
    if (array != null) {
      for (JsonNode element : array.elements()) {
        effects.add(readValueEffect(element, findings));
      }
    }
    return new ValueEffect.AllOf(effects);
  }
}
