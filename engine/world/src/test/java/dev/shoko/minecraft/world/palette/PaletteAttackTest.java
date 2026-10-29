package dev.shoko.minecraft.world.palette;

import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.longs;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.Nbt;
import dev.shoko.minecraft.world.support.RefPacking;
import dev.shoko.minecraft.world.support.TestEntries;
import dev.shoko.minecraft.world.support.TreeMutator;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Seeded attack on bit packing and on the container codec (CF-R6 to CF-R10). */
class PaletteAttackTest {

  static Stream<Long> seeds() {
    return LongStream.range(0, 50).boxed();
  }

  private static PalettedContainerCodec<Integer> blocks() {
    return PalettedContainerCodec.of(ContainerLayout.BLOCKS, TestEntries.ints());
  }

  private static PalettedContainerCodec<Integer> biomes() {
    return PalettedContainerCodec.of(ContainerLayout.BIOMES, TestEntries.ints());
  }

  private static CompoundTag container(
      int paletteSize, int count, int bits, SplittableRandom random) {
    List<NbtTag> palette = new ArrayList<>();
    for (int k = 0; k < paletteSize; k++) {
      palette.add(i(k * 3));
    }
    int[] indices = new int[count];
    for (int k = 0; k < count; k++) {
      indices[k] = random.nextInt(paletteSize);
    }
    return compound("palette", Nbt.listOf(palette), "data", longs(RefPacking.pack(indices, bits)));
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void packThenUnpackIsTheIdentityForAnyWidthAndCount(long seed) {
    // spec: CF-R8 (pack and unpack are inverse, whatever the width from 1 to 32)
    SplittableRandom random = new SplittableRandom(seed);
    for (int round = 0; round < 20; round++) {
      int bits = 1 + random.nextInt(32);
      int count = random.nextInt(700);
      int[] values = new int[count];
      for (int k = 0; k < count; k++) {
        values[k] = bits >= 31 ? random.nextInt(Integer.MAX_VALUE) : random.nextInt(1 << bits);
      }

      long[] packed = BitPacking.pack(values, bits);

      assertThat(packed).hasSize(BitPacking.longCount(count, bits));
      assertThat(BitPacking.unpack(packed, count, bits)).isEqualTo(values);
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void unpackOfAnyLongsStaysInRange(long seed) {
    // spec: BitPacking.unpack contract (each entry in [0, 2^bits)), CF-R8 (unused bits ignored)
    SplittableRandom random = new SplittableRandom(1_000 + seed);
    for (int round = 0; round < 20; round++) {
      int bits = 1 + random.nextInt(32);
      int count = random.nextInt(500);
      long[] data = new long[BitPacking.longCount(count, bits)];
      for (int k = 0; k < data.length; k++) {
        data[k] = random.nextLong();
      }

      int[] values = BitPacking.unpack(data, count, bits);

      long bound = 1L << bits;
      for (int value : values) {
        assertThat((long) value).isBetween(0L, bound - 1);
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void unpackWithAWrongLengthOrBitsIsRejectedWithAnIllegalArgument(long seed) {
    // spec: BitPacking contract (IllegalArgumentException, nothing else)
    SplittableRandom random = new SplittableRandom(2_000 + seed);
    int bits = 1 + random.nextInt(32);
    int count = 1 + random.nextInt(500);
    long[] data = new long[BitPacking.longCount(count, bits) + 1 + random.nextInt(3)];

    Failures.expect(IllegalArgumentException.class, () -> BitPacking.unpack(data, count, bits));
    Failures.expect(
        IllegalArgumentException.class,
        () -> BitPacking.unpack(data, count, 33 + random.nextInt(100)));
    Failures.expect(
        IllegalArgumentException.class,
        () -> BitPacking.pack(new int[] {-1 - random.nextInt(1000)}, bits));
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void damagedContainersGiveAContainerOrAFormatFault(long seed) {
    // spec: CF-R10 (inconsistent data: a PaletteFormatException), CF-R6
    SplittableRandom random = new SplittableRandom(3_000 + seed);
    TreeMutator mutator = new TreeMutator(seed);
    int[] sizes = {2, 5, 16, 17, 100, 257};
    for (int paletteSize : sizes) {
      int bits = RefPacking.blockBits(paletteSize);
      CompoundTag valid = container(paletteSize, 4096, bits, random);
      CompoundTag damaged = mutator.mutate(valid, 1 + random.nextInt(3));

      checkBlocks(damaged);
    }
    CompoundTag biome = container(9, 64, RefPacking.biomeBits(9), random);
    checkBiomes(mutator.mutate(biome, 2));
  }

  private static void checkBlocks(CompoundTag damaged) {
    try {
      PalettedContainer<Integer> container = blocks().decode(damaged);
      assertThat(container.size()).isEqualTo(4096);
      PalettedContainer<Integer> again = blocks().decode(blocks().encode(container));
      for (int index = 0; index < 4096; index++) {
        assertThat(again.get(index)).isEqualTo(container.get(index));
      }
    } catch (PaletteFormatException expected) {
      assertThat(expected.getMessage()).isNotBlank();
    }
  }

  private static void checkBiomes(CompoundTag damaged) {
    try {
      PalettedContainer<Integer> container = biomes().decode(damaged);
      assertThat(container.size()).isEqualTo(64);
      PalettedContainer<Integer> again = biomes().decode(biomes().encode(container));
      for (int index = 0; index < 64; index++) {
        assertThat(again.get(index)).isEqualTo(container.get(index));
      }
    } catch (PaletteFormatException expected) {
      assertThat(expected.getMessage()).isNotBlank();
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void randomContainersWrittenAndReadKeepEveryPosition(long seed) throws PaletteFormatException {
    // spec: CF-R6 to CF-R9 (round trip for any number of distinct values)
    SplittableRandom random = new SplittableRandom(4_000 + seed);
    int distinct = 1 + random.nextInt(random.nextBoolean() ? 20 : 3000);
    PalettedContainer<Integer> container = PalettedContainer.filled(4096, 0);
    for (int index = 0; index < 4096; index++) {
      container.set(index, random.nextInt(distinct));
    }

    PalettedContainer<Integer> back = blocks().decode(blocks().encode(container));

    for (int index = 0; index < 4096; index++) {
      assertThat(back.get(index)).isEqualTo(container.get(index));
    }
    assertThat(blocks().encode(container).getList("palette").elements())
        .hasSize(container.distinctValues().size());
  }
}
