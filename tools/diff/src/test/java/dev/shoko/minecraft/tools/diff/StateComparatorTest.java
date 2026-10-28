package dev.shoko.minecraft.tools.diff;

import static dev.shoko.minecraft.tools.diff.WorldFixture.entry;
import static dev.shoko.minecraft.tools.diff.WorldFixture.indexOf;
import static dev.shoko.minecraft.tools.diff.WorldFixture.state;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.tools.diff.Comparison.Verdict;
import dev.shoko.minecraft.tools.diff.Divergence.Kind;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Each test injects one known defect and checks that it is detected and located first. */
class StateComparatorTest {

  private static final String SECTION_2_4 = "chunk(2,-1)/section(4)";
  private static final String SECTION_2_5 = "chunk(2,-1)/section(5)";

  private static Comparison compareWith(List<CanonicalState.Entry> actual) {
    return StateComparator.compare(WorldFixture.reference(), state(actual));
  }

  @Test
  void identicalStatesAreIdentical() {
    Comparison result = compareWith(WorldFixture.entries());

    assertThat(result.verdict()).isEqualTo(Verdict.IDENTICAL);
    assertThat(result.count()).isZero();
    assertThat(result.first()).isEmpty();
    assertThat(result.actualDigest()).isEqualTo(result.expectedDigest());
    assertThat(result.expectedSize()).isEqualTo(44).isEqualTo(result.actualSize());
  }

  @Test
  void detectsAChangedBlock() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    String key = "chunk(3,-1)/section(4)/block(49,70,-12)";
    int index = indexOf(actual, key);
    assertThat(actual.get(index).value()).isEqualTo("minecraft:air");
    actual.set(index, entry(key, "minecraft:dirt"));

    Comparison result = compareWith(actual);

    assertThat(result.verdict()).isEqualTo(Verdict.DIVERGENT);
    assertThat(result.divergences())
        .containsExactly(
            new Divergence(Kind.VALUE, key, "minecraft:air", "minecraft:dirt", index, index));
    assertThat(result.actualDigest()).isNotEqualTo(result.expectedDigest());
  }

  @Test
  void detectsAShiftedPaletteAndLocatesItsFirstEntry() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    int start = indexOf(actual, SECTION_2_5 + "/palette(0)");
    for (int i = 0; i < WorldFixture.PALETTE.length; i++) {
      actual.set(
          start + i, entry(SECTION_2_5 + "/palette(" + (i + 1) + ")", WorldFixture.PALETTE[i]));
    }

    Comparison result = compareWith(actual);

    assertThat(result.verdict()).isEqualTo(Verdict.DIVERGENT);
    assertThat(result.first())
        .contains(
            new Divergence(
                Kind.MISSING,
                SECTION_2_5 + "/palette(0)",
                "minecraft:stone",
                Divergence.ABSENT,
                start,
                -1));
    assertThat(result.divergences())
        .extracting(Divergence::kind)
        .containsExactly(Kind.MISSING, Kind.VALUE, Kind.VALUE, Kind.EXTRA);
    assertThat(result.divergences().get(1))
        .isEqualTo(
            new Divergence(
                Kind.VALUE,
                SECTION_2_5 + "/palette(1)",
                "minecraft:dirt",
                "minecraft:stone",
                start + 1,
                start));
    assertThat(result.divergences().get(3).key()).isEqualTo(SECTION_2_5 + "/palette(3)");
  }

  @Test
  void detectsAMissingChunk() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    int before = actual.size();
    actual.removeIf(e -> StateKey.isUnder(e.key(), "chunk(3,-1)"));
    int removed = before - actual.size();

    Comparison result = compareWith(actual);

    assertThat(removed).isEqualTo(22);
    assertThat(result.count()).isEqualTo(removed);
    assertThat(result.divergences()).allMatch(d -> d.kind() == Kind.MISSING);
    assertThat(result.first().orElseThrow().key()).isEqualTo("chunk(3,-1)/section(4)/palette(0)");
    assertThat(StateComparator.divergentSubStates(WorldFixture.reference(), state(actual), 1))
        .containsExactly("chunk(3,-1)");
  }

  @Test
  void detectsAnExtraEntryAtItsPosition() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    String key = SECTION_2_4 + "/block(35,64,-16)";
    int position = indexOf(actual, SECTION_2_4 + "/block(33,70,-12)") + 1;
    actual.add(position, entry(key, "minecraft:stone"));

    Comparison result = compareWith(actual);

    assertThat(result.divergences())
        .containsExactly(
            new Divergence(Kind.EXTRA, key, Divergence.ABSENT, "minecraft:stone", -1, position));
    assertThat(result.actualSize()).isEqualTo(result.expectedSize() + 1);
  }

  @Test
  void detectsTheSameValuesInAnotherOrder() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    int first = indexOf(actual, SECTION_2_4 + "/block(32,71,-12)");
    Collections.swap(actual, first, first + 1);

    Comparison result = compareWith(actual);

    assertThat(new HashSet<>(state(actual).entries()))
        .isEqualTo(new HashSet<>(WorldFixture.reference().entries()));
    assertThat(result.verdict()).isEqualTo(Verdict.DIVERGENT);
    assertThat(result.divergences())
        .containsExactly(
            new Divergence(
                Kind.ORDER,
                SECTION_2_4 + "/block(32,71,-12)",
                SECTION_2_4 + "/block(32,71,-12)",
                SECTION_2_4 + "/block(33,71,-12)",
                first,
                first));
    assertThat(result.actualDigest()).isNotEqualTo(result.expectedDigest());
  }

  @Test
  void detectsSwappedChunksAsAnOrderDivergence() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    Collections.rotate(actual, 22);

    Comparison result = compareWith(actual);

    assertThat(result.verdict()).isEqualTo(Verdict.DIVERGENT);
    assertThat(result.divergences()).allMatch(d -> d.kind() == Kind.ORDER);
    assertThat(result.first().orElseThrow().key()).isEqualTo(SECTION_2_4 + "/palette(0)");
    assertThat(result.first().orElseThrow().actual())
        .isEqualTo("chunk(3,-1)/section(4)/palette(0)");
  }

  @Test
  void reportsTheEarliestOfSeveralDefectsFirst() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    String late = "chunk(3,-1)/section(5)/block(48,86,-12)";
    actual.set(indexOf(actual, late), entry(late, "minecraft:gold_block"));
    String early = SECTION_2_5 + "/block(33,87,-11)";
    actual.set(indexOf(actual, early), entry(early, "minecraft:gold_block"));

    Comparison result = compareWith(actual);

    assertThat(result.count()).isEqualTo(2);
    assertThat(result.first().orElseThrow().key()).isEqualTo(early);
    assertThat(result.divergences().get(1).key()).isEqualTo(late);
  }

  @Test
  void reportsAMissingKeyBeforeTheExtraKeyThatReplacesIt() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    String key = SECTION_2_4 + "/palette(1)";
    actual.set(indexOf(actual, key), entry(SECTION_2_4 + "/palette(9)", "minecraft:dirt"));

    Comparison result = compareWith(actual);

    assertThat(result.divergences())
        .extracting(Divergence::kind)
        .containsExactly(Kind.MISSING, Kind.EXTRA);
    assertThat(result.first().orElseThrow().key()).isEqualTo(key);
  }

  @Test
  void comparingTwiceGivesTheSameResult() {
    List<CanonicalState.Entry> actual = WorldFixture.entries();
    Collections.reverse(actual);
    actual.remove(3);

    assertThat(compareWith(actual)).isEqualTo(compareWith(actual));
  }
}
