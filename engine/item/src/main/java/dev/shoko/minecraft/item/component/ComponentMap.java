package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * Component values by component identifier: the prototype of an item (IC-R2) or the effective
 * components of a stack. Immutable; the equality of two maps ignores the order of the entries.
 *
 * @param entries the values in insertion order, unmodifiable, no {@code null}
 */
public record ComponentMap(SequencedMap<Identifier, ComponentValue> entries) {

  /**
   * Returns the map without any component.
   *
   * @return the empty map
   */
  public static ComponentMap empty() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads one component.
   *
   * @param id a component identifier
   * @return the value, or empty when the component is absent
   */
  public Optional<ComponentValue> get(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether a component is present, even with an empty value (IC-R6b).
   *
   * @param id a component identifier
   * @return {@code true} when present
   */
  public boolean contains(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Applies a patch.
   *
   * @param patch the modifications of a stack
   * @return the map with the values set and the removed components dropped (IC-R14)
   */
  public ComponentMap apply(ComponentPatch patch) {
    return Skeleton.throwNotImplemented();
  }
}
