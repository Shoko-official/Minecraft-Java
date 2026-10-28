package dev.shoko.minecraft.tools.diff;

import dev.shoko.minecraft.tools.diff.CanonicalState.Entry;
import dev.shoko.minecraft.tools.diff.Divergence.Kind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares an actual state with the expected (reference) state, entry by entry.
 *
 * <p>Both states are walked together, so divergences come out in document order and the first one
 * is the earliest place where the states part. At each step: an expected key absent from the actual
 * state is {@link Kind#MISSING}; otherwise an actual key absent from the expected state is {@link
 * Kind#EXTRA}; otherwise both keys exist on both sides, and either they are the same key (its
 * values are compared, {@link Kind#VALUE}) or the actual state has them in another order: one
 * {@link Kind#ORDER} divergence is recorded, the expected key is compared with its counterpart
 * wherever it is, and the walk goes on. The result depends only on the two states.
 */
public final class StateComparator {

  private StateComparator() {}

  public static Comparison compare(CanonicalState expected, CanonicalState actual) {
    List<Entry> want = expected.entries();
    List<Entry> got = actual.entries();
    Map<String, Integer> wantIndex = indexOf(want);
    Map<String, Integer> gotIndex = indexOf(got);
    boolean[] consumed = new boolean[got.size()];
    List<Divergence> divergences = new ArrayList<>();
    int i = 0;
    int j = 0;
    while (true) {
      while (j < got.size() && consumed[j]) {
        j++;
      }
      if (i < want.size() && !gotIndex.containsKey(want.get(i).key())) {
        Entry e = want.get(i);
        divergences.add(new Divergence(Kind.MISSING, e.key(), e.value(), Divergence.ABSENT, i, -1));
        i++;
      } else if (j < got.size() && !wantIndex.containsKey(got.get(j).key())) {
        Entry a = got.get(j);
        divergences.add(new Divergence(Kind.EXTRA, a.key(), Divergence.ABSENT, a.value(), -1, j));
        j++;
      } else if (i < want.size()) {
        // Both keys are common: an unconsumed counterpart of want[i] lies at or after j.
        Entry e = want.get(i);
        int k = gotIndex.get(e.key());
        if (k != j) {
          divergences.add(new Divergence(Kind.ORDER, e.key(), e.key(), got.get(j).key(), i, j));
        }
        String value = got.get(k).value();
        if (!value.equals(e.value())) {
          divergences.add(new Divergence(Kind.VALUE, e.key(), e.value(), value, i, k));
        }
        consumed[k] = true;
        i++;
      } else {
        return new Comparison(
            divergences.isEmpty() ? Comparison.Verdict.IDENTICAL : Comparison.Verdict.DIVERGENT,
            divergences,
            want.size(),
            got.size(),
            expected.digest(),
            actual.digest());
      }
    }
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
