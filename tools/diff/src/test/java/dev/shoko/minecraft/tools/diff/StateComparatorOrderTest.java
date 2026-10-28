package dev.shoko.minecraft.tools.diff;

import static dev.shoko.minecraft.tools.diff.WorldFixture.entry;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.tools.diff.Divergence.Kind;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins the order of the divergence list: document order of the expected state, an extra entry just
 * before the next key kept in place, ORDER before VALUE for the same key. The first divergence of a
 * report is the first of this list, so a wrong order points the operator at the wrong place.
 */
class StateComparatorOrderTest {

  private static final String EXTRA = "chunk(2,-1)/section(4)/block(40,64,-16)";

  /** Items are "key" (value "v") or "key=value". */
  private static CanonicalState state(String... items) {
    CanonicalState.Builder builder = CanonicalState.builder();
    for (String item : items) {
      int equals = item.indexOf('=');
      builder.add(
          equals < 0 ? item : item.substring(0, equals),
          equals < 0 ? "v" : item.substring(equals + 1));
    }
    return builder.build();
  }

  private static List<String> keys(Comparison comparison) {
    return comparison.divergences().stream().map(Divergence::key).toList();
  }

  private static List<Kind> kinds(Comparison comparison) {
    return comparison.divergences().stream().map(Divergence::kind).toList();
  }

  @Test
  void placesAnExtraEntryBeforeAValueDefectFiveKeysLater() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    actual.add(10, entry(EXTRA, "minecraft:stone"));
    String late = WorldFixture.entries().get(14).key();
    actual.set(15, entry(late, "minecraft:gold_block"));

    Comparison result =
        StateComparator.compare(WorldFixture.reference(), WorldFixture.state(actual));

    assertThat(keys(result)).containsExactly(EXTRA, late);
    assertThat(kinds(result)).containsExactly(Kind.EXTRA, Kind.VALUE);
  }

  @Test
  void placesAnExtraEntryBeforeTheValueDefectOfTheKeyThatFollowsIt() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    String next = WorldFixture.entries().get(10).key();
    actual.set(10, entry(next, "minecraft:gold_block"));
    actual.add(10, entry(EXTRA, "minecraft:stone"));

    Comparison result =
        StateComparator.compare(WorldFixture.reference(), WorldFixture.state(actual));

    assertThat(keys(result)).containsExactly(EXTRA, next);
    assertThat(kinds(result)).containsExactly(Kind.EXTRA, Kind.VALUE);
  }

  @Test
  void placesAnExtraEntryAfterTheValueDefectOfTheKeyThatPrecedesIt() {
    Comparison result = StateComparator.compare(state("a", "b", "c"), state("a=x", "n", "b", "c"));

    assertThat(keys(result)).containsExactly("a", "n");
    assertThat(kinds(result)).containsExactly(Kind.VALUE, Kind.EXTRA);
  }

  @Test
  void reportsTheOrderDivergenceOfAKeyBeforeItsValueDivergence() {
    Comparison result = StateComparator.compare(state("a", "b", "c"), state("b", "c", "a=changed"));

    assertThat(keys(result)).containsExactly("a", "a");
    assertThat(kinds(result)).containsExactly(Kind.ORDER, Kind.VALUE);
  }

  @Test
  void keepsConsecutiveExtraEntriesInTheirActualOrder() {
    Comparison result = StateComparator.compare(state("a", "b"), state("a", "x", "y", "b"));

    assertThat(keys(result)).containsExactly("x", "y");
  }

  @Test
  void keepsConsecutiveExtraEntriesInTheirActualOrderWhateverTheirKeys() {
    Comparison result = StateComparator.compare(state("a", "b"), state("a", "y", "x", "b"));

    assertThat(keys(result)).containsExactly("y", "x");
  }

  @Test
  void keepsTrailingExtraEntriesInTheirActualOrder() {
    Comparison result = StateComparator.compare(state("a"), state("a", "y", "x"));

    assertThat(keys(result)).containsExactly("y", "x");
  }

  @Test
  void placesAnExtraEntryAfterAKeyMovedToTheEndOfTheState() {
    Comparison result = StateComparator.compare(state("a", "b", "c"), state("b", "c", "x", "a"));

    assertThat(keys(result)).containsExactly("a", "x");
    assertThat(kinds(result)).containsExactly(Kind.ORDER, Kind.EXTRA);
  }

  @Test
  void placesAnExtraEntryAfterTheMovedKeyThatPrecedesItsAnchorInTheExpectedState() {
    Comparison result = StateComparator.compare(state("a", "b", "c"), state("x", "b", "c", "a"));

    assertThat(keys(result)).containsExactly("a", "x");
    assertThat(kinds(result)).containsExactly(Kind.ORDER, Kind.EXTRA);
  }

  @Test
  void anchorsAnExtraEntryOnTheNextKeptKeyNotOnAMovedOne() {
    Comparison result =
        StateComparator.compare(state("a", "b", "c", "d"), state("b", "x", "a", "c", "d"));

    assertThat(keys(result)).containsExactly("a", "x");
  }
}
