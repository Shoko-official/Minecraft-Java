package dev.shoko.minecraft.loot.port;

import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.Optional;

/**
 * Read-only view of an entity (LT-R16): the only port through which loot predicates and enchantment
 * effects see an entity. The surface is the one deduced from the predicate keys of the data
 * (section {@code R57} of the spec). Implemented by {@code entity} and {@code game}; tests use a
 * fake. Methods have no side effect and return the same value until the entity changes.
 */
public interface EntityView {

  /**
   * Returns the entity type, whose tags are looked up through the registries.
   *
   * @return the type identifier, such as {@code minecraft:cow}
   */
  Identifier type();

  /**
   * Returns the position. The block at this position is read through {@code world}.
   *
   * @return the position of the entity
   */
  Vec3 position();

  /**
   * Tells whether a flag is set.
   *
   * @param flag the flag
   * @return {@code true} when set
   */
  boolean hasFlag(EntityFlag flag);

  /**
   * Returns the stack worn or held in a slot.
   *
   * @param slot the slot
   * @return the stack, the empty stack when the slot is empty or does not exist for this entity
   */
  ItemStack equipment(EquipmentSlot slot);

  /**
   * Reads a component of the entity, such as {@code minecraft:sheep/color} (LT-R57a).
   *
   * @param component the component identifier
   * @return the value, empty when the entity has no such component
   */
  Optional<ComponentValue> component(Identifier component);

  /**
   * Returns the vehicle.
   *
   * @return the vehicle, empty when the entity does not ride
   */
  Optional<EntityView> vehicle();

  /**
   * Returns the movement values read by the {@code movement} predicate.
   *
   * @return the horizontal speed and the fall distance
   */
  EntityMotion motion();

  /**
   * Returns the tick counter read by the {@code periodic_tick} predicate (EN-R30).
   *
   * @return the number of ticks lived
   */
  int tickCount();

  /**
   * Returns the state of the kind-specific predicates.
   *
   * @return the state, empty for an entity of no kind-specific predicate
   */
  Optional<TypeSpecific> typeSpecific();
}
