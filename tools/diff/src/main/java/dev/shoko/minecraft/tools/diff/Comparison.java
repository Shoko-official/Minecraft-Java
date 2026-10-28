package dev.shoko.minecraft.tools.diff;

import java.util.List;
import java.util.Optional;

/**
 * Outcome of comparing an actual state with the expected (reference) state.
 *
 * @param divergences every difference, in the order of {@link StateComparator}
 * @param expectedSize number of entries of the expected state
 * @param actualSize number of entries of the actual state
 */
public record Comparison(
    Verdict verdict,
    List<Divergence> divergences,
    int expectedSize,
    int actualSize,
    String expectedDigest,
    String actualDigest) {

  /** Overall verdict. */
  public enum Verdict {
    IDENTICAL,
    DIVERGENT
  }

  public Comparison {
    divergences = List.copyOf(divergences);
    if ((verdict == Verdict.IDENTICAL) != divergences.isEmpty()) {
      throw new IllegalArgumentException("verdict " + verdict + " with " + divergences.size());
    }
  }

  public int count() {
    return divergences.size();
  }

  /** The first difference, which locates where to start investigating. */
  public Optional<Divergence> first() {
    return divergences.stream().findFirst();
  }
}
