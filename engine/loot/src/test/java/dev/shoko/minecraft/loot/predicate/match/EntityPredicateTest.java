package dev.shoko.minecraft.loot.predicate.match;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.loot.context.DamageSourceData;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.port.EntityFlag;
import dev.shoko.minecraft.loot.port.EntityView;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.port.TypeSpecific;
import dev.shoko.minecraft.loot.predicate.match.EntityPredicate.Check;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Sets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: LT-R57a (keys of the entity predicate), LT-R57b (all keys must hold), LT-R60 (damage
// source)
class EntityPredicateTest {

  private static LootContext context() {
    return Ctx.context(ScriptedRandom.of());
  }

  private static boolean matches(EntityView entity, Check... checks) {
    return new EntityPredicate(List.of(checks)).matches(entity, context());
  }

  private static Check flag(EntityFlag flag, boolean expected) {
    SequencedMap<EntityFlag, Boolean> flags = new LinkedHashMap<>();
    flags.put(flag, expected);
    return new Check.Flags(flags);
  }

  private static Check mainhand(ItemMatch item) {
    SequencedMap<EquipmentSlot, ItemMatch> slots = new LinkedHashMap<>();
    slots.put(EquipmentSlot.MAINHAND, item);
    return new Check.Equipment(slots);
  }

  private static ItemMatch shears() {
    return new ItemMatch(
        Sets.items("shears"), Sets.anyNumber(), new LinkedHashMap<>(), new LinkedHashMap<>());
  }

  @Test
  void provisionalTheEmptyPredicateMatchesEveryEntity() {
    // spec: LT-R57a/LT-R57b (EntityPredicate contract)
    assertThat(matches(FakeEntity.of("cow"))).isTrue();
    assertThat(matches(FakeEntity.of("zombie"))).isTrue();
  }

  @Test
  void provisionalEntityTypeAcceptsAnIdentifierOrATag() {
    // spec: LT-R57a (entity_type: 25 usages, tags included)
    assertThat(matches(FakeEntity.of("cow"), new Check.EntityType(Sets.entityTypes("cow"))))
        .isTrue();
    assertThat(matches(FakeEntity.of("zombie"), new Check.EntityType(Sets.entityTypes("cow"))))
        .isFalse();
    assertThat(matches(FakeEntity.of("zombie"), new Check.EntityType(Sets.entityTypeTag("undead"))))
        .isTrue();
    assertThat(matches(FakeEntity.of("cow"), new Check.EntityType(Sets.entityTypeTag("undead"))))
        .isFalse();
  }

  @Test
  void provisionalFlagsTestTheExpectedValueOfEachFlag() {
    // spec: LT-R57a (flags: is_on_fire, is_on_ground, is_flying)
    FakeEntity burning = FakeEntity.of("cow").flag(EntityFlag.ON_FIRE);
    FakeEntity calm = FakeEntity.of("cow");
    assertThat(matches(burning, flag(EntityFlag.ON_FIRE, true))).isTrue();
    assertThat(matches(calm, flag(EntityFlag.ON_FIRE, true))).isFalse();
    assertThat(matches(calm, flag(EntityFlag.ON_FIRE, false))).isTrue();
    assertThat(matches(burning, flag(EntityFlag.ON_FIRE, false))).isFalse();
  }

  @Test
  void provisionalEquipmentTestsTheStackOfTheSlot() {
    // spec: LT-R57a (equipment: slot -> item predicate)
    FakeEntity holding =
        FakeEntity.of("player").wearing(EquipmentSlot.MAINHAND, LootItems.stack("shears"));
    FakeEntity empty = FakeEntity.of("player");
    FakeEntity wrongSlot =
        FakeEntity.of("player").wearing(EquipmentSlot.OFFHAND, LootItems.stack("shears"));
    assertThat(matches(holding, mainhand(shears()))).isTrue();
    assertThat(matches(empty, mainhand(shears()))).isFalse();
    assertThat(matches(wrongSlot, mainhand(shears()))).isFalse();
  }

  @Test
  void provisionalVehicleAppliesItsPredicateToTheMount() {
    // spec: LT-R57a (vehicle: 6 usages)
    Check onPillager =
        new Check.Vehicle(
            new EntityPredicate(List.of(new Check.EntityType(Sets.entityTypes("pillager")))));
    FakeEntity riding = FakeEntity.of("zombie").riding(FakeEntity.of("pillager"));
    FakeEntity walking = FakeEntity.of("zombie");
    assertThat(matches(riding, onPillager)).isTrue();
    assertThat(matches(walking, onPillager)).isFalse();
  }

  @Test
  void provisionalMovementTestsTheHorizontalSpeedAndTheFallDistance() {
    // spec: LT-R57a, EN-R30 (movement: horizontal_speed, fall_distance)
    Check fast = new Check.Movement(Sets.atLeast(0.2), Sets.anyNumber());
    Check falling = new Check.Movement(Sets.anyNumber(), Sets.atLeast(3));
    assertThat(matches(FakeEntity.of("zombie").moving(0.3, 0), fast)).isTrue();
    assertThat(matches(FakeEntity.of("zombie").moving(0.1, 0), fast)).isFalse();
    assertThat(matches(FakeEntity.of("zombie").moving(0, 5), falling)).isTrue();
    assertThat(matches(FakeEntity.of("zombie").moving(0, 1), falling)).isFalse();
  }

  @Test
  void provisionalPeriodicTickMatchesMultiplesOfThePeriod() {
    // spec: LT-R57a, EN-R30 (periodic_tick: an integer)
    Check every20 = new Check.PeriodicTick(20);
    assertThat(matches(FakeEntity.of("zombie").ticks(40), every20)).isTrue();
    assertThat(matches(FakeEntity.of("zombie").ticks(41), every20)).isFalse();
  }

  @Test
  void provisionalASheepPredicateReadsTheShearingState() {
    // spec: LT-R57a (type_specific/sheep: sheared), TypeSpecific contract
    FakeEntity shorn = FakeEntity.of("sheep").specific(new TypeSpecific.Sheep(true));
    FakeEntity woolly = FakeEntity.of("sheep").specific(new TypeSpecific.Sheep(false));
    assertThat(matches(shorn, new Check.Sheep(Tristate.TRUE))).isTrue();
    assertThat(matches(woolly, new Check.Sheep(Tristate.TRUE))).isFalse();
    assertThat(matches(woolly, new Check.Sheep(Tristate.FALSE))).isTrue();
  }

  @Test
  void provisionalAKindSpecificPredicateIsFalseOnAnotherKind() {
    // spec: TypeSpecific contract (an entity is of one kind only)
    assertThat(matches(FakeEntity.of("zombie"), new Check.Sheep(Tristate.TRUE))).isFalse();
    FakeEntity slime = FakeEntity.of("slime").specific(new TypeSpecific.CubeMob(2));
    assertThat(matches(slime, new Check.Sheep(Tristate.TRUE))).isFalse();
  }

  @Test
  void provisionalACubeMobPredicateReadsTheSize() {
    // spec: LT-R57a (type_specific/cube_mob: size)
    FakeEntity slime = FakeEntity.of("slime").specific(new TypeSpecific.CubeMob(2));
    assertThat(matches(slime, new Check.CubeMob(Sets.atLeast(2)))).isTrue();
    assertThat(matches(slime, new Check.CubeMob(Sets.atLeast(3)))).isFalse();
  }

  @Test
  void provisionalARaiderPredicateReadsTheCaptainState() {
    // spec: LT-R57a (type_specific/raider)
    FakeEntity captain = FakeEntity.of("pillager").specific(new TypeSpecific.Raider(true));
    assertThat(matches(captain, new Check.Raider(Tristate.TRUE))).isTrue();
    assertThat(matches(captain, new Check.Raider(Tristate.FALSE))).isFalse();
  }

  @Test
  void provisionalAFishingHookPredicateReadsOpenWater() {
    // spec: LT-R57a, LT-R27 (type_specific/fishing_hook: in_open_water)
    FakeEntity open = FakeEntity.of("fishing_bobber").specific(new TypeSpecific.FishingHook(true));
    FakeEntity shallow =
        FakeEntity.of("fishing_bobber").specific(new TypeSpecific.FishingHook(false));
    assertThat(matches(open, new Check.FishingHook(Tristate.TRUE))).isTrue();
    assertThat(matches(shallow, new Check.FishingHook(Tristate.TRUE))).isFalse();
  }

  @Test
  void provisionalComponentsCompareTheExpectedValue() {
    // spec: LT-R57a ({"minecraft:sheep/color": "white"})
    SequencedMap<Identifier, JsonNode> expected = new LinkedHashMap<>();
    expected.put(Identifier.parse("minecraft:sheep/color"), Json.parse("'white'"));
    Check white = new Check.Components(expected);
    FakeEntity whiteSheep =
        FakeEntity.of("sheep")
            .component("minecraft:sheep/color", new ComponentValue(new StringTag("white")));
    FakeEntity blackSheep =
        FakeEntity.of("sheep")
            .component("minecraft:sheep/color", new ComponentValue(new StringTag("black")));
    assertThat(matches(whiteSheep, white)).isTrue();
    assertThat(matches(blackSheep, white)).isFalse();
    assertThat(matches(FakeEntity.of("sheep"), white)).isFalse();
  }

  @Test
  void provisionalEveryKeyMustHold() {
    // spec: LT-R57b (ET)
    Check cow = new Check.EntityType(Sets.entityTypes("cow"));
    Check burning = flag(EntityFlag.ON_FIRE, true);
    assertThat(matches(FakeEntity.of("cow").flag(EntityFlag.ON_FIRE), cow, burning)).isTrue();
    assertThat(matches(FakeEntity.of("cow"), cow, burning)).isFalse();
    assertThat(matches(FakeEntity.of("zombie").flag(EntityFlag.ON_FIRE), cow, burning)).isFalse();
  }

  @Test
  void provisionalAKeyNoDataUsesMatchesNothing() {
    // spec: EntityPredicate.Check.Other contract (kept as read, matches nothing)
    Check team = new Check.Other(Identifier.parse("minecraft:team"), Json.parse("'red'"));
    assertThat(matches(FakeEntity.of("zombie"), team)).isFalse();
  }

  @Test
  void provisionalADamageSourcePredicateChecksTheDirectness() {
    // spec: LT-R60 (is_direct)
    DamageSourcePredicate direct =
        new DamageSourcePredicate(List.of(new DamageSourcePredicate.Requirement.IsDirect(true)));
    DamageSourceData melee = new DamageSourceData(Identifier.minecraft("player_attack"), true);
    DamageSourceData arrow = new DamageSourceData(Identifier.minecraft("arrow"), false);
    assertThat(direct.matches(context(), melee)).isTrue();
    assertThat(direct.matches(context(), arrow)).isFalse();
  }

  @Test
  void provisionalADamageSourcePredicateChecksTheExpectedTagMembership() {
    // spec: LT-R60 (tags: id and expected)
    DamageSourcePredicate notProjectile =
        new DamageSourcePredicate(
            List.of(
                new DamageSourcePredicate.Requirement.Tags(
                    List.of(
                        new DamageSourcePredicate.TagExpectation(
                            Identifier.minecraft("is_projectile"), false)))));
    DamageSourceData melee = new DamageSourceData(Identifier.minecraft("player_attack"), true);
    DamageSourceData arrow = new DamageSourceData(Identifier.minecraft("arrow"), false);
    assertThat(notProjectile.matches(context(), melee)).isTrue();
    assertThat(notProjectile.matches(context(), arrow)).isFalse();
  }

  @Test
  void provisionalAnEmptyDamageSourcePredicateMatchesAnySource() {
    // spec: LT-R60 (all requirements must hold; none listed)
    DamageSourcePredicate any = new DamageSourcePredicate(List.of());
    assertThat(any.matches(context(), new DamageSourceData(Identifier.minecraft("arrow"), false)))
        .isTrue();
  }
}
