package dev.shoko.minecraft.loot.number;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

// spec: LT-R50 (int providers), LT-R51 (uniform), LT-R53 (binomial), LT-R5x (rolls forms)
class IntProviderTest {

  private static IntProvider.Constant c(int value) {
    return new IntProvider.Constant(value);
  }

  private static IntProvider.Uniform uniform(int min, int max) {
    return new IntProvider.Uniform(c(min), c(max));
  }

  private static LootContext context(RandomSource random) {
    return Ctx.context(random);
  }

  private static LootContext withKiller(RandomSource random) {
    return Ctx.builder(random)
        .entity(EntityTarget.ATTACKING_PLAYER, FakeEntity.of("player"))
        .build();
  }

  private static IntProvider arithmetic(IntProvider.Operator operator, int... operands) {
    return new IntProvider.Arithmetic(
        operator,
        java.util.Arrays.stream(operands)
            .<IntProvider>mapToObj(IntProvider.Constant::new)
            .toList());
  }

  @Test
  void aConstantReturnsItsValueWithoutDrawing() {
    // spec: LT-R50 (a bare number is a constant)
    ScriptedRandom random = ScriptedRandom.of();
    assertThat(c(7).sample(context(random))).isEqualTo(7);
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalUniformIsMinPlusABoundedDrawOverAnInclusiveRange() {
    // spec: LT-R51: min + nextInt(max - min + 1), one draw
    ScriptedRandom low = ScriptedRandom.of(0.0);
    ScriptedRandom middle = ScriptedRandom.of(0.4);
    ScriptedRandom high = ScriptedRandom.of(0.999);
    assertThat(uniform(1, 3).sample(context(low))).isEqualTo(1);
    assertThat(uniform(1, 3).sample(context(middle))).isEqualTo(2);
    assertThat(uniform(1, 3).sample(context(high))).isEqualTo(3);
    assertThat(low.steps()).isEqualTo(1);
    assertThat(high.steps()).isEqualTo(1);
  }

  @Test
  void provisionalUniformWithEqualBoundsReturnsTheBoundWithoutDrawing() {
    // spec: LT-R51 (min >= max: min, no draw)
    ScriptedRandom random = ScriptedRandom.of();
    assertThat(uniform(5, 5).sample(context(random))).isEqualTo(5);
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalUniformWithMinAboveMaxReturnsMinWithoutDrawing() {
    // spec: LT-R51 (min >= max: min, no draw); cas limite: min > max
    ScriptedRandom random = ScriptedRandom.of();
    assertThat(uniform(5, 2).sample(context(random))).isEqualTo(5);
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalUniformWithNegativeBoundsStaysInside() {
    // spec: LT-R51
    RandomSource random = Lcg48Random.ofSeed(11);
    for (int i = 0; i < 200; i++) {
      assertThat(uniform(-3, -1).sample(context(random))).isBetween(-3, -1);
    }
  }

  @Test
  void provisionalUniformReachesEveryValueOfTheRange() {
    // spec: LT-R51 (bounds inclusive)
    RandomSource random = Lcg48Random.ofSeed(2024);
    TreeSet<Integer> seen = new TreeSet<>();
    for (int i = 0; i < 400; i++) {
      seen.add(uniform(0, 3).sample(context(random)));
    }
    assertThat(seen).containsExactly(0, 1, 2, 3);
  }

  @Test
  void provisionalBinomialCountsTheDrawsBelowP() {
    // spec: LT-R53: n float draws, success when strictly below p
    ScriptedRandom random = ScriptedRandom.of(0.1, 0.9, 0.4);
    IntProvider binomial = new IntProvider.Binomial(c(3), new FloatProvider.Constant(0.5f));
    assertThat(binomial.sample(context(random))).isEqualTo(2);
    assertThat(random.steps()).isEqualTo(3);
  }

  @Test
  void provisionalBinomialDrawExactlyEqualToPIsNotASuccess() {
    // spec: LT-R53 (strictly below)
    ScriptedRandom random = ScriptedRandom.of(0.5);
    IntProvider binomial = new IntProvider.Binomial(c(1), new FloatProvider.Constant(0.5f));
    assertThat(binomial.sample(context(random))).isZero();
  }

  @Test
  void provisionalBinomialOfNothingDrawsNothing() {
    // spec: LT-R53 (n tests consumed in order)
    ScriptedRandom random = ScriptedRandom.of();
    IntProvider binomial = new IntProvider.Binomial(c(0), new FloatProvider.Constant(0.5f));
    assertThat(binomial.sample(context(random))).isZero();
    assertThat(random.steps()).isZero();
  }

  @Test
  void provisionalBinomialMeanIsNTimesP() {
    // spec: LT-R53 (melon seeds: n=3, p=0.5333)
    RandomSource random = Lcg48Random.ofSeed(7);
    IntProvider binomial = new IntProvider.Binomial(c(3), new FloatProvider.Constant(0.53333336f));
    long total = 0;
    int samples = 6000;
    for (int i = 0; i < samples; i++) {
      total += binomial.sample(context(random));
    }
    assertThat(total / (double) samples).isBetween(1.5, 1.7);
  }

  @Test
  void provisionalArithmeticCombinesItsOperandsInOrder() {
    // spec: LT-R50 (add, sub, mul, min, max, negate, abs, floor_div, floor_mod)
    LootContext context = context(ScriptedRandom.of());
    assertThat(arithmetic(IntProvider.Operator.ADD, 2, 3, 4).sample(context)).isEqualTo(9);
    assertThat(arithmetic(IntProvider.Operator.SUB, 2, 5).sample(context)).isEqualTo(-3);
    assertThat(arithmetic(IntProvider.Operator.MUL, 3, 4).sample(context)).isEqualTo(12);
    assertThat(arithmetic(IntProvider.Operator.MIN, 3, 4).sample(context)).isEqualTo(3);
    assertThat(arithmetic(IntProvider.Operator.MAX, 3, 4).sample(context)).isEqualTo(4);
    assertThat(arithmetic(IntProvider.Operator.NEGATE, 6).sample(context)).isEqualTo(-6);
    assertThat(arithmetic(IntProvider.Operator.ABS, -6).sample(context)).isEqualTo(6);
    assertThat(arithmetic(IntProvider.Operator.FLOOR_DIV, -7, 2).sample(context)).isEqualTo(-4);
    assertThat(arithmetic(IntProvider.Operator.FLOOR_MOD, -7, 2).sample(context)).isEqualTo(1);
  }

  @Test
  void provisionalArithmeticEvaluatesItsOperandsInOrderOfTheTree() {
    // spec: LT-R50, LT-R66 (draws follow the order of the tree)
    ScriptedRandom random = ScriptedRandom.of(0.0, 0.999);
    IntProvider tree =
        new IntProvider.Arithmetic(IntProvider.Operator.SUB, List.of(uniform(0, 9), uniform(0, 9)));
    assertThat(tree.sample(context(random))).isEqualTo(0 - 9);
    assertThat(random.steps()).isEqualTo(2);
  }

  @Test
  void provisionalAConditionalPicksItsBranchFromTheCondition() {
    // spec: LT-R50 (conditional)
    IntProvider conditional =
        new IntProvider.Conditional(new LootCondition.KilledByPlayer(), c(10), c(20));
    assertThat(conditional.sample(withKiller(ScriptedRandom.of()))).isEqualTo(10);
    assertThat(conditional.sample(context(ScriptedRandom.of()))).isEqualTo(20);
  }

  @Test
  void provisionalANumberDispatcherTakesTheFirstCaseThatHolds() {
    // spec: LT-R50 (number_dispatcher: first matching case, else the default)
    IntProvider dispatcher =
        new IntProvider.NumberDispatcher(
            List.of(
                new IntProvider.NumberDispatcher.Case(new LootCondition.AnyOf(List.of()), c(1)),
                new IntProvider.NumberDispatcher.Case(new LootCondition.KilledByPlayer(), c(2)),
                new IntProvider.NumberDispatcher.Case(new LootCondition.AllOf(List.of()), c(3))),
            c(4));
    assertThat(dispatcher.sample(withKiller(ScriptedRandom.of()))).isEqualTo(2);
    assertThat(dispatcher.sample(context(ScriptedRandom.of()))).isEqualTo(3);
    IntProvider noMatch =
        new IntProvider.NumberDispatcher(
            List.of(
                new IntProvider.NumberDispatcher.Case(new LootCondition.AnyOf(List.of()), c(1))),
            c(4));
    assertThat(noMatch.sample(context(ScriptedRandom.of()))).isEqualTo(4);
  }

  @Test
  void provisionalAWeightedListFollowsItsWeights() {
    // spec: LT-R50 (weighted_list: one weighted draw)
    IntProvider list =
        new IntProvider.WeightedList(
            List.of(
                new WeightedEntry<IntProvider>(c(10), 1),
                new WeightedEntry<IntProvider>(c(20), 3)));
    RandomSource random = Lcg48Random.ofSeed(31);
    int twenties = 0;
    int samples = 4000;
    for (int i = 0; i < samples; i++) {
      int value = list.sample(context(random));
      assertThat(value).isIn(10, 20);
      if (value == 20) {
        twenties++;
      }
    }
    assertThat(twenties / (double) samples).isBetween(0.70, 0.80);
  }

  @Test
  void aReferenceEvaluatesTheNamedProviderOfTheLoadedData() {
    // spec: LT-R50 (named providers, e.g. cooking/time_coal)
    LootContext context =
        LootContext.builder(
                Ctx.data(
                    Map.of(),
                    Ctx.standardPredicates(),
                    Ctx.providers(Map.of("cooking/time_coal", c(1600)), Map.of())),
                Ctx.ANY,
                ScriptedRandom.of())
            .build();
    assertThat(new IntProvider.Reference(Identifier.minecraft("cooking/time_coal")).sample(context))
        .isEqualTo(1600);
  }

  @Test
  void aReferenceToAProviderThatIsNotLoadedIsAnIllegalState() {
    // spec: IntProvider#sample contract (IllegalStateException)
    LootContext context = context(ScriptedRandom.of());
    IntProvider missing = new IntProvider.Reference(Identifier.minecraft("no/such_provider"));
    assertThatThrownBy(() -> missing.sample(context)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void provisionalAFloatProviderReadAsAnIntegerKeepsAnExactInteger() {
    // spec: LT-R50 (from_float); the rounding of fractions is open
    assertThat(
            new IntProvider.FromFloat(new FloatProvider.Constant(3.0f))
                .sample(context(ScriptedRandom.of())))
        .isEqualTo(3);
  }
}
