package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.List;

/**
 * An integer provider (registry {@code context_int_provider_type}, LT-R50). A bare number in the
 * data is a {@link Constant}. Providers are immutable trees; {@link #sample} evaluates one with a
 * context and consumes its random source in the order of the tree.
 */
public sealed interface IntProvider
    permits IntProvider.Constant,
        IntProvider.Uniform,
        IntProvider.Binomial,
        IntProvider.WeightedList,
        IntProvider.Conditional,
        IntProvider.NumberDispatcher,
        IntProvider.FromFloat,
        IntProvider.Arithmetic,
        IntProvider.Reference,
        IntProvider.Unspecified {

  /** The operators of the arithmetic types, applied to the operands in order. */
  enum Operator {
    /** {@code abs}. */
    ABS,
    /** {@code avg}. */
    AVG,
    /** {@code add}. */
    ADD,
    /** {@code sub}. */
    SUB,
    /** {@code mul}. */
    MUL,
    /** {@code div}. */
    DIV,
    /** {@code mod}. */
    MOD,
    /** {@code floor_div}. */
    FLOOR_DIV,
    /** {@code floor_mod}. */
    FLOOR_MOD,
    /** {@code negate}. */
    NEGATE,
    /** {@code pow}. */
    POW,
    /** {@code min}. */
    MIN,
    /** {@code max}. */
    MAX
  }

  /**
   * Evaluates the provider. {@code uniform}: bounds inclusive, {@code min + nextInt(max - min + 1)}
   * when {@code min < max}, else {@code min} without a draw (LT-R51); {@code binomial}: {@code n}
   * float draws below {@code p} (LT-R53, provisional); {@code weighted_list}: one weighted draw;
   * {@code conditional} and {@code number_dispatcher}: the first matching branch, conditions
   * evaluated in order; {@code Reference}: the named provider of the loaded data. The rounding of
   * {@code div}, {@code from_float} and {@code pow} is open (spec Q: arrondis).
   *
   * @param context the context; its source is consumed
   * @return the value
   * @throws IllegalStateException when a {@link Reference} names a provider that is not loaded
   */
  default int sample(LootContext context) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * A constant.
   *
   * @param value the value
   */
  record Constant(int value) implements IntProvider {}

  /**
   * An integer drawn between two inclusive bounds (LT-R51).
   *
   * @param min the lower bound
   * @param max the upper bound
   */
  record Uniform(IntProvider min, IntProvider max) implements IntProvider {}

  /**
   * A binomial count (LT-R53): successes among {@code n} trials of probability {@code p}.
   *
   * @param n the number of trials
   * @param p the probability of one trial
   */
  record Binomial(IntProvider n, FloatProvider p) implements IntProvider {}

  /**
   * A weighted choice among providers (compost layers, LT-R50).
   *
   * @param distribution the entries in file order, at least one
   */
  record WeightedList(List<WeightedEntry<IntProvider>> distribution) implements IntProvider {}

  /**
   * A branch on a condition (fuel durations).
   *
   * @param condition the condition, evaluated with the context
   * @param onTrue the value when it holds
   * @param onFalse the value when it does not
   */
  record Conditional(LootCondition condition, IntProvider onTrue, IntProvider onFalse)
      implements IntProvider {}

  /**
   * The first case whose condition holds, else the default (compost layers).
   *
   * @param cases the cases in file order
   * @param fallback the value when no case holds
   */
  record NumberDispatcher(List<Case> cases, IntProvider fallback) implements IntProvider {

    /**
     * One case.
     *
     * @param condition the condition
     * @param value the value when it holds
     */
    public record Case(LootCondition condition, IntProvider value) {}
  }

  /**
   * A float provider read as an integer.
   *
   * @param source the float provider; its rounding to an integer is open
   */
  record FromFloat(FloatProvider source) implements IntProvider {}

  /**
   * An arithmetic combination.
   *
   * @param operator the operator
   * @param operands one for the unary operators, two or more otherwise, in file order
   */
  record Arithmetic(Operator operator, List<IntProvider> operands) implements IntProvider {}

  /**
   * A reference to a named provider of the loaded data, such as {@code
   * minecraft:cooking/normal_burn_time_reduction_factor}.
   *
   * @param id the identifier of a {@code context_int_provider} element
   */
  record Reference(Identifier id) implements IntProvider {}

  /**
   * A type with no field in the data ({@code score}, {@code storage}, {@code
   * environment_attribute}). Kept as read; evaluated as 0 with a finding (provisional).
   *
   * @param type the type identifier
   * @param fields the object as read
   */
  record Unspecified(Identifier type, JsonObject fields) implements IntProvider {}
}
