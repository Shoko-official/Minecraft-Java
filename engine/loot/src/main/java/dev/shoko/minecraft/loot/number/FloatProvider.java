package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.context.NodeBudget;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.List;

/**
 * A float provider (registry {@code context_float_provider_type}, LT-R50). A bare number in the
 * data is a {@link Constant}. Values are 32-bit floats (EN-R21).
 */
public sealed interface FloatProvider
    permits FloatProvider.Constant,
        FloatProvider.Uniform,
        FloatProvider.WeightedList,
        FloatProvider.Conditional,
        FloatProvider.NumberDispatcher,
        FloatProvider.EnchantmentLevel,
        FloatProvider.FromInt,
        FloatProvider.Arithmetic,
        FloatProvider.Reference,
        FloatProvider.Unspecified {

  /** The operators of the arithmetic types, applied to the operands in order. */
  enum Operator {
    /** {@code abs}. */
    ABS,
    /** {@code avg}. */
    AVG,
    /** {@code ceil}. */
    CEIL,
    /** {@code floor}. */
    FLOOR,
    /** {@code round}. */
    ROUND,
    /** {@code truncate}. */
    TRUNCATE,
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
    /** {@code negate}. */
    NEGATE,
    /** {@code pow}. */
    POW,
    /** {@code sqrt}. */
    SQRT,
    /** {@code sin}. */
    SIN,
    /** {@code cos}. */
    COS,
    /** {@code min}. */
    MIN,
    /** {@code max}. */
    MAX
  }

  /**
   * Evaluates the provider. {@code uniform}: {@code min + nextFloat() * (max - min)}, one draw
   * (LT-R52, provisional); {@code enchantment_level}: the level-based value at the {@code
   * enchantment_level} parameter of the context, 0 level when absent; the other types as in {@link
   * IntProvider#sample}.
   *
   * @param context the context; its source is consumed
   * @return the value
   * @throws IllegalStateException when a {@link Reference} names a provider that is not loaded
   */
  default float sample(LootContext context) {
    NodeBudget budget = context.budget();
    budget.enter();
    try {
      return FloatSampler.sample(this, context, 0);
    } finally {
      budget.exit();
    }
  }

  /**
   * A constant.
   *
   * @param value the value
   */
  record Constant(float value) implements FloatProvider {}

  /**
   * A float between two bounds (LT-R52).
   *
   * @param min the lower bound
   * @param max the upper bound
   */
  record Uniform(FloatProvider min, FloatProvider max) implements FloatProvider {}

  /**
   * A weighted choice among providers.
   *
   * @param distribution the entries in file order, at least one
   */
  record WeightedList(List<WeightedEntry<FloatProvider>> distribution) implements FloatProvider {}

  /**
   * A branch on a condition (cooking speed).
   *
   * @param condition the condition, evaluated with the context
   * @param onTrue the value when it holds
   * @param onFalse the value when it does not
   */
  record Conditional(LootCondition condition, FloatProvider onTrue, FloatProvider onFalse)
      implements FloatProvider {}

  /**
   * The first case whose condition holds, else the default.
   *
   * @param cases the cases in file order
   * @param fallback the value when no case holds
   */
  record NumberDispatcher(List<Case> cases, FloatProvider fallback) implements FloatProvider {

    /**
     * One case.
     *
     * @param condition the condition
     * @param value the value when it holds
     */
    public record Case(LootCondition condition, FloatProvider value) {}
  }

  /**
   * A value that depends on the level of the enchantment being evaluated, seen in the {@code
   * random_chance} of enchantments (LT-R30).
   *
   * @param amount the value at the level
   */
  record EnchantmentLevel(LevelBasedValue amount) implements FloatProvider {}

  /**
   * An integer provider read as a float.
   *
   * @param source the integer provider
   */
  record FromInt(IntProvider source) implements FloatProvider {}

  /**
   * An arithmetic combination.
   *
   * @param operator the operator
   * @param operands one for the unary operators, two or more otherwise, in file order
   */
  record Arithmetic(Operator operator, List<FloatProvider> operands) implements FloatProvider {}

  /**
   * A reference to a named provider of the loaded data, such as {@code
   * minecraft:cooking/normal_speed_multiplier}.
   *
   * @param id the identifier of a {@code context_float_provider} element
   */
  record Reference(Identifier id) implements FloatProvider {}

  /**
   * A type with no field in the data ({@code storage}, {@code environment_attribute}, {@code
   * length}). Kept as read; evaluated as 0 with a finding (provisional).
   *
   * @param type the type identifier
   * @param fields the object as read
   */
  record Unspecified(Identifier type, JsonObject fields) implements FloatProvider {}
}
