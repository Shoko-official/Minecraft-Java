package dev.shoko.minecraft.world.palette;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Failures;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CF-R6 and CF-R12: the in-memory container is free, its observable behavior is not. */
class PalettedContainerTest {

  @Test
  void filledContainerHoldsTheSameValueEverywhere() {
    // spec: CF-R6 (a palette of one entry fills the container)
    PalettedContainer<String> container = PalettedContainer.filled(64, "a");

    assertThat(container.size()).isEqualTo(64);
    for (int i = 0; i < 64; i++) {
      assertThat(container.get(i)).isEqualTo("a");
    }
    assertThat(container.isUniform()).isTrue();
    assertThat(container.distinctValues()).containsExactly("a");
  }

  @Test
  void setReturnsThePreviousValue() {
    PalettedContainer<String> container = PalettedContainer.filled(64, "a");

    assertThat(container.set(5, "b")).isEqualTo("a");
    assertThat(container.set(5, "c")).isEqualTo("b");
    assertThat(container.get(5)).isEqualTo("c");
    assertThat(container.get(4)).isEqualTo("a");
    assertThat(container.get(6)).isEqualTo("a");
  }

  @Test
  void containerIsNoLongerUniformAfterADifferentValue() {
    PalettedContainer<String> container = PalettedContainer.filled(64, "a");

    container.set(63, "b");

    assertThat(container.isUniform()).isFalse();
    container.set(63, "a");
    assertThat(container.isUniform()).isTrue();
  }

  @Test
  void distinctValuesFollowTheFirstOccurrenceByPosition() {
    // spec: PalettedContainer.distinctValues contract (order of first occurrence by position)
    PalettedContainer<String> container = PalettedContainer.filled(64, "a");
    container.set(10, "c");
    container.set(3, "b");
    container.set(40, "b");
    container.set(20, "d");

    assertThat(container.distinctValues()).containsExactly("a", "b", "c", "d");
  }

  @Test
  void valueNoLongerHeldLeavesTheDistinctValues() {
    // spec: PalettedContainer.distinctValues contract (no unused entry), CF-R7 (compacted)
    PalettedContainer<String> container = PalettedContainer.filled(64, "a");
    container.set(0, "b");
    container.set(0, "a");

    assertThat(container.distinctValues()).containsExactly("a");
  }

  @Test
  void distinctValuesCannotBeModified() {
    // spec: PalettedContainer.distinctValues contract (unmodifiable)
    List<String> values = PalettedContainer.filled(64, "a").distinctValues();

    Failures.expect(UnsupportedOperationException.class, () -> values.add("b"));
  }

  @Test
  void positionsOutOfRangeAreRejected() {
    // spec: PalettedContainer contract (IndexOutOfBoundsException)
    PalettedContainer<String> container = PalettedContainer.filled(64, "a");

    Failures.expect(IndexOutOfBoundsException.class, () -> container.get(-1));
    Failures.expect(IndexOutOfBoundsException.class, () -> container.get(64));
    Failures.expect(IndexOutOfBoundsException.class, () -> container.set(-1, "b"));
    Failures.expect(IndexOutOfBoundsException.class, () -> container.set(64, "b"));
  }

  @Test
  void copyIsIndependent() {
    // spec: PalettedContainer.copy contract
    PalettedContainer<String> container = PalettedContainer.filled(64, "a");
    container.set(1, "b");

    PalettedContainer<String> copy = container.copy();
    copy.set(1, "z");
    container.set(2, "y");

    assertThat(copy.get(1)).isEqualTo("z");
    assertThat(copy.get(2)).isEqualTo("a");
    assertThat(container.get(1)).isEqualTo("b");
    assertThat(copy.size()).isEqualTo(64);
  }

  @Test
  void largePaletteKeepsEveryPosition() {
    // spec: CF-R7 (up to 4096 distinct values in 4096 positions)
    PalettedContainer<Integer> container = PalettedContainer.filled(4096, 0);
    for (int i = 0; i < 4096; i++) {
      container.set(i, i);
    }

    for (int i = 0; i < 4096; i++) {
      assertThat(container.get(i)).isEqualTo(i);
    }
    assertThat(container.distinctValues()).hasSize(4096);
  }
}
