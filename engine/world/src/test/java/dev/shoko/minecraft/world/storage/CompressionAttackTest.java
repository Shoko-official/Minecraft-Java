package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.world.support.Hex;
import dev.shoko.minecraft.world.support.Lz4Frames;
import dev.shoko.minecraft.world.support.Regions;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Seeded attack on the decoders of {@link RegionCompression} and {@link Lz4BlockCodec}: random and
 * damaged streams end with the output, a {@link CorruptStreamException} or an {@link
 * OutputLimitExceededException}, never another exception, and never more than the limit.
 */
class CompressionAttackTest {

  private static final long[] LIMITS = {0, 1, 100, 6_000, 70_000, 1 << 20};

  static Stream<Long> seeds() {
    return LongStream.range(0, 60).boxed();
  }

  private static byte[] original(SplittableRandom random) {
    byte[] data = new byte[random.nextInt(9000)];
    random.nextBytes(data);
    if (random.nextBoolean() && data.length > 10) {
      Arrays.fill(data, 0, data.length / 2, (byte) 7);
    }
    return data;
  }

  private static byte[] damage(byte[] stream, SplittableRandom random) {
    byte[] copy = stream.clone();
    if (copy.length == 0) {
      return copy;
    }
    switch (random.nextInt(4)) {
      case 0 -> copy[random.nextInt(copy.length)] ^= (byte) (1 + random.nextInt(255));
      case 1 -> copy = Arrays.copyOf(copy, random.nextInt(copy.length));
      case 2 -> {
        for (int k = 0; k < 4; k++) {
          copy[random.nextInt(copy.length)] = (byte) random.nextInt(256);
        }
      }
      default -> copy = Hex.concat(copy, new byte[] {(byte) random.nextInt(256)});
    }
    return copy;
  }

  private static void assertTyped(RegionCompression compression, byte[] stream, long limit) {
    try {
      byte[] out = compression.decompress(stream, limit);
      assertThat(out.length).isLessThanOrEqualTo((int) Math.min(limit, Integer.MAX_VALUE));
    } catch (CorruptStreamException | OutputLimitExceededException expected) {
      assertThat(expected.getMessage()).isNotNull();
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void damagedStreamsOfEveryTypeEndInATypedOutcome(long seed) {
    // spec: AR-R20, AR-R29, AR-R30 a (a stream that does not decode, or the limit)
    SplittableRandom random = new SplittableRandom(seed);
    for (RegionCompression compression : RegionCompression.values()) {
      byte[] valid = Regions.compress(compression.id(), original(random));
      for (int round = 0; round < 6; round++) {
        byte[] stream = damage(valid, random);
        assertTyped(compression, stream, LIMITS[random.nextInt(LIMITS.length)]);
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void pureNoiseEndsInATypedOutcome(long seed) {
    // spec: AR-R20, AR-R29
    SplittableRandom random = new SplittableRandom(50_000 + seed);
    for (RegionCompression compression : RegionCompression.values()) {
      byte[] noise = new byte[random.nextInt(3000)];
      random.nextBytes(noise);
      assertTyped(compression, noise, 1 << 20);
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void lz4StreamsWithRandomHeadersAndBodiesNeverEscape(long seed) {
    // spec: AR-R15, AR-R29 (announced lengths are never believed before the budget check)
    SplittableRandom random = new SplittableRandom(70_000 + seed);
    int[] announced = {0, 1, 72, 4096, 65_536, 65_537, Integer.MAX_VALUE, -1, Integer.MIN_VALUE};
    for (int round = 0; round < 10; round++) {
      int length = announced[random.nextInt(announced.length)];
      int compressed = announced[random.nextInt(announced.length)];
      byte[] body = new byte[random.nextInt(300)];
      random.nextBytes(body);
      int token =
          random.nextBoolean() ? 0x26 : (0x10 | random.nextInt(16) | (random.nextInt(4) << 5));
      byte[] stream =
          Hex.concat(
              Lz4Frames.block(token, compressed, length, random.nextInt(), body),
              random.nextBoolean() ? Lz4Frames.endMark(6) : new byte[0]);

      assertLz4Typed(stream, LIMITS[random.nextInt(LIMITS.length)]);
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void damagedValidLz4StreamsEndInATypedOutcome(long seed) {
    // spec: AR-R15 (checksum, end mark, block structure)
    SplittableRandom random = new SplittableRandom(90_000 + seed);
    byte[] data = original(random);
    byte[][] streams = {
      Lz4Frames.defaultStream(data),
      Hex.concat(
          Lz4Frames.literalBlock(Arrays.copyOf(data, Math.min(data.length, 3000)), 6),
          Lz4Frames.endMark(6)),
      Hex.concat(Lz4Frames.runBlock(10_000 + random.nextInt(50_000), 6), Lz4Frames.endMark(6)),
      Lz4BlockCodec.encode(data)
    };

    for (byte[] valid : streams) {
      for (int round = 0; round < 8; round++) {
        assertLz4Typed(damage(valid, random), LIMITS[random.nextInt(LIMITS.length)]);
      }
    }
  }

  @ParameterizedTest(name = "seed {0}")
  @MethodSource("seeds")
  @Timeout(30)
  void everyValidStreamDecodesToItsDataWhateverTheLimitAboveItsSize(long seed) throws Exception {
    // spec: AR-R29 (the limit only bounds), Lz4BlockCodec.encode contract
    SplittableRandom random = new SplittableRandom(110_000 + seed);
    byte[] data = original(random);

    byte[] stream = Lz4BlockCodec.encode(data);

    assertThat(Lz4BlockCodec.decode(stream, data.length)).isEqualTo(data);
    assertThat(Lz4BlockCodec.decode(stream, data.length + 1L + random.nextInt(1000)))
        .isEqualTo(data);
  }

  private static void assertLz4Typed(byte[] stream, long limit) {
    try {
      byte[] out = Lz4BlockCodec.decode(stream, limit);
      assertThat(out.length).isLessThanOrEqualTo((int) Math.min(limit, Integer.MAX_VALUE));
    } catch (CorruptStreamException | OutputLimitExceededException expected) {
      assertThat(expected.getMessage()).isNotNull();
    }
  }
}
