package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.SequencedMap;

/**
 * The modifications a stack carries over the prototype of its item (IC-R14). Immutable.
 *
 * @param changes the changes in insertion order, unmodifiable, no {@code null}
 */
public record ComponentPatch(SequencedMap<Identifier, ComponentChange> changes) {

  /**
   * Returns the patch with no change.
   *
   * @return the empty patch
   */
  public static ComponentPatch empty() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether the patch changes nothing.
   *
   * @return {@code true} when there is no change
   */
  public boolean isEmpty() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a patch that also sets a component.
   *
   * @param id the component
   * @param value its value
   * @return the new patch; a previous change of the same component is replaced
   */
  public ComponentPatch withSet(Identifier id, ComponentValue value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a patch that also removes a component of the prototype.
   *
   * @param id the component
   * @return the new patch; a previous change of the same component is replaced
   */
  public ComponentPatch withRemoved(Identifier id) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Normalizes the patch against a prototype (IC-R17): a set to a value equal to the prototype (bit
   * for bit) and a removal of a component the prototype lacks are dropped (IC-R18), so two stacks
   * with the same effective components have equal patches.
   *
   * @param prototype the prototype components of the item
   * @return the normalized patch
   */
  public ComponentPatch normalizedAgainst(ComponentMap prototype) {
    return Skeleton.throwNotImplemented();
  }
}
