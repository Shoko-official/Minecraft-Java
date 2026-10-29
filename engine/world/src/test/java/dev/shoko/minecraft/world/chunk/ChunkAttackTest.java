package dev.shoko.minecraft.world.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.LocalIndex;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import java.util.SplittableRandom;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Seeded attack on {@link Chunk}, {@link ChunkSection} and {@link NibbleArray}. */
class ChunkAttackTest {

  static Stream<Long> seeds() {
    return LongStream.range(0, 60).boxed();
  }

  private static final int[] EDGES = {
    Integer.MIN_VALUE,
    Integer.MIN_VALUE + 1,
    -1_000_000,
    -513,
    -512,
    -17,
    -16,
    -15,
    -1,
    0,
    1,
    15,
    16,
    17,
    511,
    512,
    1_000_000,
    Integer.MAX_VALUE - 1,
    Integer.MAX_VALUE
  };

  private static int pick(SplittableRandom random) {
    return random.nextInt(4) == 0 ? EDGES[random.nextInt(EDGES.length)] : random.nextInt(-100, 100);
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void positionsInsideTheChunkRoundTripAndOthersAreIllegalArguments(long seed) {
    // spec: Chunk contract (IllegalArgumentException outside the chunk or the height), CF-R3, CF-R4
    BlockStateRegistry registry = Blocks.registry();
    SplittableRandom random = new SplittableRandom(seed);
    ChunkPos pos = new ChunkPos(pick(random) >> 4, pick(random) >> 4);
    Chunk chunk = Chunks.empty(registry, pos, Chunks.OVERWORLD);
    for (int round = 0; round < 200; round++) {
      boolean inside = random.nextInt(3) != 0;
      int x = inside ? pos.minBlockX() + random.nextInt(16) : pick(random);
      int z = inside ? pos.minBlockZ() + random.nextInt(16) : pick(random);
      int y = random.nextInt(5) == 0 ? pick(random) : random.nextInt(-64, 320);
      BlockPos block = new BlockPos(x, y, z);
      boolean legal = block.chunk().equals(pos) && Chunks.OVERWORLD.contains(y);
      BlockState state = registry.state(random.nextInt(registry.stateCount()));

      if (legal) {
        chunk.setBlockState(block, state);
        assertThat(chunk.blockState(block)).isEqualTo(state);
        int section = Math.floorDiv(y, 16);
        assertThat(chunk.section(section).blockStates().get(LocalIndex.sectionStateIndex(x, y, z)))
            .isEqualTo(state);
        chunk.setBiome(block, Chunks.DESERT);
        assertThat(chunk.biome(block)).isEqualTo(Chunks.DESERT);
      } else {
        Failures.expect(IllegalArgumentException.class, () -> chunk.blockState(block));
        Failures.expect(IllegalArgumentException.class, () -> chunk.setBlockState(block, state));
        Failures.expect(IllegalArgumentException.class, () -> chunk.biome(block));
        Failures.expect(IllegalArgumentException.class, () -> chunk.setBiome(block, Chunks.DESERT));
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void sectionLookupAndSkyLightRejectEveryYOutsideTheHeight(long seed) {
    // spec: Chunk.section and effectiveSkyLight contracts (IllegalArgumentException)
    SplittableRandom random = new SplittableRandom(1_000 + seed);
    Chunk chunk = Chunks.empty(Blocks.registry(), new ChunkPos(0, 0), Chunks.OVERWORLD);
    for (int round = 0; round < 100; round++) {
      int y = pick(random);
      if (y >= -4 && y <= 19) {
        assertThat(chunk.section(y).y()).isEqualTo(y);
        assertThat(chunk.effectiveSkyLight(y).toBytes()).hasSize(2048);
      } else {
        Failures.expect(IllegalArgumentException.class, () -> chunk.section(y));
        Failures.expect(IllegalArgumentException.class, () -> chunk.effectiveSkyLight(y));
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void nibbleArrayKeepsEveryValueAndRejectsTheOthersWithTypedErrors(long seed) {
    // spec: CF-R14, NibbleArray contract (IndexOutOfBounds for the index, IllegalArgument for the
    // value)
    SplittableRandom random = new SplittableRandom(2_000 + seed);
    NibbleArray array = NibbleArray.zeros();
    int[] model = new int[4096];
    for (int round = 0; round < 500; round++) {
      int index = random.nextInt(5) == 0 ? pick(random) : random.nextInt(4096);
      int value = random.nextInt(5) == 0 ? pick(random) : random.nextInt(16);
      boolean indexOk = index >= 0 && index < 4096;
      boolean valueOk = value >= 0 && value < 16;
      if (indexOk && valueOk) {
        array.set(index, value);
        model[index] = value;
      } else if (!indexOk) {
        Failures.expect(IndexOutOfBoundsException.class, () -> array.set(index, value));
        Failures.expect(IndexOutOfBoundsException.class, () -> array.get(index));
      } else {
        Failures.expect(IllegalArgumentException.class, () -> array.set(index, value));
      }
    }

    byte[] bytes = array.toBytes();
    for (int index = 0; index < 4096; index++) {
      assertThat(array.get(index)).isEqualTo(model[index]);
      int nibble = (bytes[index >> 1] >> ((index & 1) * 4)) & 15;
      assertThat(nibble).isEqualTo(model[index]);
    }
    assertThat(NibbleArray.ofBytes(bytes).toBytes()).isEqualTo(bytes);
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void biomeIndexStaysInRangeForAnyCoordinate(long seed) {
    // spec: CF-R5 (index in [0, 63], floor semantics for negative coordinates)
    SplittableRandom random = new SplittableRandom(3_000 + seed);
    for (int round = 0; round < 200; round++) {
      int x = pick(random);
      int y = pick(random);
      int z = pick(random);

      int index = ChunkSection.biomeIndex(x, y, z);

      int expected = ((((y & 15) >> 2) * 4 + ((z & 15) >> 2)) * 4) + ((x & 15) >> 2);
      assertThat(index).isBetween(0, 63).isEqualTo(expected);
    }
  }
}
