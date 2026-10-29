package dev.shoko.minecraft.item.component;

/**
 * One modification of a stack relative to the prototype of its item (IC-R14): a component set to a
 * value, or a component of the prototype removed.
 */
public sealed interface ComponentChange permits ComponentChange.Set, ComponentChange.Removed {

  /**
   * The component takes this value, added or replacing the prototype value.
   *
   * @param value the value
   */
  record Set(ComponentValue value) implements ComponentChange {}

  /** The component of the prototype is removed; written under the key "!id" with {} (IC-R27). */
  record Removed() implements ComponentChange {}
}
