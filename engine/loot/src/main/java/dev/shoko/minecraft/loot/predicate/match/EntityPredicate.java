package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.port.EntityFlag;
import dev.shoko.minecraft.loot.port.EntityView;
import dev.shoko.minecraft.loot.port.EquipmentSlot;
import java.util.List;
import java.util.SequencedMap;

/**
 * The entity predicate of {@code entity_properties.predicate} (LT-R57a): an object whose keys are
 * entity sub-predicate types. All requirements must hold (LT-R57b, provisional). The empty
 * predicate matches every entity.
 *
 * @param checks the keys present, in file order, unmodifiable
 */
public record EntityPredicate(List<Check> checks) {

  /**
   * One key of an entity predicate. The record names follow the keys met in the data (counts in
   * LT-R57a).
   */
  public sealed interface Check
      permits Check.EntityType,
          Check.Flags,
          Check.Components,
          Check.Equipment,
          Check.Vehicle,
          Check.MovementAffectedBy,
          Check.Movement,
          Check.PeriodicTick,
          Check.Location,
          Check.Sheep,
          Check.CubeMob,
          Check.Raider,
          Check.FishingHook,
          Check.Player,
          Check.Other {

    /**
     * {@code minecraft:entity_type}.
     *
     * @param types the accepted entity types
     */
    record EntityType(IdOrTagSet types) implements Check {}

    /**
     * {@code minecraft:flags}.
     *
     * @param flags the expected value of each flag listed
     */
    record Flags(SequencedMap<EntityFlag, Boolean> flags) implements Check {}

    /**
     * {@code minecraft:components}, such as {@code {"minecraft:sheep/color": "white"}}.
     *
     * @param expected the expected value of each component, as JSON
     */
    record Components(SequencedMap<Identifier, JsonNode> expected) implements Check {}

    /**
     * {@code minecraft:equipment}.
     *
     * @param slots the item predicate of each slot listed
     */
    record Equipment(SequencedMap<EquipmentSlot, ItemMatch> slots) implements Check {}

    /**
     * {@code minecraft:vehicle}.
     *
     * @param vehicle the predicate of the vehicle
     */
    record Vehicle(EntityPredicate vehicle) implements Check {}

    /**
     * {@code minecraft:movement_affected_by}: the block that affects the movement.
     *
     * @param block the block predicate
     */
    record MovementAffectedBy(BlockMatch block) implements Check {}

    /**
     * {@code minecraft:movement}.
     *
     * @param horizontalSpeed the accepted horizontal speed
     * @param fallDistance the accepted fall distance
     */
    record Movement(NumberRange horizontalSpeed, NumberRange fallDistance) implements Check {}

    /**
     * {@code minecraft:periodic_tick}.
     *
     * @param period the period in ticks; matches when the tick counter is a multiple of it
     */
    record PeriodicTick(int period) implements Check {}

    /**
     * {@code minecraft:location}.
     *
     * @param location the position predicate
     */
    record Location(LocationPredicate location) implements Check {}

    /**
     * {@code minecraft:type_specific/sheep}.
     *
     * @param sheared the expected shearing state
     */
    record Sheep(Tristate sheared) implements Check {}

    /**
     * {@code minecraft:type_specific/cube_mob}.
     *
     * @param size the accepted size
     */
    record CubeMob(NumberRange size) implements Check {}

    /**
     * {@code minecraft:type_specific/raider}.
     *
     * @param captain the expected captain state
     */
    record Raider(Tristate captain) implements Check {}

    /**
     * {@code minecraft:type_specific/fishing_hook}.
     *
     * @param inOpenWater the expected state
     */
    record FishingHook(Tristate inOpenWater) implements Check {}

    /** {@code minecraft:type_specific/player}: the keys are open (LT-R57a), so no field. */
    record Player() implements Check {}

    /**
     * A registered key that no data of the base uses ({@code distance}, {@code stepping_on}, {@code
     * effects}, {@code nbt}, {@code passenger}, {@code team}, {@code slots}...). Kept as read;
     * matches nothing and raises a finding (provisional).
     *
     * @param key the sub-predicate type
     * @param value the value as read
     */
    record Other(Identifier key, JsonNode value) implements Check {}
  }

  /**
   * Tells whether an entity satisfies the predicate.
   *
   * @param entity the entity to test
   * @param context the context, for the registries (tags of entity types) and the chunks
   * @return {@code true} when every check holds
   */
  public boolean matches(EntityView entity, LootContext context) {
    return Skeleton.throwNotImplemented();
  }
}
