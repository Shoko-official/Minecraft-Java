package dev.shoko.minecraft.loot.number;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R50 (float providers), LT-R52 (uniform float), LT-R30 (enchantment_level in chances)
class FloatProviderTest {

  private static FloatProvider.Constant c(float value) {
    return new FloatProvider.Constant(value);
  }

  private static LootContext context(RandomSource random) {
    return Ctx.context(random);
  }

  private static FloatProvider arithmetic(FloatProvider.Operator operator, float... operands) {
    List<FloatProvider> list = new java.util.ArrayList<>();
    for (float operand : operands) {
      list.add(c(operand));
    }
    return new FloatProvider.Arithmetic(operator, list);
  }

  @Test
  void aConstantReturnsItsValueWithoutDrawing() {
    // spec: LT-R50 (a bare number is a constant)
    ScriptedRandom random = ScriptedRandom.of();
    assertThat(c(0.25f).sample(context(random))).isEqualTo(0.25f);
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalUniformIsMinPlusOneFloatTimesTheSpan() {
    // spec: LT-R52: min + nextFloat() * (max - min), one consumption
    ScriptedRandom random = ScriptedRandom.of(0.5);
    FloatProvider uniform = new FloatProvider.Uniform(c(1.0f), c(3.0f));
    assertThat(uniform.sample(context(random))).isEqualTo(2.0f);
    assertThat(random.steps()).isEqualTo(1);
  }

  @Test
  void provisionalUniformStaysInsideItsBounds() {
    // spec: LT-R52 (looting: uniform 0.0 to 1.0)
    RandomSource random = Lcg48Random.ofSeed(3);
    FloatProvider uniform = new FloatProvider.Uniform(c(0.0f), c(1.0f));
    for (int i = 0; i < 300; i++) {
      assertThat(uniform.sample(context(random))).isBetween(0.0f, 1.0f);
    }
  }

  @Test
  void provisionalAnEnchantmentLevelProviderReadsTheLevelOfTheContext() {
    // spec: LT-R30 (random_chance with enchantment_level), EN-R21 (linear)
    FloatProvider chance =
        new FloatProvider.EnchantmentLevel(new LevelBasedValue.Linear(0.5f, 0.25f));
    LootContext level3 = Ctx.builder(ScriptedRandom.of()).enchantment(3, true).build();
    assertThat(chance.sample(level3)).isEqualTo(1.0f);
    LootContext level1 = Ctx.builder(ScriptedRandom.of()).enchantment(1, true).build();
    assertThat(chance.sample(level1)).isEqualTo(0.5f);
  }

  @Test
  void provisionalAnIntegerProviderReadAsAFloatKeepsItsValue() {
    // spec: LT-R50 (from_int)
    FloatProvider fromInt = new FloatProvider.FromInt(new IntProvider.Constant(4));
    assertThat(fromInt.sample(context(ScriptedRandom.of()))).isEqualTo(4.0f);
  }

  @Test
  void provisionalArithmeticCombinesItsOperandsInOrder() {
    // spec: LT-R50 (add, sub, mul, min, max, negate, abs, avg, floor, ceil, truncate, sqrt, pow)
    LootContext context = context(ScriptedRandom.of());
    assertThat(arithmetic(FloatProvider.Operator.ADD, 1.5f, 2.0f).sample(context)).isEqualTo(3.5f);
    assertThat(arithmetic(FloatProvider.Operator.SUB, 1.0f, 3.0f).sample(context)).isEqualTo(-2.0f);
    assertThat(arithmetic(FloatProvider.Operator.MUL, 1.5f, 4.0f).sample(context)).isEqualTo(6.0f);
    assertThat(arithmetic(FloatProvider.Operator.MIN, 1.0f, 3.0f).sample(context)).isEqualTo(1.0f);
    assertThat(arithmetic(FloatProvider.Operator.MAX, 1.0f, 3.0f).sample(context)).isEqualTo(3.0f);
    assertThat(arithmetic(FloatProvider.Operator.NEGATE, 2.0f).sample(context)).isEqualTo(-2.0f);
    assertThat(arithmetic(FloatProvider.Operator.ABS, -2.5f).sample(context)).isEqualTo(2.5f);
    assertThat(arithmetic(FloatProvider.Operator.AVG, 2.0f, 4.0f).sample(context)).isEqualTo(3.0f);
    assertThat(arithmetic(FloatProvider.Operator.FLOOR, 2.7f).sample(context)).isEqualTo(2.0f);
    assertThat(arithmetic(FloatProvider.Operator.CEIL, 2.1f).sample(context)).isEqualTo(3.0f);
    assertThat(arithmetic(FloatProvider.Operator.TRUNCATE, -2.7f).sample(context)).isEqualTo(-2.0f);
    assertThat(arithmetic(FloatProvider.Operator.SQRT, 9.0f).sample(context)).isEqualTo(3.0f);
    assertThat(arithmetic(FloatProvider.Operator.POW, 2.0f, 3.0f).sample(context)).isEqualTo(8.0f);
    assertThat(arithmetic(FloatProvider.Operator.SIN, 0.0f).sample(context)).isEqualTo(0.0f);
    assertThat(arithmetic(FloatProvider.Operator.COS, 0.0f).sample(context)).isEqualTo(1.0f);
  }

  @Test
  void provisionalAConditionalPicksItsBranchFromTheCondition() {
    // spec: LT-R50 (conditional, cooking speed)
    FloatProvider yes =
        new FloatProvider.Conditional(new LootCondition.AllOf(List.of()), c(2.0f), c(1.0f));
    FloatProvider no =
        new FloatProvider.Conditional(new LootCondition.AnyOf(List.of()), c(2.0f), c(1.0f));
    assertThat(yes.sample(context(ScriptedRandom.of()))).isEqualTo(2.0f);
    assertThat(no.sample(context(ScriptedRandom.of()))).isEqualTo(1.0f);
  }

  @Test
  void provisionalANumberDispatcherTakesTheFirstCaseThatHolds() {
    // spec: LT-R50 (number_dispatcher)
    FloatProvider dispatcher =
        new FloatProvider.NumberDispatcher(
            List.of(
                new FloatProvider.NumberDispatcher.Case(
                    new LootCondition.AnyOf(List.of()), c(1.0f)),
                new FloatProvider.NumberDispatcher.Case(
                    new LootCondition.AllOf(List.of()), c(2.0f))),
            c(3.0f));
    assertThat(dispatcher.sample(context(ScriptedRandom.of()))).isEqualTo(2.0f);
  }

  @Test
  void provisionalAWeightedListFollowsItsWeights() {
    // spec: LT-R50 (weighted_list)
    FloatProvider list =
        new FloatProvider.WeightedList(
            List.of(
                new WeightedEntry<FloatProvider>(c(1.0f), 3),
                new WeightedEntry<FloatProvider>(c(2.0f), 1)));
    RandomSource random = Lcg48Random.ofSeed(8);
    int ones = 0;
    int samples = 4000;
    for (int i = 0; i < samples; i++) {
      float value = list.sample(context(random));
      assertThat(Arrays.asList(1.0f, 2.0f)).contains(value);
      if (value == 1.0f) {
        ones++;
      }
    }
    assertThat(ones / (double) samples).isBetween(0.70, 0.80);
  }

  @Test
  void aReferenceEvaluatesTheNamedProviderOfTheLoadedData() {
    // spec: LT-R50 (named providers, e.g. cooking/speed_default)
    LootContext context =
        LootContext.builder(
                Ctx.data(
                    Map.of(),
                    Ctx.standardPredicates(),
                    Ctx.providers(Map.of(), Map.of("cooking/speed_default", c(1.5f)))),
                Ctx.ANY,
                ScriptedRandom.of())
            .build();
    assertThat(
            new FloatProvider.Reference(Identifier.minecraft("cooking/speed_default"))
                .sample(context))
        .isEqualTo(1.5f);
  }

  @Test
  void aReferenceToAProviderThatIsNotLoadedIsAnIllegalState() {
    // spec: FloatProvider#sample contract (IllegalStateException)
    LootContext context = context(ScriptedRandom.of());
    FloatProvider missing = new FloatProvider.Reference(Identifier.minecraft("no/such_provider"));
    assertThatThrownBy(() -> missing.sample(context)).isInstanceOf(IllegalStateException.class);
  }
}
