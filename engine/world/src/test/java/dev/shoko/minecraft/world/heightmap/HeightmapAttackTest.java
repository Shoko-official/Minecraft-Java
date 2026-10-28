package dev.shoko.minecraft.world.heightmap;

import static dev.shoko.minecraft.world.support.Nbt.longs;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.RefPacking;
import dev.shoko.minecraft.world.support.TreeMutator;
import dev.shoko.minecraft.world.support.Warnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Seeded attack on {@link Heightmap} and {@link Heightmaps} (CF-R17, CF-R34). */
class HeightmapAttackTest {

  static Stream<Long> seeds() {
    return LongStream.range(0, 60).boxed();
  }

  private static long[] randomLongs(SplittableRandom random, int length) {
    long[] data = new long[length];
    for (int k = 0; k < length; k++) {
      data[k] = random.nextLong();
    }
    return data;
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void anyLongsAreEitherAValidHeightmapOrUnknownNeverAnException(long seed) {
    // spec: CF-R34 (a wrong length or a value above height: unknown, never an exception)
    SplittableRandom random = new SplittableRandom(seed);
    WorldHeight[] heights = {
      Chunks.OVERWORLD, Chunks.NETHER, new WorldHeight(0, 128), new WorldHeight(-64, 16)
    };
    for (WorldHeight height : heights) {
      int expected = Heightmap.longCount(height);
      for (int length : new int[] {expected, expected, expected - 1, expected + 1, 0, 1}) {
        long[] data = randomLongs(random, length);

        Optional<Heightmap> read = Heightmap.fromLongs(data, height);

        if (read.isPresent()) {
          assertThat(length).isEqualTo(expected);
          for (int index = 0; index < 256; index++) {
            assertThat(read.get().get(index % 16, index / 16)).isBetween(0, height.height());
          }
          assertThat(Heightmap.fromLongs(read.get().toLongs(), height)).isPresent();
        }
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void validValuesAlwaysRoundTrip(long seed) {
    // spec: CF-R17 (pack then read gives the same 256 values, for every legal value)
    SplittableRandom random = new SplittableRandom(1_000 + seed);
    WorldHeight height =
        random.nextBoolean() ? Chunks.OVERWORLD : new WorldHeight(0, 1 + random.nextInt(600));
    int[] values = new int[256];
    for (int k = 0; k < 256; k++) {
      values[k] = random.nextInt(height.height() + 1);
    }
    Heightmap heightmap = Heightmap.filled(height, 0);
    for (int k = 0; k < 256; k++) {
      heightmap.set(k % 16, k / 16, values[k]);
    }

    long[] packed = heightmap.toLongs();

    assertThat(packed).hasSize(Heightmap.longCount(height));
    assertThat(packed).isEqualTo(RefPacking.pack(values, Heightmap.bitsPerColumn(height)));
    Heightmap back = Heightmap.fromLongs(packed, height).orElseThrow();
    for (int k = 0; k < 256; k++) {
      assertThat(back.get(k % 16, k / 16)).isEqualTo(values[k]);
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void damagedHeightmapCompoundsNeverThrowAndOnlyWarnWithTheirCode(long seed) {
    // spec: CF-R34 (present but invalid: one HEIGHTMAP_INVALID, absent: none; no exception)
    SplittableRandom random = new SplittableRandom(2_000 + seed);
    TreeMutator mutator = new TreeMutator(seed);
    Map<String, NbtTag> entries = new LinkedHashMap<>();
    long[] valid = RefPacking.pack(new int[256], 9);
    for (HeightmapType type : HeightmapType.values()) {
      if (random.nextInt(4) != 0) {
        entries.put(type.name(), longs(valid));
      }
    }
    CompoundTag compound =
        mutator.mutate(CompoundTag.of(new LinkedHashMap<>(entries)), 1 + random.nextInt(4));
    Warnings warnings = new Warnings();

    Heightmaps read = Heightmaps.fromNbt(compound, Chunks.OVERWORLD, warnings);

    assertThat(warnings.codes()).isSubsetOf(WarningCode.HEIGHTMAP_INVALID);
    assertThat(warnings.codes().size()).isLessThanOrEqualTo(6);
    for (HeightmapType type : HeightmapType.values()) {
      assertThat(read.get(type).isEmpty()).isEqualTo(read.unknownTypes().contains(type));
    }
    CompoundTag written = read.toNbt();
    assertThat(written.entries().keySet())
        .doesNotContainAnyElementsOf(read.unknownTypes().stream().map(Enum::name).toList());
    Warnings second = new Warnings();
    Heightmaps again = Heightmaps.fromNbt(written, Chunks.OVERWORLD, second);
    assertThat(second.isEmpty()).isTrue();
    assertThat(again.unknownTypes()).isEqualTo(read.unknownTypes());
    assertThat(again.toNbt()).isEqualTo(written);
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void randomTagsAsTheWholeCompoundDoNotThrow(long seed) {
    // spec: CF-R19, CF-R34 (a type of any NBT kind)
    TreeMutator mutator = new TreeMutator(3_000 + seed);
    Map<String, NbtTag> entries = new LinkedHashMap<>();
    for (HeightmapType type : HeightmapType.values()) {
      entries.put(type.name(), mutator.randomTag(2));
    }

    Heightmaps read =
        Heightmaps.fromNbt(
            CompoundTag.of(new LinkedHashMap<>(entries)), Chunks.OVERWORLD, new Warnings());

    assertThat(read.unknownTypes().size()).isLessThanOrEqualTo(6);
  }
}
