package dev.shoko.minecraft.tools.diff;

import dev.shoko.minecraft.tools.diff.CanonicalState.Entry;
import dev.shoko.minecraft.tools.diff.Divergence.Kind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares an actual state with the expected (reference) state, entry by entry.
 *
 * <ul>
 *   <li>{@link Kind#MISSING}: key of the expected state absent from the actual state;
 *   <li>{@link Kind#EXTRA}: key of the actual state absent from the expected state;
 *   <li>{@link Kind#VALUE}: key on both sides with different values;
 *   <li>{@link Kind#ORDER}: key on both sides, moved. Among the keys present on both sides, the
 *       largest set that keeps the same relative order in both states (a longest common
 *       subsequence) stays in place; every other common key is moved and gives one ORDER
 *       divergence. The ORDER count is thus the minimal number of entries to move, the same in both
 *       directions: one entry taken from the start and put at the end counts 1, not 999.
 * </ul>
 *
 * <p>Divergences come in document order of the expected state: at its position for an expected key
 * (ORDER before VALUE for the same key), and just before the next key kept in place for an extra
 * key, so after any divergence of the expected keys that precede it. The first divergence is the
 * earliest place where the states part. The result depends only on the two states.
 */
public final class StateComparator {

  private StateComparator() {}

  /** A divergence and its place in document order. */
  private record Placed(long rank, int tie, Divergence divergence) {}

  public static Comparison compare(CanonicalState expected, CanonicalState actual) {
    List<Entry> want = expected.entries();
    List<Entry> got = actual.entries();
    Map<String, Integer> wantIndex = indexOf(want);
    Map<String, Integer> gotIndex = indexOf(got);
    boolean[] kept = keptInPlace(want, gotIndex, got.size());
    List<Placed> placed = new ArrayList<>();
    for (int i = 0; i < want.size(); i++) {
      Entry e = want.get(i);
      Integer k = gotIndex.get(e.key());
      long rank = 2L * i;
      if (k == null) {
        placed.add(
            new Placed(
                rank,
                0,
                new Divergence(Kind.MISSING, e.key(), e.value(), Divergence.ABSENT, i, -1)));
        continue;
      }
      if (!kept[k]) {
        String from = Integer.toString(i);
        String to = Integer.toString(k);
        placed.add(new Placed(rank, 0, new Divergence(Kind.ORDER, e.key(), from, to, i, k)));
      }
      String value = got.get(k).value();
      if (!value.equals(e.value())) {
        placed.add(
            new Placed(rank, 1, new Divergence(Kind.VALUE, e.key(), e.value(), value, i, k)));
      }
    }
    long nextKept = 2L * want.size();
    for (int j = got.size() - 1; j >= 0; j--) {
      Entry a = got.get(j);
      Integer i = wantIndex.get(a.key());
      if (i == null) {
        placed.add(
            new Placed(
                nextKept - 1,
                j,
                new Divergence(Kind.EXTRA, a.key(), Divergence.ABSENT, a.value(), -1, j)));
      } else if (kept[j]) {
        nextKept = 2L * i;
      }
    }
    placed.sort(Comparator.comparingLong(Placed::rank).thenComparingInt(Placed::tie));
    List<Divergence> divergences = placed.stream().map(Placed::divergence).toList();
    return new Comparison(
        divergences.isEmpty() ? Comparison.Verdict.IDENTICAL : Comparison.Verdict.DIVERGENT,
        divergences,
        want.size(),
        got.size(),
        expected.digest(),
        actual.digest());
  }

  /**
   * Marks, by actual position, the common entries kept in place: a longest increasing subsequence
   * of their actual positions taken in expected order (patience sorting, O(n log n)). Among
   * subsequences of the same length, the one found is fixed by the two states.
   */
  private static boolean[] keptInPlace(
      List<Entry> want, Map<String, Integer> gotIndex, int gotSize) {
    List<Integer> positions = new ArrayList<>();
    for (Entry e : want) {
      Integer k = gotIndex.get(e.key());
      if (k != null) {
        positions.add(k);
      }
    }
    int n = positions.size();
    int[] tails = new int[n];
    int[] previous = new int[n];
    int length = 0;
    for (int p = 0; p < n; p++) {
      int value = positions.get(p);
      int low = 0;
      int high = length;
      while (low < high) {
        int middle = (low + high) >>> 1;
        if (positions.get(tails[middle]) < value) {
          low = middle + 1;
        } else {
          high = middle;
        }
      }
      previous[p] = low > 0 ? tails[low - 1] : -1;
      tails[low] = p;
      length = Math.max(length, low + 1);
    }
    boolean[] kept = new boolean[gotSize];
    for (int p = length > 0 ? tails[length - 1] : -1; p >= 0; p = previous[p]) {
      kept[positions.get(p)] = true;
    }
    return kept;
  }

  /**
   * Prefixes of depth {@code depth} whose sub-state digests differ or exist on one side only:
   * expected prefixes first in their order, then prefixes found only in the actual state.
   */
  public static List<String> divergentSubStates(
      CanonicalState expected, CanonicalState actual, int depth) {
    Map<String, String> want = expected.digestsAt(depth);
    Map<String, String> got = actual.digestsAt(depth);
    List<String> prefixes = new ArrayList<>();
    want.forEach(
        (prefix, digest) -> {
          if (!digest.equals(got.get(prefix))) {
            prefixes.add(prefix);
          }
        });
    got.keySet().stream().filter(prefix -> !want.containsKey(prefix)).forEach(prefixes::add);
    return List.copyOf(prefixes);
  }

  private static Map<String, Integer> indexOf(List<Entry> entries) {
    Map<String, Integer> index = new HashMap<>();
    for (int i = 0; i < entries.size(); i++) {
      index.put(entries.get(i).key(), i);
    }
    return index;
  }
}
