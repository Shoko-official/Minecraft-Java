package dev.shoko.minecraft.world.block;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

/**
 * A block together with one value for each of its properties, identified by its global state
 * identifier, an integer from 0 to the state count minus 1 (R5, R6, R12).
 *
 * <p>Two states are equal when they have the same identifier. Obtain states from a {@link
 * BlockStateRegistry}: a constructed value with an identifier outside the range of its block is
 * invalid.
 *
 * @param block the block
 * @param id global state identifier, never written to disk
 */
public record BlockState(BlockType block, int id) {

  /**
   * Reads one property.
   *
   * @param propertyName name of a property of the block
   * @return the value as text
   * @throws IllegalArgumentException if the block has no such property
   */
  public String value(String propertyName) {
    return block.valueOf(id, propertyName);
  }

  /**
   * Returns the state that equals this one except for one property.
   *
   * @param propertyName name of a property of the block
   * @param value new value, in the list of that property
   * @return the other state of the same block
   * @throws IllegalArgumentException if the property is unknown or the value is not in its list
   */
  public BlockState with(String propertyName, String value) {
    return new BlockState(block, block.withValue(id, propertyName, value));
  }

  /**
   * Returns all property values, in property name order; empty for a block without properties
   * (R15).
   *
   * @return an unmodifiable map from property name to value
   */
  public SequencedMap<String, String> values() {
    SequencedMap<String, String> values = new LinkedHashMap<>();
    for (BlockProperty property : block.properties()) {
      values.put(property.name(), block.valueOf(id, property.name()));
    }
    return Collections.unmodifiableSequencedMap(values);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof BlockState state && state.id == id;
  }

  @Override
  public int hashCode() {
    return id;
  }
}
