package dev.shoko.minecraft.loot.hunt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootFinding;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.number.NumberProviderCodec;
import dev.shoko.minecraft.loot.number.NumberProviderSet;
import dev.shoko.minecraft.loot.testing.Ctx;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

// Adversarial tests on number providers and their references.
class NumbersHuntTest {

  private static NumberProviderSet ints(String name, IntProvider provider) {
    var ints = new LinkedHashMap<Identifier, IntProvider>();
    ints.put(Identifier.minecraft(name), provider);
    return new NumberProviderSet(ints, new LinkedHashMap<>());
  }

  // spec: LT-R50 / module contract: "provider not loaded" must be a load finding, not an
  // IllegalStateException when the provider is sampled.
  @Test
  void provisionalAnUnresolvedReferenceInsideFromFloatIsReportedAtLoad() {
    NumberProviderSet set =
        ints(
            "a",
            new IntProvider.FromFloat(
                new FloatProvider.Reference(Identifier.minecraft("missing_float"))));
    List<LootFinding> unresolved = NumberProviderCodec.unresolvedReferences(set);
    assertThat(unresolved).isNotEmpty();
  }

  @Test
  void provisionalAnUnresolvedReferenceInsideTheProbabilityOfABinomialIsReportedAtLoad() {
    NumberProviderSet set =
        ints(
            "a",
            new IntProvider.Binomial(
                new IntProvider.Constant(3),
                new FloatProvider.Reference(Identifier.minecraft("missing_float"))));
    assertThat(NumberProviderCodec.unresolvedReferences(set)).isNotEmpty();
  }

  @Test
  void provisionalAnUnresolvedReferenceInsideFromIntIsReportedAtLoad() {
    var floats = new LinkedHashMap<Identifier, FloatProvider>();
    floats.put(
        Identifier.minecraft("f"),
        new FloatProvider.FromInt(new IntProvider.Reference(Identifier.minecraft("missing_int"))));
    NumberProviderSet set = new NumberProviderSet(new LinkedHashMap<>(), floats);
    assertThat(NumberProviderCodec.unresolvedReferences(set)).isNotEmpty();
  }

  // spec: LT-R50: arithmetic never throws, whatever the operands.
  @Test
  void provisionalIntegerArithmeticNeverThrowsOnItsBoundaries() {
    int[] values = {Integer.MIN_VALUE, -1, 0, 1, 2, Integer.MAX_VALUE};
    LootContext context = Ctx.context(Lcg48Random.ofSeed(1));
    for (IntProvider.Operator operator : IntProvider.Operator.values()) {
      for (int a : values) {
        for (int b : values) {
          IntProvider provider =
              new IntProvider.Arithmetic(
                  operator, List.of(new IntProvider.Constant(a), new IntProvider.Constant(b)));
          assertThatCode(() -> provider.sample(context)).doesNotThrowAnyException();
        }
      }
    }
  }

  @Test
  void provisionalFloatArithmeticNeverThrowsAndIntConversionSaturates() {
    float[] values = {Float.NaN, Float.NEGATIVE_INFINITY, -1f, 0f, 1f, Float.MAX_VALUE};
    LootContext context = Ctx.context(Lcg48Random.ofSeed(1));
    for (FloatProvider.Operator operator : FloatProvider.Operator.values()) {
      for (float a : values) {
        for (float b : values) {
          FloatProvider provider =
              new FloatProvider.Arithmetic(
                  operator, List.of(new FloatProvider.Constant(a), new FloatProvider.Constant(b)));
          assertThatCode(() -> new IntProvider.FromFloat(provider).sample(context))
              .doesNotThrowAnyException();
        }
      }
    }
  }

  @Test
  void provisionalAUniformFullRangeStaysInsideTheIntRange() {
    LootContext context = Ctx.context(Lcg48Random.ofSeed(77));
    IntProvider full =
        new IntProvider.Uniform(
            new IntProvider.Constant(Integer.MIN_VALUE),
            new IntProvider.Constant(Integer.MAX_VALUE));
    for (int i = 0; i < 10_000; i++) {
      full.sample(context);
    }
    IntProvider degenerate =
        new IntProvider.Uniform(new IntProvider.Constant(5), new IntProvider.Constant(-5));
    assertThat(degenerate.sample(context)).isEqualTo(5);
  }
}
