package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.core.math.WeightedSelection;
import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.loot.context.LootContext;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

/** Evaluation of {@link IntProvider} trees (LT-R50 to LT-R53). */
final class IntSampler {

  /** Bound on chained references, so that a cycle of named providers ends in a typed error. */
  static final int MAX_REFERENCE_DEPTH = 64;

  /** Bound on the trials of a binomial, far above what any data asks for. */
  static final int MAX_TRIALS = 100_000;

  private IntSampler() {}

  static int sample(IntProvider provider, LootContext context, int depth) {
    return switch (provider) {
      case IntProvider.Constant constant -> constant.value();
      case IntProvider.Uniform uniform -> uniform(uniform, context, depth);
      case IntProvider.Binomial binomial -> binomial(binomial, context, depth);
      case IntProvider.WeightedList list -> weighted(list.distribution(), context, depth);
      case IntProvider.Conditional conditional ->
          sample(
              conditional.condition().test(context).passed()
                  ? conditional.onTrue()
                  : conditional.onFalse(),
              context,
              depth);
      case IntProvider.NumberDispatcher dispatcher -> dispatch(dispatcher, context, depth);
      case IntProvider.FromFloat fromFloat ->
          (int) Math.floor(FloatSampler.sample(fromFloat.source(), context, depth));
      case IntProvider.Arithmetic arithmetic -> arithmetic(arithmetic, context, depth);
      case IntProvider.Reference reference -> reference(reference, context, depth);
      case IntProvider.Unspecified unspecified -> 0;
    };
  }

  private static int uniform(IntProvider.Uniform uniform, LootContext context, int depth) {
    int min = sample(uniform.min(), context, depth);
    int max = sample(uniform.max(), context, depth);
    return uniformBetween(context.random(), min, max);
  }

  /** Draws in [min, max]; a range wider than an int is drawn from a long (LT-R51). */
  static int uniformBetween(RandomSource random, int min, int max) {
    if (min >= max) {
      return min;
    }
    long size = (long) max - min + 1;
    if (size <= Integer.MAX_VALUE) {
      return random.nextIntBetweenInclusive(min, max);
    }
    return (int) (min + Math.floorMod(random.nextLong(), size));
  }

  private static int binomial(IntProvider.Binomial binomial, LootContext context, int depth) {
    int trials = Math.min(sample(binomial.n(), context, depth), MAX_TRIALS);
    float probability = FloatSampler.sample(binomial.p(), context, depth);
    RandomSource random = context.random();
    int successes = 0;
    for (int i = 0; i < trials; i++) {
      if (random.nextFloat() < probability) {
        successes++;
      }
    }
    return successes;
  }

  private static int weighted(
      List<WeightedEntry<IntProvider>> distribution, LootContext context, int depth) {
    int[] weights = distribution.stream().mapToInt(WeightedEntry::weight).toArray();
    RandomSource random = context.random();
    OptionalInt picked = WeightedSelection.weightedPick(weights, random::nextInt);
    if (picked.isEmpty()) {
      return 0;
    }
    return sample(distribution.get(picked.getAsInt()).data(), context, depth);
  }

  private static int dispatch(
      IntProvider.NumberDispatcher dispatcher, LootContext context, int depth) {
    for (IntProvider.NumberDispatcher.Case branch : dispatcher.cases()) {
      if (branch.condition().test(context).passed()) {
        return sample(branch.value(), context, depth);
      }
    }
    return sample(dispatcher.fallback(), context, depth);
  }

  private static int reference(IntProvider.Reference reference, LootContext context, int depth) {
    if (depth >= MAX_REFERENCE_DEPTH) {
      throw new IllegalStateException("provider references nested too deep: " + reference.id());
    }
    IntProvider named =
        context
            .data()
            .numbers()
            .intProvider(reference.id())
            .orElseThrow(() -> new IllegalStateException("provider not loaded: " + reference.id()));
    return sample(named, context, depth + 1);
  }

  private static int arithmetic(IntProvider.Arithmetic node, LootContext context, int depth) {
    int[] values = new int[node.operands().size()];
    for (int i = 0; i < values.length; i++) {
      values[i] = sample(node.operands().get(i), context, depth);
    }
    if (values.length == 0) {
      return 0;
    }
    return apply(node.operator(), values);
  }

  private static int apply(IntProvider.Operator operator, int[] v) {
    int first = v[0];
    int second = v.length > 1 ? v[1] : 0;
    return switch (operator) {
      case ABS -> Math.abs(first);
      case NEGATE -> -first;
      case AVG -> (int) (sum(v) / v.length);
      case ADD -> (int) sum(v);
      case MUL -> product(v);
      case MIN -> Arrays.stream(v).min().orElse(0);
      case MAX -> Arrays.stream(v).max().orElse(0);
      case SUB -> first - second;
      case DIV -> second == 0 ? 0 : first / second;
      case MOD -> second == 0 ? 0 : first % second;
      case FLOOR_DIV -> second == 0 ? 0 : Math.floorDiv(first, second);
      case FLOOR_MOD -> second == 0 ? 0 : Math.floorMod(first, second);
      case POW -> (int) Math.pow(first, second);
    };
  }

  private static long sum(int[] values) {
    long total = 0;
    for (int value : values) {
      total += value;
    }
    return total;
  }

  private static int product(int[] values) {
    int result = 1;
    for (int value : values) {
      result *= value;
    }
    return result;
  }
}
