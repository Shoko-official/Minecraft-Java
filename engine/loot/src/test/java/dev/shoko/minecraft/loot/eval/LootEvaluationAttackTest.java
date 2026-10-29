package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.container.LootBinding;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootBudgetException;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.DamageSourceData;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.modifier.BonusFormula;
import dev.shoko.minecraft.loot.modifier.ModifierAction;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.match.EntityPredicate;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.table.LootTableCodec;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.SequenceStore;
import dev.shoko.minecraft.loot.testing.Sets;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: LT-R23 (no unbounded recursion), LT-R25 (weights bounded below by 0), LT-R49/LT-R54 (no
// empty stack, no stack above its maximum), LT-R67 (determinism) (adversarial): random contexts
// with
// odd luck, radius and parameters, cyclic and deeply nested tables, huge numbers of rolls.
@Timeout(120)
class LootEvaluationAttackTest {

  private static final long SEED = 20261029L;
  private static final float[] LUCKS = {
    0f,
    0.5f,
    -0.5f,
    1f,
    -1f,
    7f,
    -7f,
    100f,
    -100f,
    1e9f,
    -1e9f,
    Float.MAX_VALUE,
    -Float.MAX_VALUE,
    Float.POSITIVE_INFINITY,
    Float.NEGATIVE_INFINITY,
    Float.NaN
  };
  private static final float[] RADII = {
    0f, 1f, 4f, 0.1f, -1f, 1e30f, Float.NaN, Float.POSITIVE_INFINITY
  };

  private static Map<String, LootTable> tables() {
    Map<String, LootTable> map = new LinkedHashMap<>();
    LootTable stone =
        Tables.table(
            Tables.pool(
                1,
                Tables.alternatives(
                    Tables.when(
                        Tables.item("stone"),
                        new LootCondition.Reference(
                            Identifier.parse("minecraft:tool/can_silk_touch"))),
                    Tables.when(
                        Tables.item("cobblestone"), new LootCondition.SurvivesExplosion()))));
    map.put("blocks/stone", stone);
    map.put("a", Tables.table(Tables.pool(1, Tables.ref("b"), Tables.item("stone"))));
    map.put("b", Tables.table(Tables.pool(1, Tables.ref("a"), Tables.item("dirt"))));
    map.put("self", Tables.table(Tables.pool(2, Tables.ref("self"))));
    map.put("wild", wild());
    map.put(
        "nested", Tables.table(Tables.pool(2, Tables.ref("blocks/stone"), Tables.inline(wild()))));
    return map;
  }

  private static LootTable wild() {
    List<LootEntry> entries = new ArrayList<>();
    entries.add(
        Tables.modified(
            Tables.item("diamond", 3, 1),
            Tables.uniformCount(0, 5),
            Tables.modifier(
                new ModifierAction.ApplyBonus(
                    Identifier.minecraft("fortune"),
                    new BonusFormula.BinomialWithBonusCount(3, 0.5f))),
            Tables.modifier(new ModifierAction.ExplosionDecay())));
    entries.add(
        Tables.when(
            Tables.item("emerald", 2, -1),
            new LootCondition.KilledByPlayer(),
            Tables.chance(0.5f)));
    entries.add(Tables.empty(4));
    entries.add(Tables.tag("test_gems", true, 2));
    entries.add(
        Tables.when(
            Tables.item("beef"),
            new LootCondition.EntityProperties(
                EntityTarget.ATTACKER,
                new EntityPredicate(
                    List.of(new EntityPredicate.Check.EntityType(Sets.entityTypes("player")))))));
    LootEntry[] array = entries.toArray(new LootEntry[0]);
    return Tables.withModifiers(
        Tables.table(
            Tables.poolWith(
                Tables.conditions(Tables.chance(0.9f)),
                Tables.uniform(-2, 4),
                0.75f,
                List.of(Tables.limit(0, 9)),
                array)),
        Tables.setCount(70));
  }

  private static LootContext randomContext(Random random, long drawSeed) {
    LootContext.Builder builder =
        Ctx.builder(tables(), Lcg48Random.ofSeed(drawSeed))
            .luck(LUCKS[random.nextInt(LUCKS.length)]);
    if (random.nextBoolean()) {
      builder.explosionRadius(RADII[random.nextInt(RADII.length)]);
    }
    if (random.nextBoolean()) {
      Map<String, Integer> enchantments = new LinkedHashMap<>();
      enchantments.put("minecraft:fortune", random.nextInt(300));
      enchantments.put("minecraft:silk_touch", random.nextInt(2));
      builder.tool(LootItems.enchanted(LootItems.stack("iron_pickaxe"), enchantments));
    }
    if (random.nextBoolean()) {
      builder.entity(
          EntityTarget.ATTACKER,
          FakeEntity.of(random.nextBoolean() ? "player" : "zombie")
              .wearing(EquipmentSlot.MAINHAND, LootItems.stack("diamond_sword")));
    }
    if (random.nextBoolean()) {
      builder.entity(EntityTarget.ATTACKING_PLAYER, FakeEntity.of("player"));
    }
    if (random.nextBoolean()) {
      builder.damageSource(
          new DamageSourceData(Identifier.minecraft("arrow"), random.nextBoolean()));
    }
    return builder.build();
  }

  private static void assertValid(LootResult result) {
    for (ItemStack stack : result.stacks()) {
      assertThat(stack.isEmpty()).isFalse();
      assertThat(stack.count()).isBetween(1, stack.maxStackSize());
    }
  }

  @Test
  void randomContextsNeverBreakTheEvaluation() {
    // spec: LT-R49, LT-R54, LootResult contract
    Random random = new Random(SEED);
    for (int i = 0; i < 400; i++) {
      long drawSeed = random.nextLong();
      LootContext context = randomContext(random, drawSeed);
      for (String name : List.of("blocks/stone", "wild", "nested")) {
        assertValid(LootEvaluator.evaluate(Identifier.minecraft(name), context));
      }
    }
  }

  @Test
  void theSameContextAndTheSameSeedGiveTheSameResult() {
    // spec: LT-R67, determinism of the module
    for (int i = 0; i < 60; i++) {
      long contextSeed = SEED + i;
      long drawSeed = 1000L + i;
      LootResult first =
          LootEvaluator.evaluate(
              Identifier.minecraft("wild"), randomContext(new Random(contextSeed), drawSeed));
      LootResult second =
          LootEvaluator.evaluate(
              Identifier.minecraft("wild"), randomContext(new Random(contextSeed), drawSeed));
      assertThat(first.stacks()).isEqualTo(second.stacks());
    }
  }

  @Test
  void aCyclicPairOfTablesTerminates() {
    // spec: LT-R23
    LootContext context = Ctx.builder(tables(), Lcg48Random.ofSeed(1)).build();
    LootResult result = LootEvaluator.evaluate(Identifier.minecraft("a"), context);
    assertThat(result.stacks().size()).isLessThan(100_000);
    assertValid(result);
  }

  @Test
  void aTableThatReferencesItselfTerminates() {
    // spec: LT-R23
    LootContext context = Ctx.builder(tables(), Lcg48Random.ofSeed(1)).build();
    assertValid(LootEvaluator.evaluate(Identifier.minecraft("self"), context));
  }

  @Test
  void aDeeplyNestedInlineTableTerminates() {
    // spec: LT-R10, LT-R23
    LootTable table = Tables.table(Tables.pool(1, Tables.item("stone")));
    for (int depth = 0; depth < 150; depth++) {
      table = Tables.table(Tables.pool(1, Tables.inline(table)));
    }
    LootResult result = LootEvaluator.evaluate(table, Ctx.context(Lcg48Random.ofSeed(1)));
    assertThat(result.stacks()).hasSize(1);
  }

  @Test
  void aFullPoolOfRollsWithNothingToDrawIsCheap() {
    // spec: LT-R6, LT-R24 (empty), LT-R74a (4096 rolls are the cap and stay cheap), cas limite
    LootTable table = Tables.table(Tables.pool(4096, Tables.empty(1)));
    assertThat(LootEvaluator.evaluate(table, Ctx.context(Lcg48Random.ofSeed(1))).stacks())
        .isEmpty();
  }

  @Test
  void aHugeNumberOfRollsIsRefusedAtDecodingAndFailsTheDrawIfBuiltByHand() {
    // spec: LT-R74a, LT-R74g (a constant over the cap is a load error, VALUE_RANGE; an exceeded cap
    // at
    // evaluation fails the draw), cas limite
    String json =
        "{'type':'minecraft:chest','pools':[{'rolls':200000,"
            + "'entries':[{'type':'minecraft:empty'}]}]}";
    Decoded.failsWith(
        LootTableCodec.decode(Json.parse(json), EnchantFixtures.access(), LootItems.catalog()),
        LootFindingCode.VALUE_RANGE);
    LootTable table = Tables.table(Tables.pool(200_000, Tables.empty(1)));
    assertThatThrownBy(() -> LootEvaluator.evaluate(table, Ctx.context(Lcg48Random.ofSeed(1))))
        .isInstanceOf(LootBudgetException.class);
  }

  @Test
  void aHugeCountIsSplitIntoStacksOfTheMaximum() {
    // spec: LT-R54
    LootTable table =
        Tables.table(
            Tables.pool(1, Tables.modified(Tables.item("stone"), Tables.setCount(100_000))));
    LootResult result = LootEvaluator.evaluate(table, Ctx.context(ScriptedRandom.of()));
    assertValid(result);
    assertThat(result.stacks().stream().mapToInt(ItemStack::count).sum()).isEqualTo(100_000);
  }

  @Test
  void theEffectiveWeightIsNeverNegativeWhateverTheLuckAndTheQuality() {
    // spec: LT-R25 (borne a 0)
    int[] weights = {0, 1, 10, 85, Integer.MAX_VALUE};
    int[] qualities = {Integer.MIN_VALUE, -100, -1, 0, 1, 100, Integer.MAX_VALUE};
    for (int weight : weights) {
      for (int quality : qualities) {
        for (float luck : LUCKS) {
          assertThat(WeightMath.effectiveWeight(weight, quality, luck))
              .as("weight %d quality %d luck %s", weight, quality, luck)
              .isNotNegative();
        }
      }
    }
  }

  @Test
  void aSmallContainerNeverMakesTheUnpackingFail() {
    // spec: LT-R70
    LootTable three =
        Tables.named(
            "chests/odd",
            Tables.pool(1, Tables.item("stone")),
            Tables.pool(1, Tables.item("dirt")),
            Tables.pool(1, Tables.item("stick")));
    for (int size : new int[] {1, 2, 3, 27, 54}) {
      LootContext context =
          Ctx.builder(Map.of("chests/odd", three), Lcg48Random.ofSeed(size)).build();
      UnpackResult result =
          ContainerUnpacker.unpack(
              new LootBinding.Bound(Identifier.parse("minecraft:chests/odd"), true, 5L + size),
              size,
              context,
              new SequenceStore(1));
      assertThat(result.slots()).hasSize(size);
    }
  }

  @Test
  void aNegativeOrHugeSeedIsAValidSeed() {
    // spec: LT-R63 (only 0 means absent)
    for (long seed : new long[] {Long.MIN_VALUE, -1L, 1L, Long.MAX_VALUE}) {
      LootResult result =
          LootEvaluator.evaluateTopLevel(
              tables().get("wild"),
              Ctx.builder(tables(), Lcg48Random.ofSeed(1)).build(),
              seed,
              new SequenceStore(1));
      assertValid(result);
    }
  }

  @Test
  void aLargeUniformRangeStaysInsideItsBounds() {
    // spec: LT-R51
    IntProvider range =
        new IntProvider.Uniform(
            new IntProvider.Constant(-1_000_000_000), new IntProvider.Constant(1_000_000_000));
    LootContext context = Ctx.context(Lcg48Random.ofSeed(8));
    for (int i = 0; i < 1000; i++) {
      assertThat(range.sample(context)).isBetween(-1_000_000_000, 1_000_000_000);
    }
  }
}
