package dev.shoko.minecraft.tools.diff;

/**
 * One difference between an expected (reference) state and an actual state.
 *
 * <p>For {@link Kind#VALUE}, {@code expected} and {@code actual} are the two values of {@code key}.
 * For {@link Kind#MISSING} and {@link Kind#EXTRA}, the absent side holds {@link #ABSENT}. For
 * {@link Kind#ORDER}, {@code key} was moved: {@code expected} and {@code actual} are its positions
 * in each state, in decimal (see {@link StateComparator} for which keys count as moved).
 *
 * @param expectedIndex position in the expected state, or -1 for an extra entry
 * @param actualIndex position in the actual state, or -1 for a missing entry
 */
public record Divergence(
    Kind kind, String key, String expected, String actual, int expectedIndex, int actualIndex) {

  /** Shown in place of a value that one side does not have; the kind tells which side. */
  public static final String ABSENT = "(absent)";

  /** Nature of a difference. */
  public enum Kind {
    /** Same key on both sides, different values. */
    VALUE,
    /** Key present in the expected state only. */
    MISSING,
    /** Key present in the actual state only. */
    EXTRA,
    /** Key present on both sides, moved relative to the keys kept in place. */
    ORDER
  }
}
