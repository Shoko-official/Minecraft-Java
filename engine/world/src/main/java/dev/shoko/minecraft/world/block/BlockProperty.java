package dev.shoko.minecraft.world.block;

import java.util.List;

/**
 * One property of a block with its possible values, in the order of the block report (R1, R9).
 *
 * <p>The order of values depends on the block, not on the property name: {@code facing} has three
 * different lists. The rank of a value is its position in the list, not its numeric value ({@code
 * level} of a cauldron starts at 1). Booleans are listed {@code true, false}, so {@code true} has
 * rank 0.
 *
 * @param name property name, ASCII lower case, digits and underscores
 * @param values the possible values as text, unmodifiable, at least one, without duplicates
 */
public record BlockProperty(String name, List<String> values) {

  /**
   * Returns the position of a value in {@link #values()} (R6, R9).
   *
   * @param value a value of this property
   * @return the rank, from 0
   * @throws IllegalArgumentException if the value is not in the list
   */
  public int rankOf(String value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of values, the factor of this property in the number of states (R6).
   *
   * @return the number of values
   */
  public int size() {
    return Skeleton.throwNotImplemented();
  }
}
