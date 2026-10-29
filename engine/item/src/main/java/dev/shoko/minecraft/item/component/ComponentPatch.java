package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

/**
 * The modifications a stack carries over the prototype of its item (IC-R14). Immutable.
 *
 * @param changes the changes in insertion order, unmodifiable, no {@code null}
 */
public record ComponentPatch(SequencedMap<Identifier, ComponentChange> changes) {

  private static final ComponentPatch EMPTY = new ComponentPatch(new LinkedHashMap<>());

  /**
   * Copies the changes.
   *
   * @param changes the changes in insertion order
   */
  public ComponentPatch {
    changes = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(changes));
  }

  /**
   * Returns the patch with no change.
   *
   * @return the empty patch
   */
  public static ComponentPatch empty() {
    return EMPTY;
  }

  /**
   * Tells whether the patch changes nothing.
   *
   * @return {@code true} when there is no change
   */
  public boolean isEmpty() {
    return changes.isEmpty();
  }

  /**
   * Returns a patch that also sets a component.
   *
   * @param id the component
   * @param value its value
   * @return the new patch; a previous change of the same component is replaced
   */
  public ComponentPatch withSet(Identifier id, ComponentValue value) {
    return with(id, new ComponentChange.Set(value));
  }

  /**
   * Returns a patch that also removes a component of the prototype.
   *
   * @param id the component
   * @return the new patch; a previous change of the same component is replaced
   */
  public ComponentPatch withRemoved(Identifier id) {
    return with(id, new ComponentChange.Removed());
  }

  private ComponentPatch with(Identifier id, ComponentChange change) {
    LinkedHashMap<Identifier, ComponentChange> copy = new LinkedHashMap<>(changes);
    copy.put(id, change);
    return new ComponentPatch(copy);
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
    LinkedHashMap<Identifier, ComponentChange> kept = new LinkedHashMap<>();
    changes.forEach(
        (id, change) -> {
          boolean useless =
              switch (change) {
                case ComponentChange.Set set ->
                    prototype.get(id).filter(set.value()::equals).isPresent();
                case ComponentChange.Removed ignored -> !prototype.contains(id);
              };
          if (!useless) {
            kept.put(id, change);
          }
        });
    return kept.size() == changes.size() ? this : new ComponentPatch(kept);
  }
}
