package dev.shoko.minecraft.world.palette;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.RefPacking;
import java.util.Arrays;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** CF-R8, CF-R9, CF-R17 and vectors PK1 to PK5, HM1 to HM3 of world-vectors.md. */
class BitPackingTest {

  @ParameterizedTest
  @CsvSource({
    "1,64", "2,32", "3,21", "4,16", "5,12", "6,10", "7,9", "8,8", "9,7", "10,6", "11,5", "12,5",
    "13,4", "16,4", "17,3", "21,3", "22,2", "32,2"
  })
  void entriesPerLongIsTheIntegerPartOf64OverBits(int bits, int perLong) {
    // spec: CF-R8, PK1
    assertThat(BitPacking.entriesPerLong(bits)).isEqualTo(perLong);
  }

  @ParameterizedTest
  @CsvSource({
    "1,64,1",
    "2,128,2",
    "3,196,4",
    "4,256,4",
    "5,342,6",
    "6,410,7",
    "7,456,8",
    "8,512,8",
    "9,586,10",
    "10,683,11",
    "11,820,13",
    "12,820,13",
    "16,1024,16"
  })
  void longCountMatchesTheTableOfThePackingVector(int bits, int blocks, int biomes) {
    // spec: CF-R8, CF-R9, PK1 (4096 block entries, 64 biome entries)
    assertThat(BitPacking.longCount(4096, bits)).isEqualTo(blocks);
    assertThat(BitPacking.longCount(64, bits)).isEqualTo(biomes);
  }

  @Test
  void longCountOf256ColumnsMatchesTheTableOfThePackingVector() {
    // spec: CF-R17, PK1 (256 entries: 9 bits give 37 longs, 5 bits give 22)
    assertThat(BitPacking.longCount(256, 9)).isEqualTo(37);
    assertThat(BitPacking.longCount(256, 5)).isEqualTo(22);
    assertThat(BitPacking.longCount(256, 1)).isEqualTo(4);
    assertThat(BitPacking.longCount(256, 16)).isEqualTo(64);
  }

  @Test
  void longCountOfNothingIsZeroAndOfOneEntryIsOne() {
    // spec: CF-R8 (ceil(count / p))
    assertThat(BitPacking.longCount(0, 5)).isZero();
    assertThat(BitPacking.longCount(1, 5)).isEqualTo(1);
    assertThat(BitPacking.longCount(12, 5)).isEqualTo(1);
    assertThat(BitPacking.longCount(13, 5)).isEqualTo(2);
  }

  @Test
  void sixteenFifteensAtFourBitsMakeMinusOne() {
    // spec: CF-R8 ("seul le motif de bits compte")
    long[] packed = BitPacking.pack(ChunkNbt.constant(16, 15), 4);

    assertThat(packed).containsExactly(-1L);
  }

  @Test
  void packFollowsVectorPk2() {
    // spec: PK2 (value i & 15 at 4 bits: every long is 0xfedcba9876543210)
    long[] packed = BitPacking.pack(RefPacking.cycle(4096, 16), 4);

    assertThat(packed).hasSize(256).containsOnly(0xfedcba9876543210L);
    assertThat(packed[0]).isEqualTo(-81985529216486896L);
  }

  @Test
  void packFollowsVectorPk3WithoutOverlap() {
    // spec: PK3 (value i mod 17 at 5 bits: 342 longs, an entry never spans two longs)
    long[] packed = BitPacking.pack(RefPacking.cycle(4096, 17), 5);

    assertThat(packed).hasSize(342);
    assertThat(packed[0]).isEqualTo(0x05a928398a418820L);
    assertThat(packed[1]).isEqualTo(0x031483104107b9acL);
    assertThat(packed[341]).isEqualTo(0x000000000007b9acL);
  }

  @Test
  void entryTwelveStartsAtTheLowBitsOfTheSecondLong() {
    // spec: "Cas limites" of chunk-format (with 5 bits entry 12 starts at bit 0 of long 1)
    int[] values = new int[24];
    values[12] = 31;
    values[11] = 31;

    long[] packed = BitPacking.pack(values, 5);

    assertThat(packed).hasSize(2);
    assertThat(packed[0]).isEqualTo(31L << 55);
    assertThat(packed[1]).isEqualTo(31L);
  }

  @Test
  void packFollowsVectorPk4() {
    // spec: PK4 (one layer of 256 entries at value 1, 4 bits)
    int[] values = new int[4096];
    Arrays.fill(values, 0, 256, 1);

    long[] packed = BitPacking.pack(values, 4);

    for (int k = 0; k < 16; k++) {
      assertThat(packed[k]).as("long " + k).isEqualTo(0x1111111111111111L);
    }
    for (int k = 16; k < 256; k++) {
      assertThat(packed[k]).as("long " + k).isZero();
    }
  }

  @Test
  void packFollowsVectorsPk5() {
    // spec: PK5a, PK5b, PK5c (biome cells)
    assertThat(BitPacking.pack(RefPacking.cycle(64, 5), 3))
        .containsExactly(0x08d111a223444688L, 0x111a2234446888d1L, 0x22234446888d111aL, 3L);
    int[] upperHalf = new int[64];
    Arrays.fill(upperHalf, 32, 64, 1);
    assertThat(BitPacking.pack(upperHalf, 1)).containsExactly(0xffffffff00000000L);
    int[] westHalf = new int[64];
    for (int i = 0; i < 64; i++) {
      westHalf[i] = (i & 3) < 2 ? 1 : 0;
    }
    assertThat(BitPacking.pack(westHalf, 1)).containsExactly(0x3333333333333333L);
  }

  @Test
  void packFollowsVectorsHm1ToHm3() {
    // spec: HM1, HM2, HM3 (256 columns at 9 bits: 37 longs, 1 unused bit per long)
    long[] ones = BitPacking.pack(ChunkNbt.constant(256, 1), 9);
    assertThat(ones).hasSize(37);
    assertThat(ones[0]).isEqualTo(0x0040201008040201L);
    assertThat(ones[35]).isEqualTo(0x0040201008040201L);
    assertThat(ones[36]).isEqualTo(0x0000000008040201L);

    int[] ramp = new int[256];
    for (int i = 0; i < 256; i++) {
      ramp[i] = Math.min(384, i + 128);
    }
    long[] ramped = BitPacking.pack(ramp, 9);
    assertThat(ramped[0]).isEqualTo(0x2190a8441a090280L);
    assertThat(ramped[36]).isEqualTo(0x0000000bfdfafb7cL);

    long[] fours = BitPacking.pack(ChunkNbt.constant(256, 4), 9);
    assertThat(fours[0]).isEqualTo(0x0100804020100804L);
    assertThat(fours[36]).isEqualTo(0x0000000020100804L);
  }

  @ParameterizedTest
  @CsvSource({"3,0x7fffffffffffffff", "5,0x0fffffffffffffff", "9,0x7fffffffffffffff"})
  void unusedHighBitsAreZeroWhenWriting(int bits, String expectedLong) {
    // spec: CF-R8 (the unused high bits are 0 at writing)
    int perLong = 64 / bits;
    int max = (1 << bits) - 1;

    long[] packed = BitPacking.pack(ChunkNbt.constant(perLong * 2, max), bits);

    long expected = Long.parseUnsignedLong(expectedLong.substring(2), 16);
    assertThat(packed).containsExactly(expected, expected);
  }

  @Test
  void unpackIsTheInverseOfPack() {
    // spec: CF-R8, PK3
    int[] values = RefPacking.cycle(4096, 17);

    assertThat(BitPacking.unpack(RefPacking.pack(values, 5), 4096, 5)).isEqualTo(values);
    assertThat(BitPacking.unpack(new long[] {0x3333333333333333L}, 64, 1))
        .hasSize(64)
        .startsWith(1, 1, 0, 0, 1, 1, 0, 0);
  }

  @Test
  void provisionalUnusedHighBitsAreIgnoredWhenReading() {
    // spec: CF-R8 (the unused high bits are ignored at reading, provisional)
    int[] values = RefPacking.cycle(4096, 17);
    long[] packed = RefPacking.pack(values, 5);
    long[] dirty = Arrays.stream(packed).map(v -> v | 0xF000000000000000L).toArray();

    assertThat(BitPacking.unpack(dirty, 4096, 5)).isEqualTo(values);
  }

  @Test
  void everyWidthRoundTripsSeededValues() {
    // spec: CF-R8 (bits from 1 to 32)
    SplittableRandom random = new SplittableRandom(20261028L);
    for (int bits = 1; bits <= 32; bits++) {
      int count = 1 + random.nextInt(300);
      int bound = bits >= 31 ? Integer.MAX_VALUE : 1 << bits;
      int[] values = new int[count];
      for (int k = 0; k < count; k++) {
        values[k] = random.nextInt(bound);
      }
      long[] packed = BitPacking.pack(values, bits);
      assertThat(packed).as("bits " + bits).hasSize(RefPacking.longCount(count, bits));
      assertThat(packed).as("bits " + bits).isEqualTo(RefPacking.pack(values, bits));
      assertThat(BitPacking.unpack(packed, count, bits)).as("bits " + bits).isEqualTo(values);
    }
  }

  @Test
  void bitsOutOfRangeAreRejected() {
    // spec: BitPacking contract (bits from 1 to 32, IllegalArgumentException)
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.entriesPerLong(0));
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.entriesPerLong(33));
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.entriesPerLong(-4));
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.longCount(10, 0));
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.longCount(10, 33));
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.pack(new int[4], 0));
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.unpack(new long[1], 4, 33));
  }

  @Test
  void negativeEntryCountIsRejected() {
    // spec: BitPacking.longCount contract (entryCount not negative)
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.longCount(-1, 4));
  }

  @Test
  void valuesThatDoNotFitAreRejected() {
    // spec: BitPacking.pack contract (negative or wider than bits)
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.pack(new int[] {16}, 4));
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.pack(new int[] {0, -1}, 4));
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.pack(new int[] {512}, 9));
    Failures.expect(
        IllegalArgumentException.class, () -> BitPacking.pack(new int[] {Integer.MIN_VALUE}, 32));
  }

  @Test
  void dataOfTheWrongLengthIsRejected() {
    // spec: BitPacking.unpack contract (data.length must be longCount(entryCount, bits))
    Failures.expect(
        IllegalArgumentException.class, () -> BitPacking.unpack(new long[255], 4096, 4));
    Failures.expect(
        IllegalArgumentException.class, () -> BitPacking.unpack(new long[257], 4096, 4));
    Failures.expect(
        IllegalArgumentException.class, () -> BitPacking.unpack(new long[320], 4096, 5));
    Failures.expect(IllegalArgumentException.class, () -> BitPacking.unpack(new long[1], 0, 5));
  }
}
