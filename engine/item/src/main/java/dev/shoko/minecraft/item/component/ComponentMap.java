package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * Component values by component identifier: the prototype of an item (IC-R2) or the effective
 * components of a stack. Immutable; the equality of two maps ignores the order of the entries.
 *
 * @param entries the values in insertion order, unmodifiable, no {@code null}
 */
public record ComponentMap(SequencedMap<Identifier, ComponentValue> entries) {

  private static final ComponentMap EMPTY = new ComponentMap(new LinkedHashMap<>());

  /**
   * Copies the entries.
   *
   * @param entries the values in insertion order
   */
  public ComponentMap {
    entries = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(entries));
  }

  /**
   * Returns the map without any component.
   *
   * @return the empty map
   */
  public static ComponentMap empty() {
    return EMPTY;
  }

  /**
   * Reads one component.
   *
   * @param id a component identifier
   * @return the value, or empty when the component is absent
   */
  public Optional<ComponentValue> get(Identifier id) {
    return Optional.ofNullable(entries.get(id));
  }

  /**
   * Tells whether a component is present, even with an empty value (IC-R6b).
   *
   * @param id a component identifier
   * @return {@code true} when present
   */
  public boolean contains(Identifier id) {
    return entries.containsKey(id);
  }

  /**
   * Applies a patch.
   *
   * @param patch the modifications of a stack
   * @return the map with the values set and the removed components dropped (IC-R14)
   */
  public ComponentMap apply(ComponentPatch patch) {
    if (patch.isEmpty()) {
      return this;
    }
    LinkedHashMap<Identifier, ComponentValue> result = new LinkedHashMap<>(entries);
    patch
        .changes()
        .forEach(
            (id, change) -> {
              switch (change) {
                case ComponentChange.Set set -> result.put(id, set.value());
                case ComponentChange.Removed ignored -> result.remove(id);
              }
            });
    return new ComponentMap(result);
  }
}
