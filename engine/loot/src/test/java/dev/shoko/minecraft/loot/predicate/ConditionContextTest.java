package dev.shoko.minecraft.loot.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.context.DamageSourceData;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LevelFacts;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.match.BlockMatch;
import dev.shoko.minecraft.loot.predicate.match.DamageSourcePredicate;
import dev.shoko.minecraft.loot.predicate.match.EntityPredicate;
import dev.shoko.minecraft.loot.predicate.match.ItemMatch;
import dev.shoko.minecraft.loot.predicate.match.LocationPredicate;
import dev.shoko.minecraft.loot.predicate.match.Tristate;
import dev.shoko.minecraft.loot.testing.Blocks;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Sets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R15 (absent parameters), LT-R34 to LT-R37 (match_block, match_tool, entity_properties,
// location_check, damage_source_properties, killed_by_player, weather_check, value checks)
class ConditionContextTest {

  private static LootContext.Builder builder() {
    return Ctx.builder(ScriptedRandom.of());
  }

  private static ConditionResult test(LootCondition condition, LootContext context) {
    return condition.test(context);
  }

  private static LootCondition entityIsCow(EntityTarget target) {
    return new LootCondition.EntityProperties(
        target,
        new EntityPredicate(
            List.of(new EntityPredicate.Check.EntityType(Sets.entityTypes("cow")))));
  }

  private static LootCondition wheatAtAge(String age) {
    Map<String, String> state = new LinkedHashMap<>();
    state.put("age", age);
    return new LootCondition.MatchBlock(
        new BlockMatch(Sets.blocks("wheat"), new LinkedHashMap<>(state)));
  }

  private static LootCondition shearsTool() {
    return new LootCondition.MatchTool(
        new ItemMatch(
            Sets.items("shears"), Sets.anyNumber(), new LinkedHashMap<>(), new LinkedHashMap<>()));
  }

  @Test
  void provisionalKilledByPlayerHoldsWhenAnAttackingPlayerIsPresent() {
    // spec: LT-R35
    LootContext with =
        builder().entity(EntityTarget.ATTACKING_PLAYER, FakeEntity.of("player")).build();
    ConditionResult present = test(new LootCondition.KilledByPlayer(), with);
    assertThat(present.passed()).isTrue();
    assertThat(present.cause()).isEqualTo(ConditionCause.MATCHED);
  }

  @Test
  void provisionalKilledByPlayerFailsWithoutAnAttackingPlayer() {
    // spec: LT-R35, LT-R15
    ConditionResult absent = test(new LootCondition.KilledByPlayer(), builder().build());
    assertThat(absent.passed()).isFalse();
    assertThat(absent.cause()).isEqualTo(ConditionCause.PARAMETER_ABSENT);
  }

  @Test
  void provisionalMatchToolTestsTheToolStack() {
    // spec: LT-R35
    LootContext shears = builder().tool(LootItems.stack("shears")).build();
    LootContext pickaxe = builder().tool(LootItems.stack("iron_pickaxe")).build();
    assertThat(test(shearsTool(), shears).passed()).isTrue();
    ConditionResult other = test(shearsTool(), pickaxe);
    assertThat(other.passed()).isFalse();
    assertThat(other.cause()).isEqualTo(ConditionCause.NOT_MATCHED);
  }

  @Test
  void provisionalMatchToolFailsWithoutATool() {
    // spec: LT-R35, LT-R15
    ConditionResult result = test(shearsTool(), builder().build());
    assertThat(result.passed()).isFalse();
    assertThat(result.cause()).isEqualTo(ConditionCause.PARAMETER_ABSENT);
  }

  @Test
  void provisionalMatchBlockComparesTheStateExactly() {
    // spec: LT-R34 ({"blocks":"minecraft:wheat","state":{"age":"7"}})
    assertThat(test(wheatAtAge("7"), builder().blockState(Blocks.wheat(7)).build()).passed())
        .isTrue();
    assertThat(test(wheatAtAge("7"), builder().blockState(Blocks.wheat(6)).build()).passed())
        .isFalse();
    assertThat(test(wheatAtAge("7"), builder().blockState(Blocks.stone()).build()).passed())
        .isFalse();
  }

  @Test
  void provisionalMatchBlockFailsWithoutABlockState() {
    // spec: LT-R34, LT-R15
    ConditionResult result = test(wheatAtAge("7"), builder().build());
    assertThat(result.passed()).isFalse();
    assertThat(result.cause()).isEqualTo(ConditionCause.PARAMETER_ABSENT);
  }

  @Test
  void provisionalEntityPropertiesTestsTheChosenEntity() {
    // spec: LT-R35 (entity and predicate)
    LootContext cow = builder().entity(EntityTarget.THIS, FakeEntity.of("cow")).build();
    LootContext zombie = builder().entity(EntityTarget.THIS, FakeEntity.of("zombie")).build();
    assertThat(test(entityIsCow(EntityTarget.THIS), cow).passed()).isTrue();
    assertThat(test(entityIsCow(EntityTarget.THIS), zombie).passed()).isFalse();
  }

  @Test
  void provisionalEntityPropertiesLooksAtTheTargetItNames() {
    // spec: LT-R12, LT-R35 (this, attacker, direct_attacker are distinct parameters)
    LootContext context =
        builder()
            .entity(EntityTarget.THIS, FakeEntity.of("zombie"))
            .entity(EntityTarget.ATTACKER, FakeEntity.of("cow"))
            .build();
    assertThat(test(entityIsCow(EntityTarget.ATTACKER), context).passed()).isTrue();
    assertThat(test(entityIsCow(EntityTarget.THIS), context).passed()).isFalse();
    assertThat(test(entityIsCow(EntityTarget.DIRECT_ATTACKER), context).passed()).isFalse();
  }

  @Test
  void provisionalEntityPropertiesFailsWhenTheEntityIsAbsent() {
    // spec: LT-R35, LT-R15
    ConditionResult result = test(entityIsCow(EntityTarget.THIS), builder().build());
    assertThat(result.passed()).isFalse();
    assertThat(result.cause()).isEqualTo(ConditionCause.PARAMETER_ABSENT);
  }

  @Test
  void provisionalDamageSourcePropertiesTestsTheTagsOfTheDamageType() {
    // spec: LT-R35, LT-R60 (tags with an expected membership)
    LootCondition projectile =
        new LootCondition.DamageSourceProperties(
            new DamageSourcePredicate(
                List.of(
                    new DamageSourcePredicate.Requirement.Tags(
                        List.of(
                            new DamageSourcePredicate.TagExpectation(
                                Identifier.minecraft("is_projectile"), true))))));
    LootContext arrow =
        builder().damageSource(new DamageSourceData(Identifier.minecraft("arrow"), false)).build();
    LootContext melee =
        builder()
            .damageSource(new DamageSourceData(Identifier.minecraft("player_attack"), true))
            .build();
    assertThat(test(projectile, arrow).passed()).isTrue();
    assertThat(test(projectile, melee).passed()).isFalse();
  }

  @Test
  void provisionalDamageSourcePropertiesFailsWithoutADamageSource() {
    // spec: LT-R15
    LootCondition condition =
        new LootCondition.DamageSourceProperties(new DamageSourcePredicate(List.of()));
    ConditionResult result = test(condition, builder().build());
    assertThat(result.passed()).isFalse();
    assertThat(result.cause()).isEqualTo(ConditionCause.PARAMETER_ABSENT);
  }

  @Test
  void provisionalLocationCheckFailsWithoutAnOrigin() {
    // spec: LT-R35, LT-R15
    LootCondition condition =
        new LootCondition.LocationCheck(new LocationPredicate(List.of()), 0, 0, 0);
    ConditionResult result = test(condition, builder().build());
    assertThat(result.passed()).isFalse();
    assertThat(result.cause()).isEqualTo(ConditionCause.PARAMETER_ABSENT);
  }

  @Test
  void provisionalLocationCheckFailsWhenNoChunkIsAvailable() {
    // spec: LocationPredicate#matches (a chunk that is not available fails every requirement)
    LootCondition condition =
        new LootCondition.LocationCheck(
            new LocationPredicate(
                List.of(
                    new LocationPredicate.Requirement.Block(
                        new BlockMatch(Sets.blocks("stone"), new LinkedHashMap<>())))),
            0,
            -1,
            0);
    LootContext context = builder().origin(new Vec3(0.5, 64, 0.5)).build();
    assertThat(test(condition, context).passed()).isFalse();
  }

  @Test
  void provisionalEnchantmentActiveCheckComparesTheActiveFlag() {
    // spec: LT-R37, LT-R15
    LootContext active = builder().enchantment(1, true).build();
    LootContext inactive = builder().enchantment(1, false).build();
    assertThat(test(new LootCondition.EnchantmentActiveCheck(true), active).passed()).isTrue();
    assertThat(test(new LootCondition.EnchantmentActiveCheck(true), inactive).passed()).isFalse();
    assertThat(test(new LootCondition.EnchantmentActiveCheck(false), inactive).passed()).isTrue();
  }

  @Test
  void provisionalEnchantmentActiveCheckFailsOutsideAnEnchantmentContext() {
    // spec: LT-R15 (echoue hors contexte d'enchantement)
    ConditionResult result =
        test(new LootCondition.EnchantmentActiveCheck(true), builder().build());
    assertThat(result.passed()).isFalse();
    assertThat(result.cause()).isEqualTo(ConditionCause.PARAMETER_ABSENT);
  }

  @Test
  void provisionalWeatherCheckRainCountsAThunderstorm() {
    // spec: LT-R36 (raining: true succeeds on rain or thunderstorm)
    LootCondition rain = new LootCondition.WeatherCheck(Tristate.TRUE, Tristate.UNSET);
    LootContext raining = builder().level(new LevelFacts(true, false, 0, 0)).build();
    LootContext thunder = builder().level(new LevelFacts(true, true, 0, 0)).build();
    LootContext clear = builder().level(new LevelFacts(false, false, 0, 0)).build();
    assertThat(test(rain, raining).passed()).isTrue();
    assertThat(test(rain, thunder).passed()).isTrue();
    assertThat(test(rain, clear).passed()).isFalse();
  }

  @Test
  void provisionalWeatherCheckThunderNeedsAThunderstorm() {
    // spec: LT-R36 ({"thundering": true})
    LootCondition thunder = new LootCondition.WeatherCheck(Tristate.UNSET, Tristate.TRUE);
    LootContext raining = builder().level(new LevelFacts(true, false, 0, 0)).build();
    LootContext storm = builder().level(new LevelFacts(true, true, 0, 0)).build();
    assertThat(test(thunder, raining).passed()).isFalse();
    assertThat(test(thunder, storm).passed()).isTrue();
  }

  @Test
  void provisionalWeatherCheckCanRequireClearSky() {
    // spec: LT-R36
    LootCondition dry = new LootCondition.WeatherCheck(Tristate.FALSE, Tristate.UNSET);
    assertThat(test(dry, builder().level(new LevelFacts(false, false, 0, 0)).build()).passed())
        .isTrue();
    assertThat(test(dry, builder().level(new LevelFacts(true, false, 0, 0)).build()).passed())
        .isFalse();
  }

  @Test
  void provisionalWeatherCheckFailsWithoutLevelFacts() {
    // spec: LootContext#level (weather_check then fails), LT-R15
    LootCondition rain = new LootCondition.WeatherCheck(Tristate.UNSET, Tristate.UNSET);
    assertThat(test(rain, builder().build()).passed()).isFalse();
  }

  @Test
  void provisionalIntValueCheckTestsTheValueAgainstTheRange() {
    // spec: LT-R37 (value and test)
    LootCondition inside =
        new LootCondition.IntValueCheck(new IntProvider.Constant(5), Sets.between(3, 7));
    LootCondition outside =
        new LootCondition.IntValueCheck(new IntProvider.Constant(8), Sets.between(3, 7));
    assertThat(test(inside, builder().build()).passed()).isTrue();
    assertThat(test(outside, builder().build()).passed()).isFalse();
  }

  @Test
  void provisionalFloatValueCheckTestsTheValueAgainstTheRange() {
    // spec: LT-R37 (value and test)
    LootCondition inside =
        new LootCondition.FloatValueCheck(
            new FloatProvider.Constant(0.5f), Sets.between(0.25, 0.75));
    LootCondition below =
        new LootCondition.FloatValueCheck(
            new FloatProvider.Constant(0.1f), Sets.between(0.25, 0.75));
    assertThat(test(inside, builder().build()).passed()).isTrue();
    assertThat(test(below, builder().build()).passed()).isFalse();
  }
}
