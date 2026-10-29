package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.Fields;
import dev.shoko.minecraft.loot.Findings;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.enchant.effect.EffectCodec;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** The two provider decoders, which read each other for the conversions and {@code binomial}. */
final class Decoders {

  /** Registry {@code context_int_provider_type}, in registry order (LT-R50). */
  static final List<String> INT_TYPES =
      List.of(
          "constant",
          "uniform",
          "binomial",
          "weighted_list",
          "conditional",
          "number_dispatcher",
          "score",
          "storage",
          "environment_attribute",
          "from_float",
          "abs",
          "avg",
          "add",
          "sub",
          "mul",
          "div",
          "mod",
          "floor_div",
          "floor_mod",
          "negate",
          "pow",
          "min",
          "max");

  /** Registry {@code context_float_provider_type}, in registry order (LT-R50). */
  static final List<String> FLOAT_TYPES =
      List.of(
          "constant",
          "uniform",
          "weighted_list",
          "conditional",
          "number_dispatcher",
          "storage",
          "environment_attribute",
          "enchantment_level",
          "from_int",
          "length",
          "abs",
          "avg",
          "ceil",
          "floor",
          "round",
          "truncate",
          "add",
          "sub",
          "mul",
          "div",
          "mod",
          "negate",
          "pow",
          "sqrt",
          "sin",
          "cos",
          "min",
          "max");

  final ProviderDecoder<IntProvider> ints;
  final ProviderDecoder<FloatProvider> floats;

  Decoders(RegistryAccess access) {
    this.ints = new ProviderDecoder<>(new IntFamily(), access);
    this.floats = new ProviderDecoder<>(new FloatFamily(), access);
  }

  private final class IntFamily implements ProviderDecoder.Family<IntProvider> {
    private final Set<String> types = Set.copyOf(INT_TYPES);

    @Override
    public Set<String> types() {
      return types;
    }

    @Override
    public IntProvider constant(JsonNumber value, Findings findings) {
      return new IntProvider.Constant(
          Fields.intOf(value, findings, Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Override
    public IntProvider uniform(IntProvider min, IntProvider max) {
      return new IntProvider.Uniform(min, max);
    }

    @Override
    public IntProvider weightedList(List<WeightedEntry<IntProvider>> distribution) {
      return new IntProvider.WeightedList(distribution);
    }

    @Override
    public IntProvider conditional(
        LootCondition condition, IntProvider onTrue, IntProvider onFalse) {
      return new IntProvider.Conditional(condition, onTrue, onFalse);
    }

    @Override
    public IntProvider dispatcher(
        List<LootCondition> conditions, List<IntProvider> values, IntProvider fallback) {
      List<IntProvider.NumberDispatcher.Case> cases = new java.util.ArrayList<>();
      for (int i = 0; i < conditions.size(); i++) {
        cases.add(new IntProvider.NumberDispatcher.Case(conditions.get(i), values.get(i)));
      }
      return new IntProvider.NumberDispatcher(cases, fallback);
    }

    @Override
    public IntProvider arithmetic(String operator, List<IntProvider> operands) {
      return new IntProvider.Arithmetic(
          IntProvider.Operator.valueOf(operator.toUpperCase(Locale.ROOT)), operands);
    }

    @Override
    public IntProvider reference(Identifier id) {
      return new IntProvider.Reference(id);
    }

    @Override
    public IntProvider unspecified(Identifier type, JsonObject fields) {
      return new IntProvider.Unspecified(type, fields);
    }

    @Override
    public IntProvider special(
        String type, Fields fields, Findings findings, ProviderDecoder<IntProvider> decoder) {
      return switch (type) {
        case "binomial" ->
            new IntProvider.Binomial(
                decoder.child(fields, "n", findings), floats.child(fields, "p", findings));
        case "from_float" -> new IntProvider.FromFloat(floats.child(fields, "argument", findings));
        default -> null;
      };
    }
  }

  private final class FloatFamily implements ProviderDecoder.Family<FloatProvider> {
    private final Set<String> types = Set.copyOf(FLOAT_TYPES);

    @Override
    public Set<String> types() {
      return types;
    }

    @Override
    public FloatProvider constant(JsonNumber value, Findings findings) {
      return new FloatProvider.Constant(Fields.floatOf(value, findings));
    }

    @Override
    public FloatProvider uniform(FloatProvider min, FloatProvider max) {
      return new FloatProvider.Uniform(min, max);
    }

    @Override
    public FloatProvider weightedList(List<WeightedEntry<FloatProvider>> distribution) {
      return new FloatProvider.WeightedList(distribution);
    }

    @Override
    public FloatProvider conditional(
        LootCondition condition, FloatProvider onTrue, FloatProvider onFalse) {
      return new FloatProvider.Conditional(condition, onTrue, onFalse);
    }

    @Override
    public FloatProvider dispatcher(
        List<LootCondition> conditions, List<FloatProvider> values, FloatProvider fallback) {
      List<FloatProvider.NumberDispatcher.Case> cases = new java.util.ArrayList<>();
      for (int i = 0; i < conditions.size(); i++) {
        cases.add(new FloatProvider.NumberDispatcher.Case(conditions.get(i), values.get(i)));
      }
      return new FloatProvider.NumberDispatcher(cases, fallback);
    }

    @Override
    public FloatProvider arithmetic(String operator, List<FloatProvider> operands) {
      return new FloatProvider.Arithmetic(
          FloatProvider.Operator.valueOf(operator.toUpperCase(Locale.ROOT)), operands);
    }

    @Override
    public FloatProvider reference(Identifier id) {
      return new FloatProvider.Reference(id);
    }

    @Override
    public FloatProvider unspecified(Identifier type, JsonObject fields) {
      return new FloatProvider.Unspecified(type, fields);
    }

    @Override
    public FloatProvider special(
        String type, Fields fields, Findings findings, ProviderDecoder<FloatProvider> decoder) {
      return switch (type) {
        case "from_int" -> new FloatProvider.FromInt(ints.child(fields, "argument", findings));
        case "enchantment_level" -> enchantmentLevel(fields, findings);
        default -> null;
      };
    }

    private FloatProvider enchantmentLevel(Fields fields, Findings findings) {
      var amount = fields.required("amount");
      if (amount == null) {
        return null;
      }
      LevelBasedValue value = findings.take(EffectCodec.levelBased(amount));
      return new FloatProvider.EnchantmentLevel(value);
    }
  }

  /** Reports a reference that no loaded provider answers. */
  static void unresolvedFinding(Findings findings, Identifier owner, Identifier target) {
    findings.add(
        LootFindingCode.UNRESOLVED_REFERENCE,
        "$",
        owner + " references missing provider " + target);
  }
}
