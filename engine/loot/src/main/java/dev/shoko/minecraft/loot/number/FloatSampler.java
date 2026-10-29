package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.random.Weights;
import java.util.List;
import java.util.OptionalInt;

/** Evaluation of {@link FloatProvider} trees (LT-R50, LT-R52). */
final class FloatSampler {

  private FloatSampler() {}

  static float sample(FloatProvider provider, LootContext context, int depth) {
    return switch (provider) {
      case FloatProvider.Constant constant -> constant.value();
      case FloatProvider.Uniform uniform -> uniform(uniform, context, depth);
      case FloatProvider.WeightedList list -> weighted(list.distribution(), context, depth);
      case FloatProvider.Conditional conditional ->
          sample(
              conditional.condition().test(context).passed()
                  ? conditional.onTrue()
                  : conditional.onFalse(),
              context,
              depth);
      case FloatProvider.NumberDispatcher dispatcher -> dispatch(dispatcher, context, depth);
      case FloatProvider.EnchantmentLevel level ->
          level.amount().calculate(context.enchantmentLevel().orElse(0));
      case FloatProvider.FromInt fromInt -> IntSampler.sample(fromInt.source(), context, depth);
      case FloatProvider.Arithmetic arithmetic -> arithmetic(arithmetic, context, depth);
      case FloatProvider.Reference reference -> reference(reference, context, depth);
      case FloatProvider.Unspecified unspecified -> 0f;
    };
  }

  private static float uniform(FloatProvider.Uniform uniform, LootContext context, int depth) {
    float min = sample(uniform.min(), context, depth);
    float max = sample(uniform.max(), context, depth);
    return min + context.random().nextFloat() * (max - min);
  }

  private static float weighted(
      List<WeightedEntry<FloatProvider>> distribution, LootContext context, int depth) {
    int[] weights = distribution.stream().mapToInt(WeightedEntry::weight).toArray();
    RandomSource random = context.random();
    OptionalInt picked = Weights.pick(weights, random);
    if (picked.isEmpty()) {
      return 0f;
    }
    return sample(distribution.get(picked.getAsInt()).data(), context, depth);
  }

  private static float dispatch(
      FloatProvider.NumberDispatcher dispatcher, LootContext context, int depth) {
    for (FloatProvider.NumberDispatcher.Case branch : dispatcher.cases()) {
      if (branch.condition().test(context).passed()) {
        return sample(branch.value(), context, depth);
      }
    }
    return sample(dispatcher.fallback(), context, depth);
  }

  private static float reference(
      FloatProvider.Reference reference, LootContext context, int depth) {
    if (depth >= IntSampler.MAX_REFERENCE_DEPTH) {
      throw new IllegalStateException("provider references nested too deep: " + reference.id());
    }
    FloatProvider named =
        context
            .data()
            .numbers()
            .floatProvider(reference.id())
            .orElseThrow(() -> new IllegalStateException("provider not loaded: " + reference.id()));
    return sample(named, context, depth + 1);
  }

  private static float arithmetic(FloatProvider.Arithmetic node, LootContext context, int depth) {
    float[] values = new float[node.operands().size()];
    for (int i = 0; i < values.length; i++) {
      values[i] = sample(node.operands().get(i), context, depth);
    }
    if (values.length == 0) {
      return 0f;
    }
    return apply(node.operator(), values);
  }

  private static float apply(FloatProvider.Operator operator, float[] v) {
    float first = v[0];
    float second = v.length > 1 ? v[1] : 0f;
    return switch (operator) {
      case ABS -> Math.abs(first);
      case NEGATE -> -first;
      case CEIL -> (float) Math.ceil(first);
      case FLOOR -> (float) Math.floor(first);
      case ROUND -> Math.round(first);
      case TRUNCATE -> first < 0 ? (float) Math.ceil(first) : (float) Math.floor(first);
      case SQRT -> (float) Math.sqrt(first);
      case SIN -> (float) Math.sin(first);
      case COS -> (float) Math.cos(first);
      case AVG -> sum(v) / v.length;
      case ADD -> sum(v);
      case MUL -> product(v);
      case MIN -> min(v);
      case MAX -> max(v);
      case SUB -> first - second;
      case DIV -> first / second;
      case MOD -> first % second;
      case POW -> (float) Math.pow(first, second);
    };
  }

  private static float sum(float[] values) {
    float total = 0f;
    for (float value : values) {
      total += value;
    }
    return total;
  }

  private static float product(float[] values) {
    float result = 1f;
    for (float value : values) {
      result *= value;
    }
    return result;
  }

  private static float min(float[] values) {
    float result = values[0];
    for (float value : values) {
      result = Math.min(result, value);
    }
    return result;
  }

  private static float max(float[] values) {
    float result = values[0];
    for (float value : values) {
      result = Math.max(result, value);
    }
    return result;
  }
}
