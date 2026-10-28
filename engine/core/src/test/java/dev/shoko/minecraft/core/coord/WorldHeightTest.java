package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WorldHeightTest {

  // spec: core-math R25 (D): Overworld and overworld_caves, min_y -64, height 384
  private static final WorldHeight OVERWORLD = new WorldHeight(-64, 384);

  // spec: core-math R25 (D): the Nether and the End, min_y 0, height 256
  private static final WorldHeight NETHER = new WorldHeight(0, 256);

  // spec: core-math R25: buildable blocks -64..319, sections -4..19, 24 sections
  @Test
  void overworldSpansBlocksMinus64To319In24Sections() {
    assertThat(OVERWORLD.maxY()).isEqualTo(319);
    assertThat(OVERWORLD.minSectionY()).isEqualTo(-4);
    assertThat(OVERWORLD.maxSectionY()).isEqualTo(19);
    assertThat(OVERWORLD.sectionCount()).isEqualTo(24);
  }

  // spec: core-math R25: Nether and End span 0..255 in 16 sections
  @Test
  void netherSpansBlocks0To255In16Sections() {
    assertThat(NETHER.maxY()).isEqualTo(255);
    assertThat(NETHER.minSectionY()).isZero();
    assertThat(NETHER.maxSectionY()).isEqualTo(15);
    assertThat(NETHER.sectionCount()).isEqualTo(16);
  }

  // spec: core-math R25, I/O table: y in [-64, 319] for the Overworld, bounds inclusive
  @ParameterizedTest(name = "Overworld contains y = {0}: {1}")
  @CsvSource({
    "-65, false",
    "-64, true",
    "0, true",
    "319, true",
    "320, false",
    "-2147483648, false",
    "2147483647, false"
  })
  void overworldContainsInclusiveRange(int y, boolean contained) {
    assertThat(OVERWORLD.contains(y)).isEqualTo(contained);
  }

  // spec: core-math R25: Nether bounds inclusive
  @ParameterizedTest(name = "Nether contains y = {0}: {1}")
  @CsvSource({"-1, false", "0, true", "255, true", "256, false"})
  void netherContainsInclusiveRange(int y, boolean contained) {
    assertThat(NETHER.contains(y)).isEqualTo(contained);
  }

  // spec: core-math R25, V4 column "list index": sy - (min_y >> 4), not checked against bounds
  @ParameterizedTest(name = "Overworld section {0} has list index {1}")
  @CsvSource({"-4, 0", "-1, 3", "0, 4", "3, 7", "19, 23", "-5, -1", "20, 24"})
  void overworldSectionIndexIsOffsetByMinSection(int sectionY, int index) {
    assertThat(OVERWORLD.sectionIndex(sectionY)).isEqualTo(index);
  }

  // spec: core-math R25 edge case: block (-1, -64, -1) is in section -4, index 0
  @Test
  void lowestOverworldBlockIsInSectionIndexZero() {
    int sectionY = new BlockPos(-1, -64, -1).section().y();
    assertThat(OVERWORLD.sectionIndex(sectionY)).isZero();
    assertThat(NETHER.sectionIndex(0)).isZero();
    assertThat(NETHER.sectionIndex(15)).isEqualTo(15);
  }

  // records: value semantics
  @Test
  void isAValueObject() {
    assertThat(new WorldHeight(-64, 384))
        .isEqualTo(OVERWORLD)
        .hasSameHashCodeAs(OVERWORLD)
        .isNotEqualTo(NETHER);
    assertThat(OVERWORLD.minY()).isEqualTo(-64);
    assertThat(OVERWORLD.height()).isEqualTo(384);
  }
}
