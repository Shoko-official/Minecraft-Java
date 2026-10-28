package dev.shoko.minecraft.tools.diff;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.tools.diff.CanonicalState.Entry;
import dev.shoko.minecraft.tools.diff.Divergence.Kind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Differential test of {@link StateComparator} against a model written from the documented rules
 * (docs/VERIFICATION.md) with a quadratic longest common subsequence. Random pairs of states are
 * built from a fixed seed and compared in both directions; everything the comparator reports is
 * checked: kinds, indexes, ORDER count, document order, digests and divergent sub-states.
 */
class StateComparatorFuzzTest {

  private static final long SEED = 20261028L;
  private static final int ITERATIONS = 20_000;

  /** The two states of one case with the lookups the checks need. */
  private static final class Pair {
    final List<Entry> want;
    final List<Entry> got;
    final List<String> wantKeys;
    final List<String> gotKeys;
    final Map<String, Integer> wantIndex = new HashMap<>();
    final Map<String, Integer> gotIndex = new HashMap<>();
    final List<String> commonInWant;
    final List<String> commonInGot;

    Pair(List<Entry> want, List<Entry> got) {
      this.want = want;
      this.got = got;
      this.wantKeys = want.stream().map(Entry::key).toList();
      this.gotKeys = got.stream().map(Entry::key).toList();
      for (int i = 0; i < want.size(); i++) {
        wantIndex.put(wantKeys.get(i), i);
      }
      for (int i = 0; i < got.size(); i++) {
        gotIndex.put(gotKeys.get(i), i);
      }
      this.commonInWant = wantKeys.stream().filter(gotIndex::containsKey).toList();
      this.commonInGot = gotKeys.stream().filter(wantIndex::containsKey).toList();
    }

    String wantValue(String key) {
      return want.get(wantIndex.get(key)).value();
    }

    String gotValue(String key) {
      return got.get(gotIndex.get(key)).value();
    }

    @Override
    public String toString() {
      return "want=" + want + " got=" + got;
    }
  }

  private static String keyOf(int id) {
    return "c(" + id / 7 + ")/k(" + id + ")";
  }

  private static CanonicalState state(List<Entry> entries) {
    CanonicalState.Builder builder = CanonicalState.builder();
    entries.forEach(e -> builder.add(e.key(), e.value()));
    return builder.build();
  }

  private static int lcsLength(List<String> a, List<String> b) {
    int[][] length = new int[a.size() + 1][b.size() + 1];
    for (int i = 1; i <= a.size(); i++) {
      for (int j = 1; j <= b.size(); j++) {
        length[i][j] =
            a.get(i - 1).equals(b.get(j - 1))
                ? length[i - 1][j - 1] + 1
                : Math.max(length[i - 1][j], length[i][j - 1]);
      }
    }
    return length[a.size()][b.size()];
  }

  private static List<Entry> randomState(Random random, int universe, int size) {
    List<Integer> ids = new ArrayList<>();
    for (int i = 0; i < universe; i++) {
      ids.add(i);
    }
    Collections.shuffle(ids, random);
    List<Entry> entries = new ArrayList<>();
    for (int i = 0; i < Math.min(size, universe); i++) {
      entries.add(new Entry(keyOf(ids.get(i)), "v" + random.nextInt(3)));
    }
    return entries;
  }

  /** A few random edits: removal, insertion, new value, move, swap. */
  private static List<Entry> mutate(Random random, List<Entry> base, int universe) {
    List<Entry> out = new ArrayList<>(base);
    int edits = random.nextInt(6);
    for (int n = 0; n < edits; n++) {
      int size = out.size();
      switch (random.nextInt(5)) {
        case 0 -> {
          if (size > 0) {
            out.remove(random.nextInt(size));
          }
        }
        case 1 -> insertNewKey(random, out, universe);
        case 2 -> {
          if (size > 0) {
            int i = random.nextInt(size);
            out.set(i, new Entry(out.get(i).key(), "w" + random.nextInt(2)));
          }
        }
        case 3 -> {
          if (size > 0) {
            Entry moved = out.remove(random.nextInt(size));
            out.add(random.nextInt(size), moved);
          }
        }
        default -> {
          if (size > 1) {
            Collections.swap(out, random.nextInt(size), random.nextInt(size));
          }
        }
      }
    }
    return out;
  }

  private static void insertNewKey(Random random, List<Entry> out, int universe) {
    String key = keyOf(random.nextInt(universe + 5));
    if (out.stream().noneMatch(e -> e.key().equals(key))) {
      out.add(random.nextInt(out.size() + 1), new Entry(key, "v" + random.nextInt(3)));
    }
  }

  @Test
  void agreesWithTheModelOnRandomPairsOfStatesInBothDirections() {
    Random random = new Random(SEED);
    for (int iteration = 0; iteration < ITERATIONS; iteration++) {
      int universe = 1 + random.nextInt(iteration % 50 == 0 ? 120 : 14);
      List<Entry> want = randomState(random, universe, random.nextInt(universe + 1));
      List<Entry> got =
          iteration % 4 == 0
              ? randomState(random, universe, random.nextInt(universe + 1))
              : mutate(random, want, universe);
      check(new Pair(want, got), iteration);
      check(new Pair(got, want), iteration);
    }
  }

  private static void check(Pair pair, int iteration) {
    Supplier<String> context = () -> "iteration " + iteration + " " + pair;
    Comparison comparison = StateComparator.compare(state(pair.want), state(pair.got));
    boolean same = pair.want.equals(pair.got);
    assertThat(comparison.verdict() == Comparison.Verdict.IDENTICAL).as(context).isEqualTo(same);
    assertThat(comparison.expectedDigest().equals(comparison.actualDigest()))
        .as(context)
        .isEqualTo(same);
    Map<Kind, Set<String>> byKind = keysByKind(pair, comparison, context);
    checkKinds(pair, byKind, context);
    checkMovedKeys(pair, byKind.get(Kind.ORDER), context);
    assertThat(comparison.divergences())
        .as(() -> context.get() + " document order")
        .isEqualTo(documentOrder(pair, comparison, byKind.get(Kind.ORDER)));
    checkSubStates(pair, context);
  }

  /** Checks the fields of each divergence and returns the keys reported for each kind. */
  private static Map<Kind, Set<String>> keysByKind(
      Pair pair, Comparison comparison, Supplier<String> context) {
    Map<Kind, Set<String>> byKind = new HashMap<>();
    for (Kind kind : Kind.values()) {
      byKind.put(kind, new HashSet<>());
    }
    for (Divergence d : comparison.divergences()) {
      assertThat(byKind.get(d.kind()).add(d.key()))
          .as(() -> context.get() + " reported twice: " + d)
          .isTrue();
      assertThat(d).as(context).isEqualTo(modelDivergence(pair, d.kind(), d.key()));
    }
    return byKind;
  }

  /** The divergence the documented rules give for a key of a given kind. */
  private static Divergence modelDivergence(Pair pair, Kind kind, String key) {
    Integer in = pair.wantIndex.get(key);
    Integer at = pair.gotIndex.get(key);
    return switch (kind) {
      case MISSING ->
          new Divergence(Kind.MISSING, key, pair.wantValue(key), Divergence.ABSENT, in, -1);
      case EXTRA -> new Divergence(Kind.EXTRA, key, Divergence.ABSENT, pair.gotValue(key), -1, at);
      case VALUE ->
          new Divergence(Kind.VALUE, key, pair.wantValue(key), pair.gotValue(key), in, at);
      case ORDER -> new Divergence(Kind.ORDER, key, in.toString(), at.toString(), in, at);
    };
  }

  private static void checkKinds(Pair pair, Map<Kind, Set<String>> byKind, Supplier<String> ctx) {
    Set<String> missing = new HashSet<>(pair.wantKeys);
    missing.removeAll(pair.gotIndex.keySet());
    Set<String> extra = new HashSet<>(pair.gotKeys);
    extra.removeAll(pair.wantIndex.keySet());
    Set<String> value = new HashSet<>();
    for (String key : pair.commonInWant) {
      if (!pair.wantValue(key).equals(pair.gotValue(key))) {
        value.add(key);
      }
    }
    assertThat(byKind.get(Kind.MISSING)).as(ctx).isEqualTo(missing);
    assertThat(byKind.get(Kind.EXTRA)).as(ctx).isEqualTo(extra);
    assertThat(byKind.get(Kind.VALUE)).as(ctx).isEqualTo(value);
  }

  /** The moved keys are the common keys outside a longest common subsequence. */
  private static void checkMovedKeys(Pair pair, Set<String> moved, Supplier<String> ctx) {
    int lcs = lcsLength(pair.commonInWant, pair.commonInGot);
    assertThat(moved).as(() -> ctx.get() + " moved count").hasSize(pair.commonInWant.size() - lcs);
    assertThat(pair.commonInWant).containsAll(moved);
    assertThat(keptKeys(pair.commonInWant, moved))
        .as(() -> ctx.get() + " kept keys keep their relative order")
        .isEqualTo(keptKeys(pair.commonInGot, moved));
  }

  private static List<String> keptKeys(List<String> common, Set<String> moved) {
    return common.stream().filter(k -> !moved.contains(k)).toList();
  }

  /**
   * Builds the expected list another way: walk the expected keys, emit the extra entries anchored
   * on a kept key just before it, then the divergences of the key (ORDER before VALUE), and the
   * extra entries after the last kept key at the end.
   */
  private static List<Divergence> documentOrder(
      Pair pair, Comparison comparison, Set<String> moved) {
    Set<String> kept = new HashSet<>(keptKeys(pair.commonInWant, moved));
    Map<String, List<Divergence>> byKey = new HashMap<>();
    for (Divergence d : comparison.divergences()) {
      byKey.computeIfAbsent(d.key(), k -> new ArrayList<>()).add(d);
    }
    Map<String, List<String>> extrasBefore = extrasByAnchor(pair, kept, byKey);
    List<Divergence> expected = new ArrayList<>();
    for (String key : pair.wantKeys) {
      if (kept.contains(key)) {
        extrasBefore.getOrDefault(key, List.of()).forEach(x -> expected.addAll(byKey.get(x)));
      }
      List<Divergence> own = new ArrayList<>(byKey.getOrDefault(key, List.of()));
      own.sort((a, b) -> Boolean.compare(a.kind() == Kind.VALUE, b.kind() == Kind.VALUE));
      expected.addAll(own);
    }
    extrasBefore.getOrDefault("", List.of()).forEach(x -> expected.addAll(byKey.get(x)));
    return expected;
  }

  /** Extra keys of the actual state grouped by the next kept key after them, "" for none. */
  private static Map<String, List<String>> extrasByAnchor(
      Pair pair, Set<String> kept, Map<String, List<Divergence>> byKey) {
    Map<String, List<String>> extras = new HashMap<>();
    String anchor = "";
    List<String> pending = new ArrayList<>();
    for (int j = pair.gotKeys.size() - 1; j >= 0; j--) {
      String key = pair.gotKeys.get(j);
      boolean isExtra =
          byKey.getOrDefault(key, List.of()).stream().anyMatch(d -> d.kind() == Kind.EXTRA);
      if (isExtra) {
        pending.add(0, key);
      } else if (kept.contains(key)) {
        extras.put(anchor, pending);
        anchor = key;
        pending = new ArrayList<>();
      }
    }
    extras.put(anchor, pending);
    return extras;
  }

  /** A prefix is reported exactly when its entries differ between the two states. */
  private static void checkSubStates(Pair pair, Supplier<String> ctx) {
    List<String> reported =
        StateComparator.divergentSubStates(state(pair.want), state(pair.got), 1);
    Set<String> prefixes = new HashSet<>();
    pair.wantKeys.forEach(k -> prefixes.add(head(k)));
    pair.gotKeys.forEach(k -> prefixes.add(head(k)));
    Set<String> expected = new HashSet<>();
    for (String prefix : prefixes) {
      if (!under(pair.want, prefix).equals(under(pair.got, prefix))) {
        expected.add(prefix);
      }
    }
    assertThat(reported).as(ctx).doesNotHaveDuplicates();
    assertThat(new HashSet<>(reported)).as(() -> ctx.get() + " sub-states").isEqualTo(expected);
  }

  private static List<Entry> under(List<Entry> entries, String prefix) {
    return entries.stream().filter(e -> head(e.key()).equals(prefix)).toList();
  }

  private static String head(String key) {
    return key.substring(0, key.indexOf('/'));
  }
}
