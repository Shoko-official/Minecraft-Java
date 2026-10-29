package dev.shoko.minecraft.world.block;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A block and the layout of its global state identifiers (R5, R6).
 *
 * <p>The states of a block are the cartesian product of its property values, with the properties
 * sorted by name (code point order, never the order of the report: R8) and the last property
 * varying fastest. With {@code i_k} the rank of the value of property {@code k} and {@code n_j} the
 * number of values of property {@code j}: {@code id = firstStateId + sum_k(i_k * product_{j>k}
 * n_j)}.
 *
 * @param id block identifier with namespace, such as {@code minecraft:stone}
 * @param protocolId position in the {@code minecraft:block} registry (R4), from 0
 * @param properties properties sorted by name, unmodifiable, empty when the block has none (R15)
 * @param firstStateId identifier of the first state: the sum of the state counts of all blocks with
 *     a smaller protocol id (R5)
 * @param defaultStateId identifier of the default state, inside the range of this block (R10)
 */
public record BlockType(
    String id,
    int protocolId,
    List<BlockProperty> properties,
    int firstStateId,
    int defaultStateId) {

  /**
   * Returns the number of states, the product of the property sizes, 1 without properties (R3,
   * R15). The largest block is {@code minecraft:note_block} with 1350.
   *
   * @return the number of states
   */
  public int stateCount() {
    int count = 1;
    for (BlockProperty property : properties) {
      count *= property.size();
    }
    return count;
  }

  /**
   * Finds a property by name.
   *
   * @param name property name
   * @return the property, or empty when this block has none of that name
   */
  public Optional<BlockProperty> property(String name) {
    for (BlockProperty property : properties) {
      if (property.name().equals(name)) {
        return Optional.of(property);
      }
    }
    return Optional.empty();
  }

  /**
   * Computes the identifier of the state with exactly these values (R6). The result is independent
   * of the iteration order of the map.
   *
   * @param values one value per property of this block, by property name
   * @return the state identifier
   * @throws IllegalArgumentException if a property is missing, unknown, or given a value outside
   *     its list
   */
  public int stateId(Map<String, String> values) {
    if (values.size() != properties.size()) {
      throw new IllegalArgumentException(
          id + " has " + properties.size() + " properties, got " + values);
    }
    int offset = 0;
    for (BlockProperty property : properties) {
      String value = values.get(property.name());
      if (value == null) {
        throw new IllegalArgumentException(id + " lacks a value for " + property.name());
      }
      offset = offset * property.size() + property.rankOf(value);
    }
    return firstStateId + offset;
  }

  /**
   * Reads one property of a state of this block.
   *
   * @param stateId identifier of a state of this block
   * @param propertyName name of a property of this block
   * @return the value as text
   * @throws IllegalArgumentException if the state is not a state of this block or the property is
   *     unknown
   */
  public String valueOf(int stateId, String propertyName) {
    int offset = offsetOf(stateId);
    int stride = 1;
    for (int k = properties.size() - 1; k >= 0; k--) {
      BlockProperty property = properties.get(k);
      if (property.name().equals(propertyName)) {
        return property.values().get(offset / stride % property.size());
      }
      stride *= property.size();
    }
    throw new IllegalArgumentException(id + " has no property " + propertyName);
  }

  /**
   * Returns the identifier of the state that equals the given one except for one property (R6).
   *
   * @param stateId identifier of a state of this block
   * @param propertyName name of a property of this block
   * @param value new value, in the list of that property
   * @return the identifier of the changed state
   * @throws IllegalArgumentException if the state is not a state of this block, the property is
   *     unknown, or the value is not in its list
   */
  public int withValue(int stateId, String propertyName, String value) {
    int offset = offsetOf(stateId);
    int stride = 1;
    for (int k = properties.size() - 1; k >= 0; k--) {
      BlockProperty property = properties.get(k);
      if (property.name().equals(propertyName)) {
        int oldRank = offset / stride % property.size();
        return stateId + (property.rankOf(value) - oldRank) * stride;
      }
      stride *= property.size();
    }
    throw new IllegalArgumentException(id + " has no property " + propertyName);
  }

  private int offsetOf(int stateId) {
    int offset = stateId - firstStateId;
    if (offset < 0 || offset >= stateCount()) {
      throw new IllegalArgumentException("state " + stateId + " is not a state of " + id);
    }
    return offset;
  }
}
