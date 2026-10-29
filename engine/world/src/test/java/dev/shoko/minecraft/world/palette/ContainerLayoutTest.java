package dev.shoko.minecraft.world.palette;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Failures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** CF-R7 and CF-R9: bits per entry and number of longs for a palette size. */
class ContainerLayoutTest {

  @Test
  void layoutsHaveTheSizesOfTheSpec() {
    // spec: CF-R2, CF-R5, CF-R7
    assertThat(ContainerLayout.BLOCKS.entryCount()).isEqualTo(4096);
    assertThat(ContainerLayout.BLOCKS.minimumBits()).isEqualTo(4);
    assertThat(ContainerLayout.BLOCKS.maxPaletteSize()).isEqualTo(4096);
    assertThat(ContainerLayout.BIOMES.entryCount()).isEqualTo(64);
    assertThat(ContainerLayout.BIOMES.minimumBits()).isZero();
    assertThat(ContainerLayout.BIOMES.maxPaletteSize()).isEqualTo(64);
  }

  @ParameterizedTest
  @CsvSource({
    "1,0", "2,4", "3,4", "4,4", "15,4", "16,4", "17,5", "31,5", "32,5", "33,6", "64,6", "65,7",
    "128,7", "129,8", "256,8", "257,9", "512,9", "513,10", "1024,10", "1025,11", "2048,11",
    "2049,12", "4096,12"
  })
  void blockBitsAreAtLeastFourAndGrowWithThePalette(int paletteSize, int bits) {
    // spec: CF-R7, CF-R9, PK1 (thresholds 16/17, 32/33, 256/257)
    assertThat(ContainerLayout.BLOCKS.bitsFor(paletteSize)).isEqualTo(bits);
  }

  @ParameterizedTest
  @CsvSource({
    "1,0", "2,1", "3,2", "4,2", "5,3", "8,3", "9,4", "16,4", "17,5", "32,5", "33,6", "64,6"
  })
  void biomeBitsHaveNoMinimum(int paletteSize, int bits) {
    // spec: CF-R7, PK1 (biomes 1 -> 0, 2 -> 1, 3 -> 2, 4 -> 2, 5 -> 3, 8 -> 3, 9 -> 4, 64 -> 6)
    assertThat(ContainerLayout.BIOMES.bitsFor(paletteSize)).isEqualTo(bits);
  }

  @ParameterizedTest
  @CsvSource({
    "1,0",
    "2,256",
    "16,256",
    "17,342",
    "32,342",
    "33,410",
    "128,456",
    "129,512",
    "256,512",
    "257,586",
    "1025,820",
    "4096,820"
  })
  void blockLongCountFollowsThePackingOfTheBits(int paletteSize, int longs) {
    // spec: CF-R8, CF-R9 (4 bits -> 256, 5 -> 342, 6 -> 410, 8 -> 512, 9 -> 586, 12 -> 820)
    assertThat(ContainerLayout.BLOCKS.longCount(paletteSize)).isEqualTo(longs);
  }

  @ParameterizedTest
  @CsvSource({"1,0", "2,1", "3,2", "4,2", "5,4", "8,4", "9,4", "33,7", "64,7"})
  void biomeLongCountFollowsThePackingOfTheBits(int paletteSize, int longs) {
    // spec: CF-R9 (biomes 1 bit -> 1 long, 2 -> 2, 3 -> 4, 6 -> 7)
    assertThat(ContainerLayout.BIOMES.longCount(paletteSize)).isEqualTo(longs);
  }

  @Test
  void paletteSizesOutOfRangeAreRejected() {
    // spec: ContainerLayout contract (size from 1 to maxPaletteSize)
    Failures.expect(IllegalArgumentException.class, () -> ContainerLayout.BLOCKS.bitsFor(0));
    Failures.expect(IllegalArgumentException.class, () -> ContainerLayout.BLOCKS.bitsFor(-1));
    Failures.expect(IllegalArgumentException.class, () -> ContainerLayout.BLOCKS.bitsFor(4097));
    Failures.expect(IllegalArgumentException.class, () -> ContainerLayout.BIOMES.bitsFor(65));
    Failures.expect(IllegalArgumentException.class, () -> ContainerLayout.BLOCKS.longCount(0));
    Failures.expect(IllegalArgumentException.class, () -> ContainerLayout.BLOCKS.longCount(4097));
    Failures.expect(IllegalArgumentException.class, () -> ContainerLayout.BIOMES.longCount(65));
  }
}
