package dev.shoko.minecraft.loot.predicate.match;

/**
 * An inclusive numeric interval such as {@code {"min": 1}} or a bare number (LT-R35, LT-R58). An
 * absent bound is infinite.
 *
 * @param min the lower bound, {@link Double#NEGATIVE_INFINITY} when absent
 * @param max the upper bound, {@link Double#POSITIVE_INFINITY} when absent
 */
public record NumberRange(double min, double max) {

  /**
   * Tells whether a value is in the interval.
   *
   * @param value any number
   * @return {@code true} when {@code min <= value <= max}
   */
  public boolean contains(double value) {
    return Skeleton.throwNotImplemented();
  }
}
