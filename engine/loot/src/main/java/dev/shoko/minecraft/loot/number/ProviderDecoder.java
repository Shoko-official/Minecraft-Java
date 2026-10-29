package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.Fields;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.predicate.ConditionCodec;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Decodes one family of number providers (LT-R50). The two families, integer and float, share the
 * shapes: a number is a constant, a string a reference, an object has a {@code type}. What differs
 * is described by a {@link Family}.
 *
 * @param <T> the provider type of the family
 */
final class ProviderDecoder<T> {

  private static final Set<String> NARY = Set.of("add", "mul", "min", "max", "avg");
  private static final Set<String> BINARY = Set.of("sub", "div", "mod", "floor_div", "floor_mod");
  private static final Set<String> UNARY =
      Set.of(
          "abs",
          "negate",
          "ceil",
          "floor",
          "round",
          "truncate",
          "sqrt",
          "sin",
          "cos",
          "from_float",
          "from_int");
  private static final Set<String> OPAQUE =
      Set.of("score", "storage", "environment_attribute", "length");

  /** What differs between the integer and the float family. */
  interface Family<T> {
    /** The type names of the registry, without namespace. */
    Set<String> types();

    T constant(JsonNumber value, Findings findings);

    T uniform(T min, T max);

    T weightedList(List<WeightedEntry<T>> distribution);

    T conditional(LootCondition condition, T onTrue, T onFalse);

    T dispatcher(List<LootCondition> conditions, List<T> values, T fallback);

    T arithmetic(String operator, List<T> operands);

    T reference(Identifier id);

    T unspecified(Identifier type, JsonObject fields);

    /** Decodes a type that only this family has, {@code null} when the type is not special. */
    T special(String type, Fields fields, Findings findings, ProviderDecoder<T> decoder);
  }

  private final Family<T> family;
  private final RegistryAccess access;

  ProviderDecoder(Family<T> family, RegistryAccess access) {
    this.family = family;
    this.access = access;
  }

  LootDecoding<T> decode(JsonNode node) {
    Findings findings = new Findings();
    T value = read(node, findings);
    if (findings.failed()) {
      return findings.failure();
    }
    return findings.result(value);
  }

  T read(JsonNode node, Findings findings) {
    if (node instanceof JsonNumber number) {
      return family.constant(number, findings);
    }
    if (node instanceof JsonString string) {
      Identifier id = Fields.idOf(string, findings);
      return id == null ? null : family.reference(id);
    }
    if (node instanceof JsonObject object) {
      return object(object, findings);
    }
    findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected a number, an object or a string");
    return null;
  }

  private T object(JsonObject object, Findings findings) {
    Fields fields = Fields.of(object, findings);
    Identifier typeId = fields.requiredId("type");
    if (typeId == null) {
      return null;
    }
    String type = typeId.path();
    if (!typeId.namespace().equals("minecraft") || !family.types().contains(type)) {
      findings.add(LootFindingCode.UNKNOWN_TYPE, object, "unknown provider type " + typeId);
      return null;
    }
    T value = typed(type, object, fields, findings);
    if (OPAQUE.contains(type)) {
      return value;
    }
    fields.finish();
    return value;
  }

  private T typed(String type, JsonObject object, Fields fields, Findings findings) {
    if (OPAQUE.contains(type)) {
      return family.unspecified(Identifier.minecraft(type), object);
    }
    return switch (type) {
      case "constant" -> constantField(fields, findings);
      case "uniform" ->
          family.uniform(child(fields, "min", findings), child(fields, "max", findings));
      case "weighted_list" -> weighted(fields, findings);
      case "conditional" -> conditional(fields, findings);
      case "number_dispatcher" -> dispatcher(fields, findings);
      default -> other(type, fields, findings);
    };
  }

  private T other(String type, Fields fields, Findings findings) {
    if (NARY.contains(type)) {
      return family.arithmetic(type, operands(fields, findings));
    }
    if (type.equals("pow")) {
      // LT-R79 (provisional): pow reads base and exponent; a missing one is a finding.
      List<T> operands = new ArrayList<>();
      operands.add(child(fields, "base", findings));
      operands.add(child(fields, "exponent", findings));
      return family.arithmetic(type, operands);
    }
    if (BINARY.contains(type)) {
      List<T> operands = new ArrayList<>();
      operands.add(child(fields, "left", findings));
      operands.add(child(fields, "right", findings));
      return family.arithmetic(type, operands);
    }
    if (UNARY.contains(type) && !type.startsWith("from_")) {
      List<T> operands = new ArrayList<>();
      operands.add(child(fields, "input", findings));
      return family.arithmetic(type, operands);
    }
    return family.special(type, fields, findings, this);
  }

  /** Reads a member that holds a provider of this family. */
  T child(Fields fields, String key, Findings findings) {
    JsonNode node = fields.required(key);
    return node == null ? null : read(node, findings);
  }

  private T constantField(Fields fields, Findings findings) {
    JsonNode node = fields.required("value");
    if (node instanceof JsonNumber number) {
      return family.constant(number, findings);
    }
    if (node != null) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected a number");
    }
    return null;
  }

  private List<T> operands(Fields fields, Findings findings) {
    JsonArray array = fields.requiredArray("inputs");
    List<T> operands = new ArrayList<>();
    if (array != null) {
      for (JsonNode element : array.elements()) {
        operands.add(read(element, findings));
      }
    }
    return operands;
  }

  private T weighted(Fields fields, Findings findings) {
    JsonArray array = fields.requiredArray("distribution");
    List<WeightedEntry<T>> distribution = new ArrayList<>();
    if (array != null) {
      for (JsonNode element : array.elements()) {
        Fields entry = Fields.of(element, findings);
        T data = child(entry, "data", findings);
        int weight = entry.requiredInt("weight", 0, Integer.MAX_VALUE);
        entry.finish();
        distribution.add(new WeightedEntry<>(data, weight));
      }
      if (distribution.isEmpty()) {
        findings.add(LootFindingCode.VALUE_RANGE, array, "a weighted list needs an entry");
      }
    }
    return family.weightedList(distribution);
  }

  private LootCondition condition(JsonNode node, Findings findings) {
    return findings.take(ConditionCodec.decode(node, access));
  }

  private T conditional(Fields fields, Findings findings) {
    JsonNode condition = fields.required("condition");
    LootCondition decoded = condition == null ? null : condition(condition, findings);
    T onTrue = child(fields, "on_true", findings);
    T onFalse = child(fields, "on_false", findings);
    return family.conditional(decoded, onTrue, onFalse);
  }

  private T dispatcher(Fields fields, Findings findings) {
    JsonArray array = fields.requiredArray("cases");
    List<LootCondition> conditions = new ArrayList<>();
    List<T> values = new ArrayList<>();
    if (array != null) {
      for (JsonNode element : array.elements()) {
        Fields entry = Fields.of(element, findings);
        JsonNode condition = entry.required("condition");
        conditions.add(condition == null ? null : condition(condition, findings));
        values.add(child(entry, "value", findings));
        entry.finish();
      }
    }
    T fallback = child(fields, "default", findings);
    return family.dispatcher(conditions, values, fallback);
  }
}
