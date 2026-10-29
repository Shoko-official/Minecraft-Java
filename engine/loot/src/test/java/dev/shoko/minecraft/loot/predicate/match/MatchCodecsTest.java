package dev.shoko.minecraft.loot.predicate.match;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.port.EntityFlag;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: LT-R34, LT-R57a, LT-R58, LT-R59, LT-R60 (JSON forms of the matchers)
class MatchCodecsTest {

  private static RegistryAccess access() {
    return EnchantFixtures.access();
  }

  private static final Identifier ITEM = Identifier.minecraft("item");

  private static EntityPredicate.Check onlyCheck(String json) {
    EntityPredicate predicate = Decoded.value(MatchCodecs.entity(Json.parse(json), access()));
    assertThat(predicate.checks()).hasSize(1);
    return predicate.checks().getFirst();
  }

  @Test
  void aBareNumberIsAnExactRange() {
    // spec: MatchCodecs#range contract
    assertThat(Decoded.value(MatchCodecs.range(Json.parse("3")))).isEqualTo(new NumberRange(3, 3));
  }

  @Test
  void anObjectRangeHasOptionalBounds() {
    // spec: LT-R35, LT-R58 ({"min": 1}, {"max": 9}, {"max": 4, "min": 1})
    assertThat(Decoded.value(MatchCodecs.range(Json.parse("{'min':1}"))))
        .isEqualTo(new NumberRange(1, Double.POSITIVE_INFINITY));
    assertThat(Decoded.value(MatchCodecs.range(Json.parse("{'max':9}"))))
        .isEqualTo(new NumberRange(Double.NEGATIVE_INFINITY, 9));
    assertThat(Decoded.value(MatchCodecs.range(Json.parse("{'min':1,'max':4}"))))
        .isEqualTo(new NumberRange(1, 4));
    assertThat(Decoded.value(MatchCodecs.range(Json.parse("{}"))))
        .isEqualTo(new NumberRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
  }

  @Test
  void aRangeOfTheWrongKindIsATypeMismatch() {
    // spec: MatchCodecs contract
    Decoded.failsWith(MatchCodecs.range(Json.parse("'x'")), LootFindingCode.TYPE_MISMATCH);
  }

  @Test
  void anIdentifierATagAndAListOfBothDecodeAsASet() {
    // spec: IdOrTagSet contract (identifier, #tag or list of both)
    IdOrTagSet single =
        Decoded.value(MatchCodecs.idOrTagSet(Json.parse("'minecraft:diamond'"), ITEM, access()));
    assertThat(single.ids()).containsExactly(Identifier.minecraft("diamond"));
    assertThat(single.tags()).isEmpty();
    assertThat(single.unrestricted()).isFalse();
    IdOrTagSet tag =
        Decoded.value(MatchCodecs.idOrTagSet(Json.parse("'#minecraft:test_gems'"), ITEM, access()));
    assertThat(tag.tags()).containsExactly(Identifier.minecraft("test_gems"));
    assertThat(tag.ids()).isEmpty();
    IdOrTagSet both =
        Decoded.value(
            MatchCodecs.idOrTagSet(
                Json.parse("['minecraft:stick','#minecraft:test_gems']"), ITEM, access()));
    assertThat(both.ids()).containsExactly(Identifier.minecraft("stick"));
    assertThat(both.tags()).containsExactly(Identifier.minecraft("test_gems"));
  }

  @Test
  void aTagThatIsNotLoadedIsUnresolved() {
    // spec: MatchCodecs contract (UNRESOLVED_TAG)
    Decoded.failsWith(
        MatchCodecs.idOrTagSet(Json.parse("'#minecraft:ghost'"), ITEM, access()),
        LootFindingCode.UNRESOLVED_TAG);
  }

  @Test
  void aBrokenIdentifierIsReported() {
    // spec: MatchCodecs contract (INVALID_IDENTIFIER)
    Decoded.failsWith(
        MatchCodecs.idOrTagSet(Json.parse("'Not A Valid:Id'"), ITEM, access()),
        LootFindingCode.INVALID_IDENTIFIER);
  }

  @Test
  void aBlockPredicateReadsBlocksAndState() {
    // spec: LT-R34
    BlockMatch match =
        Decoded.value(
            MatchCodecs.block(
                Json.parse("{'blocks':'minecraft:wheat','state':{'age':'7'}}"), access()));
    assertThat(match.blocks().ids()).containsExactly(Identifier.minecraft("wheat"));
    assertThat(match.state()).containsEntry("age", "7").hasSize(1);
  }

  @Test
  void aBlockPredicateWithoutBlocksIsUnrestricted() {
    // spec: BlockMatch contract (blocks unrestricted when the key is absent)
    BlockMatch match =
        Decoded.value(MatchCodecs.block(Json.parse("{'state':{'age':'7'}}"), access()));
    assertThat(match.blocks().unrestricted()).isTrue();
  }

  @Test
  void anItemPredicateReadsItemsAndCount() {
    // spec: LT-R58
    ItemMatch match =
        Decoded.value(
            MatchCodecs.item(
                Json.parse("{'items':'minecraft:shears','count':{'min':2}}"), access()));
    assertThat(match.items().ids()).containsExactly(Identifier.minecraft("shears"));
    assertThat(match.count()).isEqualTo(new NumberRange(2, Double.POSITIVE_INFINITY));
  }

  @Test
  void anItemPredicateKeepsItsComponentPredicates() {
    // spec: LT-R58 (predicates: enchantments with levels; map_id is not a registered type)
    ItemMatch match =
        Decoded.value(
            MatchCodecs.item(
                Json.parse(
                    "{'predicates':{'minecraft:enchantments':[{'enchantments':"
                        + "'minecraft:silk_touch','levels':{'min':1}}],'minecraft:map_id':{}}}"),
                access()));
    assertThat(match.componentPredicates().keySet())
        .containsExactly(
            Identifier.parse("minecraft:enchantments"), Identifier.parse("minecraft:map_id"));
  }

  @Test
  void anEntityPredicateReadsTheTypeAndTheFlags() {
    // spec: LT-R57a (entity_type, flags)
    EntityPredicate.Check type = onlyCheck("{'minecraft:entity_type':'minecraft:cow'}");
    assertThat(type).isInstanceOf(EntityPredicate.Check.EntityType.class);
    EntityPredicate.Check flags = onlyCheck("{'minecraft:flags':{'is_on_fire':true}}");
    assertThat(((EntityPredicate.Check.Flags) flags).flags())
        .containsEntry(EntityFlag.ON_FIRE, true);
  }

  @Test
  void anEntityPredicateReadsTheKindSpecificKeys() {
    // spec: LT-R57a (type_specific/sheep, cube_mob, fishing_hook)
    assertThat(onlyCheck("{'minecraft:type_specific/sheep':{'sheared':false}}"))
        .isEqualTo(new EntityPredicate.Check.Sheep(Tristate.FALSE));
    assertThat(onlyCheck("{'minecraft:type_specific/fishing_hook':{'in_open_water':true}}"))
        .isEqualTo(new EntityPredicate.Check.FishingHook(Tristate.TRUE));
    assertThat(onlyCheck("{'minecraft:type_specific/cube_mob':{'size':{'min':2}}}"))
        .isEqualTo(new EntityPredicate.Check.CubeMob(new NumberRange(2, Double.POSITIVE_INFINITY)));
  }

  @Test
  void anEntityPredicateReadsEquipmentMovementAndPeriodicTick() {
    // spec: LT-R57a (equipment: slot -> item predicate, movement, periodic_tick)
    EntityPredicate.Check equipment =
        onlyCheck("{'minecraft:equipment':{'mainhand':{'items':'minecraft:shears'}}}");
    assertThat(((EntityPredicate.Check.Equipment) equipment).slots().keySet())
        .containsExactly(EquipmentSlot.MAINHAND);
    EntityPredicate.Check movement =
        onlyCheck("{'minecraft:movement':{'horizontal_speed':{'min':0.5}}}");
    assertThat(((EntityPredicate.Check.Movement) movement).horizontalSpeed())
        .isEqualTo(new NumberRange(0.5, Double.POSITIVE_INFINITY));
    assertThat(onlyCheck("{'minecraft:periodic_tick':20}"))
        .isEqualTo(new EntityPredicate.Check.PeriodicTick(20));
  }

  @Test
  void anEntityPredicateReadsComponentsAndVehicle() {
    // spec: LT-R57a (components {"minecraft:sheep/color": "white"}, vehicle)
    EntityPredicate.Check components =
        onlyCheck("{'minecraft:components':{'minecraft:sheep/color':'white'}}");
    assertThat(((EntityPredicate.Check.Components) components).expected().keySet())
        .containsExactly(Identifier.parse("minecraft:sheep/color"));
    EntityPredicate.Check vehicle =
        onlyCheck("{'minecraft:vehicle':{'minecraft:entity_type':'minecraft:pillager'}}");
    assertThat(vehicle).isInstanceOf(EntityPredicate.Check.Vehicle.class);
  }

  @Test
  void theKeysOfAnEntityPredicateKeepTheirFileOrder() {
    // spec: EntityPredicate contract (the keys present, in file order)
    EntityPredicate predicate =
        Decoded.value(
            MatchCodecs.entity(
                Json.parse(
                    "{'minecraft:flags':{'is_on_ground':true},"
                        + "'minecraft:entity_type':'minecraft:cow'}"),
                access()));
    assertThat(predicate.checks()).hasSize(2);
    assertThat(predicate.checks().get(0)).isInstanceOf(EntityPredicate.Check.Flags.class);
    assertThat(predicate.checks().get(1)).isInstanceOf(EntityPredicate.Check.EntityType.class);
  }

  @Test
  void anEntityPredicateKeyOutsideTheRegistryIsRefused() {
    // spec: LT-R57a (25 registered keys)
    Decoded.fails(MatchCodecs.entity(Json.parse("{'minecraft:no_such_key':1}"), access()));
  }

  @Test
  void aLocationPredicateReadsBlockBiomesAndSky() {
    // spec: LT-R59 (block, biomes, can_see_sky)
    LocationPredicate location =
        Decoded.value(
            MatchCodecs.location(
                Json.parse(
                    "{'block':{'blocks':'minecraft:stone'},'biomes':'minecraft:plains',"
                        + "'can_see_sky':true}"),
                access()));
    assertThat(location.requirements()).hasSize(3);
    assertThat(location.requirements().get(0))
        .isInstanceOf(LocationPredicate.Requirement.Block.class);
    assertThat(location.requirements().get(1))
        .isInstanceOf(LocationPredicate.Requirement.Biomes.class);
    assertThat(location.requirements().get(2))
        .isEqualTo(new LocationPredicate.Requirement.CanSeeSky(true));
  }

  @Test
  void aDamageSourcePredicateReadsTagsAndDirectness() {
    // spec: LT-R60 (tags, is_direct)
    DamageSourcePredicate predicate =
        Decoded.value(
            MatchCodecs.damageSource(
                Json.parse(
                    "{'tags':[{'id':'minecraft:is_projectile','expected':true}],'is_direct':false}"),
                access()));
    assertThat(predicate.requirements())
        .containsExactly(
            new DamageSourcePredicate.Requirement.Tags(
                List.of(
                    new DamageSourcePredicate.TagExpectation(
                        Identifier.minecraft("is_projectile"), true))),
            new DamageSourcePredicate.Requirement.IsDirect(false));
  }

  @Test
  void aDamageSourcePredicateReadsTheSourceEntity() {
    // spec: LT-R60 (source_entity: entity predicate)
    DamageSourcePredicate predicate =
        Decoded.value(
            MatchCodecs.damageSource(
                Json.parse("{'source_entity':{'minecraft:entity_type':'minecraft:player'}}"),
                access()));
    assertThat(predicate.requirements()).hasSize(1);
    assertThat(predicate.requirements().getFirst())
        .isInstanceOf(DamageSourcePredicate.Requirement.SourceEntity.class);
  }
}
