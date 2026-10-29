package dev.shoko.minecraft.loot.encode;

import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.number.WeightedEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.Function;

/** Encodes number providers and level-based values to a JSON tree (inverse of the decoders). */
final class ProviderJson {

  private static final Set<String> NARY = Set.of("add", "mul", "min", "max", "avg");
  private static final Set<String> BINARY =
      Set.of("sub", "div", "mod", "pow", "floor_div", "floor_mod");

  private ProviderJson() {}

  static Object intProvider(IntProvider provider) {
    return switch (provider) {
      case IntProvider.Constant constant -> JsonOut.number(constant.value());
      case IntProvider.Uniform uniform ->
          typed("uniform", "min", intProvider(uniform.min()), "max", intProvider(uniform.max()));
      case IntProvider.Binomial binomial ->
          typed("binomial", "n", intProvider(binomial.n()), "p", floatProvider(binomial.p()));
      case IntProvider.WeightedList list ->
          weighted(list.distribution(), ProviderJson::intProvider);
      case IntProvider.Conditional conditional ->
          typed(
              "conditional",
              "condition",
              ConditionJson.condition(conditional.condition()),
              "on_true",
              intProvider(conditional.onTrue()),
              "on_false",
              intProvider(conditional.onFalse()));
      case IntProvider.NumberDispatcher dispatcher -> {
        List<Object> cases = new ArrayList<>();
        dispatcher
            .cases()
            .forEach(c -> cases.add(dispatchCase(c.condition(), intProvider(c.value()))));
        yield typed(
            "number_dispatcher", "cases", cases, "default", intProvider(dispatcher.fallback()));
      }
      case IntProvider.FromFloat fromFloat ->
          typed("from_float", "argument", floatProvider(fromFloat.source()));
      case IntProvider.Arithmetic arithmetic ->
          arithmetic(
              arithmetic.operator().name().toLowerCase(Locale.ROOT),
              arithmetic.operands(),
              ProviderJson::intProvider);
      case IntProvider.Reference reference -> reference.id().toString();
      case IntProvider.Unspecified unspecified -> JsonOut.tree(unspecified.fields());
    };
  }

  static Object floatProvider(FloatProvider provider) {
    return switch (provider) {
      case FloatProvider.Constant constant -> JsonOut.number(constant.value());
      case FloatProvider.Uniform uniform ->
          typed(
              "uniform", "min", floatProvider(uniform.min()), "max", floatProvider(uniform.max()));
      case FloatProvider.WeightedList list ->
          weighted(list.distribution(), ProviderJson::floatProvider);
      case FloatProvider.Conditional conditional ->
          typed(
              "conditional",
              "condition",
              ConditionJson.condition(conditional.condition()),
              "on_true",
              floatProvider(conditional.onTrue()),
              "on_false",
              floatProvider(conditional.onFalse()));
      case FloatProvider.NumberDispatcher dispatcher -> {
        List<Object> cases = new ArrayList<>();
        dispatcher
            .cases()
            .forEach(c -> cases.add(dispatchCase(c.condition(), floatProvider(c.value()))));
        yield typed(
            "number_dispatcher", "cases", cases, "default", floatProvider(dispatcher.fallback()));
      }
      case FloatProvider.EnchantmentLevel level ->
          typed("enchantment_level", "amount", levelBased(level.amount()));
      case FloatProvider.FromInt fromInt ->
          typed("from_int", "argument", intProvider(fromInt.source()));
      case FloatProvider.Arithmetic arithmetic ->
          arithmetic(
              arithmetic.operator().name().toLowerCase(Locale.ROOT),
              arithmetic.operands(),
              ProviderJson::floatProvider);
      case FloatProvider.Reference reference -> reference.id().toString();
      case FloatProvider.Unspecified unspecified -> JsonOut.tree(unspecified.fields());
    };
  }

  static Object levelBased(LevelBasedValue value) {
    return switch (value) {
      case LevelBasedValue.Constant constant -> JsonOut.number(constant.value());
      case LevelBasedValue.Linear linear ->
          typed(
              "linear",
              "base",
              JsonOut.number(linear.base()),
              "per_level_above_first",
              JsonOut.number(linear.perLevelAboveFirst()));
      case LevelBasedValue.LevelsSquared squared ->
          typed("levels_squared", "added", JsonOut.number(squared.added()));
      case LevelBasedValue.Fraction fraction ->
          typed(
              "fraction",
              "numerator",
              levelBased(fraction.numerator()),
              "denominator",
              levelBased(fraction.denominator()));
      case LevelBasedValue.Clamped clamped ->
          typed(
              "clamped",
              "value",
              levelBased(clamped.value()),
              "min",
              JsonOut.number(clamped.min()),
              "max",
              JsonOut.number(clamped.max()));
      case LevelBasedValue.Lookup lookup ->
          typed(
              "lookup",
              "values",
              lookup.values().stream().map(v -> (Object) JsonOut.number(v.floatValue())).toList(),
              "fallback",
              levelBased(lookup.fallback()));
      case LevelBasedValue.Exponent exponent ->
          typed(
              "exponent",
              "base",
              levelBased(exponent.base()),
              "power",
              levelBased(exponent.power()));
    };
  }

  private static SequencedMap<String, Object> typed(String type, Object... keyValues) {
    SequencedMap<String, Object> object = JsonOut.object();
    object.put("type", "minecraft:" + type);
    for (int i = 0; i < keyValues.length; i += 2) {
      object.put((String) keyValues[i], keyValues[i + 1]);
    }
    return object;
  }

  private static Object dispatchCase(
      dev.shoko.minecraft.loot.predicate.LootCondition condition, Object value) {
    SequencedMap<String, Object> entry = JsonOut.object();
    entry.put("condition", ConditionJson.condition(condition));
    entry.put("value", value);
    return entry;
  }

  private static <T> Object weighted(
      List<WeightedEntry<T>> distribution, Function<T, Object> encode) {
    List<Object> entries = new ArrayList<>();
    for (WeightedEntry<T> entry : distribution) {
      SequencedMap<String, Object> item = JsonOut.object();
      item.put("data", encode.apply(entry.data()));
      item.put("weight", JsonOut.number(entry.weight()));
      entries.add(item);
    }
    return typed("weighted_list", "distribution", entries);
  }

  private static <T> Object arithmetic(String name, List<T> operands, Function<T, Object> encode) {
    if (NARY.contains(name)) {
      return typed(name, "inputs", operands.stream().map(encode).toList());
    }
    if (BINARY.contains(name)) {
      return typed(
          name, "left", encode.apply(operands.get(0)), "right", encode.apply(operands.get(1)));
    }
    return typed(name, "argument", encode.apply(operands.get(0)));
  }
}
