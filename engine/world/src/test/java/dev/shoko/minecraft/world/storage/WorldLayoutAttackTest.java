package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.RegionPos;
import java.nio.file.Path;
import java.util.SplittableRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Seeded attack on {@link WorldLayout}: paths stay inside the world, names parse back. */
class WorldLayoutAttackTest {

  private static final Pattern REGION_NAME = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
  private static final Pattern EXTERNAL_NAME = Pattern.compile("c\\.(-?\\d+)\\.(-?\\d+)\\.mcc");
  private static final String ALPHABET = "ab_.:/\\-012 ~%*?\"<>|";

  static Stream<Long> seeds() {
    return LongStream.range(0, 60).boxed();
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void dimensionIdentifiersEitherFailOrStayUnderTheWorldFolder(long seed) {
    // spec: WorldLayout.dimensionFolder contract (IllegalArgumentException, never an escape), AR-R1
    SplittableRandom random = new SplittableRandom(seed);
    Path root = Path.of("saves", "world").toAbsolutePath().normalize();
    for (int round = 0; round < 60; round++) {
      StringBuilder id = new StringBuilder();
      int length = random.nextInt(14);
      for (int k = 0; k < length; k++) {
        id.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
      }
      try {
        Path folder = WorldLayout.dimensionFolder(root, id.toString());

        assertThat(folder.normalize().startsWith(root.resolve("dimensions")))
            .as(id.toString())
            .isTrue();
        assertThat(folder.normalize()).isNotEqualTo(root.resolve("dimensions"));
        assertThat(id.toString()).contains(":");
      } catch (IllegalArgumentException expected) {
        assertThat(expected.getMessage()).isNotNull();
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void regionAndExternalNamesParseBackToTheirCoordinates(long seed) {
    // spec: AR-R4, AR-R17 (decimal with a minus sign, whatever the int)
    SplittableRandom random = new SplittableRandom(1_000 + seed);
    int[] edges = {Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0, 1, -32, 31, 32};
    for (int round = 0; round < 50; round++) {
      int x = random.nextBoolean() ? edges[random.nextInt(edges.length)] : random.nextInt();
      int z = random.nextBoolean() ? edges[random.nextInt(edges.length)] : random.nextInt();

      Matcher region = REGION_NAME.matcher(WorldLayout.regionFileName(new RegionPos(x, z)));
      Matcher external =
          EXTERNAL_NAME.matcher(WorldLayout.externalChunkFileName(new ChunkPos(x, z)));

      assertThat(region.matches()).isTrue();
      assertThat(Integer.parseInt(region.group(1))).isEqualTo(x);
      assertThat(Integer.parseInt(region.group(2))).isEqualTo(z);
      assertThat(external.matches()).isTrue();
      assertThat(Integer.parseInt(external.group(1))).isEqualTo(x);
      assertThat(Integer.parseInt(external.group(2))).isEqualTo(z);
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void aChunkAlwaysLandsInTheRegionFileOfItsFloorDivision(long seed) {
    // spec: AR-R4 (rx = cx >> 5, rounding down; never a division toward zero)
    SplittableRandom random = new SplittableRandom(2_000 + seed);
    for (int round = 0; round < 50; round++) {
      int cx = random.nextInt();
      int cz = random.nextInt();

      Matcher name = REGION_NAME.matcher(WorldLayout.regionFileName(new ChunkPos(cx, cz).region()));

      assertThat(name.matches()).isTrue();
      assertThat(Integer.parseInt(name.group(1))).isEqualTo(Math.floorDiv(cx, 32));
      assertThat(Integer.parseInt(name.group(2))).isEqualTo(Math.floorDiv(cz, 32));
    }
  }
}
