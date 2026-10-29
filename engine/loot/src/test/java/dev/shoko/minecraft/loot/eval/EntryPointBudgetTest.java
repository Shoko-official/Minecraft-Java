package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.reference.BrewingFuelRef;
import dev.shoko.minecraft.item.reference.CompostRef;
import dev.shoko.minecraft.item.reference.FuelRef;
import dev.shoko.minecraft.loot.LootBudgetException;
import dev.shoko.minecraft.loot.assembly.LootLoadResult;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.enchant.effect.ConditionalValueEffect;
import dev.shoko.minecraft.loot.enchant.effect.EffectComponentValue;
import dev.shoko.minecraft.loot.enchant.effect.EffectComponents;
import dev.shoko.minecraft.loot.enchant.effect.EffectEvaluator;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.enchant.effect.ValueEffect;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.number.MachineProviders;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.NamedPredicates;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Blocks;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.NamedDags;
import dev.shoko.minecraft.loot.testing.Tables;
import dev.shoko.minecraft.loot.testing.WatchedRandom;
import dev.shoko.minecraft.world.block.BlockState;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: LT-R74f (S-arch): EVERY entry point that evaluates conditions or number providers bounds
// its work, not only the draws of a table: a provider or predicate sampled directly with a context
// built by the caller, the providers of the machines (fuel, cooking, compost, brewing) and the
// effects of the enchantments. A DAG without a cycle where element i references element i + 1
// twice makes 2^levels evaluations; past 1000000 nodes the call fails fast with the typed error
// (LT-R74g). The budget is per call of an entry point and not per context: a context built once
// serves any number of small calls. The leaves draw numbers, so that WatchedRandom ends a test
// whose code does not stop; the duration check is the same bound as the coordinator's, 2 s.
@Timeout(60)
class EntryPointBudgetTest {

  private static final long WORK_LIMIT = 8_000_000L;
  private static final long TWO_SECONDS = 2_000_000_000L;
  private static final Identifier Q0 = Identifier.minecraft("q0");
  private static final Identifier F0 = Identifier.minecraft("f0");
  private static final Identifier P0 = Identifier.minecraft("p0");
  private static final BlockState MACHINE = Blocks.plain("furnace", 10, 100);

  private static WatchedRandom watched() {
    return WatchedRandom.limited(Lcg48Random.ofSeed(1), WORK_LIMIT);
  }

  private static LootLoadResult dags() {
    Map<String, String> files =
        NamedDags.providerDag(40, "add", NamedDags.INT_LEAF, "context_int_provider", "q");
    files.putAll(
        NamedDags.providerDag(40, "add", NamedDags.FLOAT_LEAF, "context_float_provider", "f"));
    files.putAll(NamedDags.predicateDag(40, "any_of", NamedDags.FALSE_LEAF, "p"));
    LootLoadResult loaded = NamedDags.load(files);
    assertThat(loaded.data().numbers().intProvider(Q0)).isPresent();
    assertThat(loaded.data().numbers().floatProvider(F0)).isPresent();
    assertThat(loaded.data().predicates().find(P0)).isPresent();
    return loaded;
  }

  private static LootContext context(LootLoadResult loaded, WatchedRandom random) {
    return LootContext.builder(loaded.data(), Ctx.ANY, random).build();
  }

  private static void assertBudgetError(Runnable call) {
    long start = System.nanoTime();
    assertThatThrownBy(call::run).isInstanceOf(LootBudgetException.class);
    assertThat(System.nanoTime() - start).isLessThan(TWO_SECONDS);
  }

  @Test
  void provisionalANamedIntegerProviderDagSampledDirectlyEndsInABudgetError() {
    // spec: LT-R74f (a direct call with a context built by the caller)
    LootLoadResult loaded = dags();
    IntProvider provider = loaded.data().numbers().intProvider(Q0).orElseThrow();
    LootContext context = context(loaded, watched());
    assertBudgetError(() -> provider.sample(context));
  }

  @Test
  void provisionalANamedFloatProviderDagSampledDirectlyEndsInABudgetError() {
    // spec: LT-R74f
    LootLoadResult loaded = dags();
    FloatProvider provider = loaded.data().numbers().floatProvider(F0).orElseThrow();
    LootContext context = context(loaded, watched());
    assertBudgetError(() -> provider.sample(context));
  }

  @Test
  void provisionalANamedPredicateDagTestedDirectlyEndsInABudgetError() {
    // spec: LT-R74f
    LootLoadResult loaded = dags();
    LootCondition condition = loaded.data().predicates().find(P0).orElseThrow();
    LootContext context = context(loaded, watched());
    assertBudgetError(() -> condition.test(context));
  }

  @Test
  void provisionalASharedInstanceProviderDagWithoutNamesEndsInABudgetError() {
    // spec: LT-R74f: the DAG made of one record used twice has no named reference at all
    IntProvider provider =
        new IntProvider.Uniform(new IntProvider.Constant(0), new IntProvider.Constant(1));
    for (int level = 0; level < 40; level++) {
      provider = new IntProvider.Arithmetic(IntProvider.Operator.ADD, List.of(provider, provider));
    }
    IntProvider root = provider;
    LootContext context = Ctx.context(watched());
    assertBudgetError(() -> root.sample(context));
  }

  @Test
  void provisionalASharedInstanceConditionDagWithoutNamesEndsInABudgetError() {
    // spec: LT-R74f
    LootCondition condition = new LootCondition.RandomChance(new FloatProvider.Constant(0f));
    for (int level = 0; level < 40; level++) {
      condition = new LootCondition.AnyOf(List.of(condition, condition));
    }
    LootCondition root = condition;
    LootContext context = Ctx.context(watched());
    assertBudgetError(() -> root.test(context));
  }

  @Test
  void provisionalTheBurnTimeOfAFuelWithADagProviderEndsInABudgetError() {
    // spec: LT-R74f (MachineProviders, fuel burn time)
    LootLoadResult loaded = dags();
    FuelRef fuel = new FuelRef(Q0, F0);
    assertBudgetError(() -> MachineProviders.burnTime(loaded.data(), fuel, MACHINE, watched()));
  }

  @Test
  void provisionalTheCookingSpeedOfAFuelWithADagProviderEndsInABudgetError() {
    // spec: LT-R74f (MachineProviders, cooking)
    LootLoadResult loaded = dags();
    FuelRef fuel = new FuelRef(Q0, F0);
    assertBudgetError(() -> MachineProviders.cookingSpeed(loaded.data(), fuel, MACHINE, watched()));
  }

  @Test
  void provisionalTheCompostLayersWithADagProviderEndsInABudgetError() {
    // spec: LT-R74f (MachineProviders, composting)
    LootLoadResult loaded = dags();
    CompostRef compost = new CompostRef(Q0);
    assertBudgetError(
        () -> MachineProviders.compostLayers(loaded.data(), compost, MACHINE, watched()));
  }

  @Test
  void provisionalTheBrewingSpeedWithADagProviderEndsInABudgetError() {
    // spec: LT-R74f (MachineProviders, brewing speed)
    LootLoadResult loaded = dags();
    BrewingFuelRef fuel = new BrewingFuelRef(F0, Q0);
    assertBudgetError(() -> MachineProviders.brewingSpeed(loaded.data(), fuel, MACHINE, watched()));
  }

  @Test
  void provisionalTheBrewingUsesWithADagProviderEndsInABudgetError() {
    // spec: LT-R74f (MachineProviders, brewing uses)
    LootLoadResult loaded = dags();
    BrewingFuelRef fuel = new BrewingFuelRef(F0, Q0);
    assertBudgetError(() -> MachineProviders.brewingUses(loaded.data(), fuel, MACHINE, watched()));
  }

  private static final Identifier COMPONENT = Identifier.minecraft("damage");

  private static EffectComponents addWhen(ValueEffect effect, LootCondition... requirements) {
    SequencedMap<Identifier, EffectComponentValue> map = new LinkedHashMap<>();
    map.put(
        COMPONENT,
        new EffectComponentValue.ValueEffects(
            List.of(new ConditionalValueEffect(effect, List.of(requirements)))));
    return new EffectComponents(map);
  }

  @Test
  void provisionalAnEnchantmentEffectWhoseRequirementIsANamedPredicateDagEndsInABudgetError() {
    // spec: LT-R74f (EffectEvaluator.value with requirements that reference named predicates)
    LootLoadResult loaded = dags();
    EffectComponents effects =
        addWhen(
            new ValueEffect.Add(new LevelBasedValue.Constant(1f)), new LootCondition.Reference(P0));
    LootContext context = context(loaded, watched());
    assertBudgetError(() -> EffectEvaluator.value(effects, COMPONENT, 1, 0f, context));
  }

  @Test
  void provisionalAnEnchantmentEffectWhoseRequirementIsASharedConditionDagEndsInABudgetError() {
    // spec: LT-R74f (the requirements of an effect are conditions like any other)
    LootCondition condition = new LootCondition.RandomChance(new FloatProvider.Constant(0f));
    for (int level = 0; level < 40; level++) {
      condition = new LootCondition.AnyOf(List.of(condition, condition));
    }
    EffectComponents effects =
        addWhen(new ValueEffect.Add(new LevelBasedValue.Constant(1f)), condition);
    LootContext context = Ctx.context(watched());
    assertBudgetError(() -> EffectEvaluator.value(effects, COMPONENT, 1, 0f, context));
  }

  @Test
  void provisionalAnEnchantmentLevelBasedValueDagEndsInABudgetError() {
    // spec: LT-R74f: a value that uses the same sub-value twice, 26 levels (67 million evaluations
    // in about 1.5 s without a budget; the evaluation draws nothing, so the level is kept short)
    LevelBasedValue value = new LevelBasedValue.Constant(1f);
    for (int level = 0; level < 26; level++) {
      value = new LevelBasedValue.Fraction(value, value);
    }
    EffectComponents effects = addWhen(new ValueEffect.Add(value));
    LootContext context = Ctx.context(watched());
    assertBudgetError(() -> EffectEvaluator.value(effects, COMPONENT, 1, 0f, context));
  }

  @Test
  void provisionalTheBudgetIsPerCallAndNotPerContextForProviders() {
    // spec: LT-R74f: 150000 calls of a provider of 10 nodes (1.5 million in all) with one context
    IntProvider ten =
        new IntProvider.Arithmetic(
            IntProvider.Operator.ADD,
            Collections.nCopies(9, (IntProvider) new IntProvider.Constant(1)));
    FloatProvider floatTen =
        new FloatProvider.Arithmetic(
            FloatProvider.Operator.ADD,
            Collections.nCopies(9, (FloatProvider) new FloatProvider.Constant(1f)));
    LootContext context = Ctx.context(Lcg48Random.ofSeed(1));
    assertThatCode(
            () -> {
              for (int call = 0; call < 150_000; call++) {
                ten.sample(context);
                floatTen.sample(context);
              }
            })
        .doesNotThrowAnyException();
  }

  @Test
  void provisionalTheBudgetIsPerCallAndNotPerContextForConditions() {
    // spec: LT-R74f
    LootCondition leaf = new LootCondition.AllOf(List.of());
    LootCondition ten = new LootCondition.AllOf(Collections.nCopies(9, leaf));
    LootContext context = Ctx.context(Lcg48Random.ofSeed(1));
    assertThatCode(
            () -> {
              for (int call = 0; call < 150_000; call++) {
                assertThat(ten.test(context).passed()).isTrue();
              }
            })
        .doesNotThrowAnyException();
  }

  @Test
  void provisionalTheBudgetIsPerDrawAndNotPerContextForTables() {
    // spec: LT-R74f: 300 draws of a table of 5000 conditions (1.5 million nodes in all)
    LootCondition leaf = new LootCondition.AllOf(List.of());
    LootTable table =
        Tables.table(
            Tables.poolWith(
                Collections.nCopies(5000, leaf),
                new IntProvider.Constant(1),
                0f,
                List.of(),
                Tables.item("stone")));
    LootContext context = Ctx.context(Lcg48Random.ofSeed(1));
    assertThatCode(
            () -> {
              for (int draw = 0; draw < 300; draw++) {
                assertThat(LootEvaluator.evaluate(table, context).stacks()).hasSize(1);
              }
            })
        .doesNotThrowAnyException();
  }

  @Test
  void provisionalTwoSuccessiveBudgetErrorsOfTheSameContextAreIndependent() {
    // spec: LT-R74f: a failed call does not poison the context: a small call that follows works
    LootLoadResult loaded = dags();
    IntProvider dag = loaded.data().numbers().intProvider(Q0).orElseThrow();
    LootContext context = context(loaded, watched());
    assertBudgetError(() -> dag.sample(context));
    assertThat(new IntProvider.Constant(7).sample(context)).isEqualTo(7);
  }

  private static LootContext chainContext(int length) {
    Map<String, IntProvider> ints = new LinkedHashMap<>();
    SequencedMap<Identifier, LootCondition> named = new LinkedHashMap<>();
    for (int i = 0; i < length; i++) {
      boolean last = i == length - 1;
      ints.put(
          "c" + i,
          last
              ? new IntProvider.Constant(9)
              : new IntProvider.Reference(Identifier.minecraft("c" + (i + 1))));
      named.put(
          Identifier.minecraft("p" + i),
          last
              ? new LootCondition.AllOf(List.of())
              : new LootCondition.Reference(Identifier.minecraft("p" + (i + 1))));
    }
    return LootContext.builder(
            Ctx.data(Map.of(), new NamedPredicates(named), Ctx.providers(ints, Map.of())),
            Ctx.ANY,
            Lcg48Random.ofSeed(1))
        .build();
  }

  @Test
  void provisionalAChainOfSixtyFourReferencesIsFollowedAtEvaluation() {
    // spec: LT-R74d (a chain of 64 links is within the bound, also for data not checked at load)
    LootContext context = chainContext(64);
    assertThat(new IntProvider.Reference(Identifier.minecraft("c0")).sample(context)).isEqualTo(9);
    assertThat(new LootCondition.Reference(Identifier.minecraft("p0")).test(context).passed())
        .isTrue();
  }

  @Test
  void provisionalAChainOfSixtyFiveReferencesFailsAtEvaluationWithTheTypedError() {
    // spec: LT-R74d (the 65th link, "comme à l'évaluation"), LT-R74g: not a silent false
    LootContext context = chainContext(65);
    assertThatThrownBy(() -> new IntProvider.Reference(Identifier.minecraft("c0")).sample(context))
        .isInstanceOf(LootBudgetException.class);
    assertThatThrownBy(() -> new LootCondition.Reference(Identifier.minecraft("p0")).test(context))
        .isInstanceOf(LootBudgetException.class);
  }
}
