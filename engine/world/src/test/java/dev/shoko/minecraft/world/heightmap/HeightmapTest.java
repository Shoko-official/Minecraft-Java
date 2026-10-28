package dev.shoko.minecraft.world.heightmap;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.RefPacking;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** CF-R17, CF-R18, CF-R34: one value per column, packed with ceil(log2(height + 1)) bits. */
class HeightmapTest {

  private static final WorldHeight OVERWORLD = Chunks.OVERWORLD;

  @ParameterizedTest
  @CsvSource({
    "-64,384,9,37",
    "0,256,9,37",
    "0,128,8,32",
    "0,16,5,22",
    "0,1,1,4",
    "0,255,8,32",
    "0,511,9,37",
    "0,512,10,43",
    "0,64,7,29"
  })
  void packingWidthAndLengthDependOnTheHeight(int minY, int height, int bits, int longs) {
    // spec: CF-R17, HM4 (b = ceil(log2(height + 1)), ceil(256 / floor(64 / b)) longs)
    WorldHeight world = new WorldHeight(minY, height);

    assertThat(Heightmap.bitsPerColumn(world)).isEqualTo(bits);
    assertThat(Heightmap.longCount(world)).isEqualTo(longs);
  }

  @Test
  void everyDimensionOf263Needs37Longs() {
    // spec: CF-R17 (nine bits, seven values per long, 37 longs)
    assertThat(Heightmap.longCount(OVERWORLD)).isEqualTo(37);
    assertThat(Heightmap.longCount(Chunks.NETHER)).isEqualTo(37);
    assertThat(Heightmap.bitsPerColumn(Chunks.NETHER)).isEqualTo(9);
  }

  @Test
  void filledHeightmapHasTheSameValueInEveryColumn() {
    Heightmap heightmap = Heightmap.filled(OVERWORLD, 4);

    for (int z = 0; z < 16; z++) {
      for (int x = 0; x < 16; x++) {
        assertThat(heightmap.get(x, z)).isEqualTo(4);
      }
    }
  }

  @Test
  void setChangesOnlyOneColumn() {
    Heightmap heightmap = Heightmap.filled(OVERWORLD, 0);

    heightmap.set(3, 5, 200);

    assertThat(heightmap.get(3, 5)).isEqualTo(200);
    assertThat(heightmap.get(5, 3)).isZero();
    assertThat(heightmap.get(4, 5)).isZero();
    assertThat(heightmap.get(3, 6)).isZero();
  }

  @Test
  void columnIndexIsZTimesSixteenPlusX() {
    // spec: CF-R17 (index lz * 16 + lx): column (3, 5) is index 83, long 11, offset 6 * 9
    Heightmap heightmap = Heightmap.filled(OVERWORLD, 0);

    heightmap.set(3, 5, 384);

    long[] packed = heightmap.toLongs();
    assertThat(packed).hasSize(37);
    for (int k = 0; k < 37; k++) {
      assertThat(packed[k]).as("long " + k).isEqualTo(k == 11 ? 384L << 54 : 0L);
    }
  }

  @Test
  void valuesOfZeroAndHeightAreAccepted() {
    // spec: CF-R18 (range 0 to height; 384 means a retained block at y 319)
    Heightmap low = Heightmap.filled(OVERWORLD, 0);
    Heightmap high = Heightmap.filled(OVERWORLD, 384);

    assertThat(low.get(15, 15)).isZero();
    assertThat(high.get(15, 15)).isEqualTo(384);
  }

  @Test
  void valuesOutOfRangeAreRejected() {
    // spec: Heightmap contract (IllegalArgumentException), CF-R18
    Heightmap heightmap = Heightmap.filled(OVERWORLD, 0);

    Failures.expect(IllegalArgumentException.class, () -> Heightmap.filled(OVERWORLD, 385));
    Failures.expect(IllegalArgumentException.class, () -> Heightmap.filled(OVERWORLD, -1));
    Failures.expect(IllegalArgumentException.class, () -> heightmap.set(0, 0, 385));
    Failures.expect(IllegalArgumentException.class, () -> heightmap.set(0, 0, -1));
    Failures.expect(IllegalArgumentException.class, () -> Heightmap.filled(Chunks.NETHER, 257));
  }

  @Test
  void columnsOutOfRangeAreRejected() {
    // spec: Heightmap contract (IndexOutOfBoundsException)
    Heightmap heightmap = Heightmap.filled(OVERWORLD, 0);

    Failures.expect(IndexOutOfBoundsException.class, () -> heightmap.get(16, 0));
    Failures.expect(IndexOutOfBoundsException.class, () -> heightmap.get(0, 16));
    Failures.expect(IndexOutOfBoundsException.class, () -> heightmap.get(-1, 0));
    Failures.expect(IndexOutOfBoundsException.class, () -> heightmap.set(0, -1, 1));
    Failures.expect(IndexOutOfBoundsException.class, () -> heightmap.set(16, 16, 1));
  }

  @Test
  void packedFormOfVectorHm1() {
    // spec: HM1 (256 columns at 1: 36 longs of 0x0040201008040201 then 0x08040201)
    long[] packed = Heightmap.filled(OVERWORLD, 1).toLongs();

    assertThat(packed).hasSize(37);
    for (int k = 0; k < 36; k++) {
      assertThat(packed[k]).isEqualTo(0x0040201008040201L);
    }
    assertThat(packed[36]).isEqualTo(0x0000000008040201L);
  }

  @Test
  void packedFormOfVectorHm2() {
    // spec: HM2 (column i holds min(384, i + 128))
    Heightmap heightmap = Heightmap.filled(OVERWORLD, 0);
    for (int index = 0; index < 256; index++) {
      heightmap.set(index % 16, index / 16, Math.min(384, index + 128));
    }

    long[] packed = heightmap.toLongs();

    assertThat(packed[0]).isEqualTo(0x2190a8441a090280L);
    assertThat(packed[36]).isEqualTo(0x0000000bfdfafb7cL);
  }

  @Test
  void packedFormOfVectorHm3AndOfTheSurfaceAt384() {
    // spec: HM3 (flat world, value 4), HM4 (every column at 384: long 0 0x6030180c06030180)
    assertThat(Heightmap.filled(OVERWORLD, 4).toLongs()[0]).isEqualTo(0x0100804020100804L);
    assertThat(Heightmap.filled(OVERWORLD, 4).toLongs()[36]).isEqualTo(0x0000000020100804L);
    assertThat(Heightmap.filled(OVERWORLD, 384).toLongs()[0]).isEqualTo(0x6030180c06030180L);
  }

  @Test
  void toLongsReturnsAFreshArray() {
    // spec: Heightmap.toLongs contract (a new array)
    Heightmap heightmap = Heightmap.filled(OVERWORLD, 1);

    long[] first = heightmap.toLongs();
    first[0] = 0;

    assertThat(heightmap.toLongs()[0]).isEqualTo(0x0040201008040201L);
  }

  @Test
  void fromLongsReadsWhatToLongsWrote() {
    // spec: CF-R17
    Heightmap heightmap = Heightmap.filled(OVERWORLD, 0);
    for (int index = 0; index < 256; index++) {
      heightmap.set(index % 16, index / 16, (index * 37) % 385);
    }

    Optional<Heightmap> back = Heightmap.fromLongs(heightmap.toLongs(), OVERWORLD);

    assertThat(back).isPresent();
    assertThat(back.orElseThrow().toLongs()).isEqualTo(heightmap.toLongs());
    assertThat(back.orElseThrow().get(7, 9)).isEqualTo(heightmap.get(7, 9));
  }

  @Test
  void fromLongsAcceptsEveryValueUpToTheHeight() {
    // spec: CF-R34 (a value above height makes the type unknown; the height itself is valid)
    int[] values = ChunkNbt.constant(256, 384);

    Optional<Heightmap> read = Heightmap.fromLongs(RefPacking.pack(values, 9), OVERWORLD);

    assertThat(read).isPresent();
    assertThat(read.orElseThrow().get(15, 15)).isEqualTo(384);
  }

  @ParameterizedTest
  @CsvSource({"385", "400", "511"})
  void fromLongsIsEmptyWhenAValueExceedsTheHeight(int tooHigh) {
    // spec: CF-R34, HM4 (385 to 511 fit in 9 bits and make the type unknown)
    int[] values = ChunkNbt.constant(256, 1);
    values[0] = tooHigh;

    assertThat(Heightmap.fromLongs(RefPacking.pack(values, 9), OVERWORLD)).isEmpty();
  }

  @Test
  void fromLongsIsEmptyForTheOldContinuousLength() {
    // spec: CF-R34, HM4 (36 longs is the old continuous packing, invalid in the Overworld)
    long[] thirtySix = Arrays.copyOf(RefPacking.pack(ChunkNbt.constant(256, 1), 9), 36);

    assertThat(Heightmap.fromLongs(thirtySix, OVERWORLD)).isEmpty();
  }

  @Test
  void fromLongsIsEmptyForAnyOtherLength() {
    // spec: CF-R34, HM4 (38 longs, 0 longs)
    long[] thirtyEight = Arrays.copyOf(RefPacking.pack(ChunkNbt.constant(256, 1), 9), 38);

    assertThat(Heightmap.fromLongs(thirtyEight, OVERWORLD)).isEmpty();
    assertThat(Heightmap.fromLongs(new long[0], OVERWORLD)).isEmpty();
  }

  @Test
  void expectedLengthDependsOnTheDimension() {
    // spec: CF-R17, HM4 (height 128 needs 32 longs of 8 bits)
    WorldHeight low = new WorldHeight(0, 128);
    long[] data = RefPacking.pack(ChunkNbt.constant(256, 100), 8);

    assertThat(data).hasSize(32);
    assertThat(Heightmap.fromLongs(data, low)).isPresent();
    assertThat(Heightmap.fromLongs(data, OVERWORLD)).isEmpty();
    assertThat(Heightmap.fromLongs(RefPacking.pack(ChunkNbt.constant(256, 129), 8), low)).isEmpty();
  }

  @Test
  void provisionalUnusedHighBitOfEachLongIsIgnored() {
    // spec: CF-R8 (the unused high bits are ignored at reading, provisional)
    long[] dirty =
        Arrays.stream(RefPacking.pack(ChunkNbt.constant(256, 1), 9))
            .map(value -> value | (1L << 63))
            .toArray();

    Optional<Heightmap> read = Heightmap.fromLongs(dirty, OVERWORLD);

    assertThat(read).isPresent();
    assertThat(read.orElseThrow().get(0, 0)).isEqualTo(1);
    assertThat(read.orElseThrow().get(15, 15)).isEqualTo(1);
  }
}
