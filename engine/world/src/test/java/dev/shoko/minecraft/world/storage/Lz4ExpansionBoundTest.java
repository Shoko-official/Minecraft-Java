package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.Hex;
import dev.shoko.minecraft.world.support.Lz4Frames;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * AR-R29 and Lz4BlockCodec.decode: a block cannot expand more than 255 times its compressed size (a
 * byte of match length adds at most 255 bytes), so a header that announces more is refused before
 * the block is allocated. The bound must not reject a legitimate dense block, and it must not
 * overflow on a large compressed size.
 */
class Lz4ExpansionBoundTest {

  private static final long BIG = 1L << 30;
  private static final int LEVEL_15 = 15;
  // Far below the 32 MiB a block announced as 2^25 bytes would take, far above what a refusal
  // needs.
  private static final long ALLOCATION_BUDGET = 1L << 20;

  private static byte[] decode(byte[] stream) throws Exception {
    return Lz4BlockCodec.decode(stream, BIG);
  }

  @ParameterizedTest
  @CsvSource({"20, 10", "22, 12"})
  void aLegitimateBlockExpandingBy254IsAccepted(int sizeLog2, int level) throws Exception {
    // spec: AR-R29 (the bound is on what no valid block does): a long match at distance 1 is
    // about 254 bytes of output per byte of input, the densest block the format allows
    int length = 1 << sizeLog2;
    byte[] stream = Hex.concat(Lz4Frames.runBlock(length, level), Lz4Frames.endMark(level));
    Lz4Frames.Block header = Lz4Frames.parse(stream).get(0);
    assertThat((long) header.length()).isGreaterThanOrEqualTo(254L * header.compressedLength());
    assertThat((long) header.length()).isLessThan(255L * header.compressedLength());

    byte[] out = Lz4BlockCodec.decode(stream, length);

    assertThat(out).hasSize(length);
    assertThat(out).containsOnly((byte) 'a');
  }

  @Test
  void theEncoderOfTheModuleOutputIsWithinTheBoundForVeryCompressibleData() throws Exception {
    // spec: AR-R15 (the writer's own stream decodes): 2^20 zeros, one block of 65 536 each
    byte[] zeros = new byte[1 << 20];

    byte[] stream = Lz4BlockCodec.encode(zeros);

    for (Lz4Frames.Block block : Lz4Frames.parse(stream)) {
      assertThat((long) block.length()).isLessThanOrEqualTo(255L * block.compressedLength());
    }
    assertThat(Lz4BlockCodec.decode(stream, zeros.length)).isEqualTo(zeros);
  }

  @Test
  void aLargeCompressedBlockIsNotMistakenForAnImpossibleOne() throws Exception {
    // the bound is computed on 64 bits: 255 times 9 million overflows a 32 bit integer
    byte[] plain = new byte[9_000_000];
    new SplittableRandom(7).nextBytes(plain);
    byte[] stream =
        Hex.concat(Lz4Frames.literalBlock(plain, LEVEL_15), Lz4Frames.endMark(LEVEL_15));

    assertThat(Lz4BlockCodec.decode(stream, plain.length)).isEqualTo(plain);
  }

  @Test
  void aHeaderAnnouncingMoreThan255TimesTheCompressedSizeIsRefusedAsImpossible() {
    // spec: AR-R29 (no announced size is believed): 10 compressed bytes cannot give 2551
    byte[] body = new byte[10];
    byte[] stream =
        Hex.concat(Lz4Frames.block(0x2A, body.length, 2551, 0, body), Lz4Frames.endMark(10));

    CorruptStreamException failure =
        Failures.expect(CorruptStreamException.class, () -> decode(stream));

    // The message is the only observable here: 2551 bytes are too few to see an allocation, and
    // the content of this body would be refused as corrupt with or without the bound.
    assertThat(failure.getMessage()).contains("impossible length");
  }

  @Test
  void aHeaderAnnouncingExactly255TimesTheCompressedSizeIsNotRefusedByTheBound() {
    // 255 times is the largest ratio the bound allows; the block is then judged by its content
    byte[] body = new byte[10];
    byte[] stream =
        Hex.concat(Lz4Frames.block(0x2A, body.length, 2550, 0, body), Lz4Frames.endMark(10));

    CorruptStreamException failure =
        Failures.expect(CorruptStreamException.class, () -> decode(stream));

    // No valid block reaches exactly 255 times, so the content refuses this one either way (the
    // type is the same); only the absence of the bound's message tells the comparison is `>`.
    assertThat(failure.getMessage()).doesNotContain("impossible length");
  }

  @Test
  void aHugeAnnouncedLengthOverAOneByteBodyIsRefusedBeforeAnyAllocation() {
    // spec: AR-R29: 2^25 bytes announced for 1 byte; refused from the header, the block is not
    // allocated or decoded
    byte[] body = {0};
    byte[] stream =
        Hex.concat(
            Lz4Frames.block(0x20 | LEVEL_15, 1, 1 << 25, 0, body), Lz4Frames.endMark(LEVEL_15));

    long allocated =
        allocatedBy(() -> Failures.expect(CorruptStreamException.class, () -> decode(stream)));

    assertThat(allocated).isLessThan(ALLOCATION_BUDGET);
  }

  @Test
  void theBoundAppliesToTheBlocksAfterTheFirstOneToo() {
    // spec: AR-R29 (every block header is checked, not the first only): the second block would
    // take 32 MiB if it were allocated
    byte[] body = new byte[10];
    byte[] stream =
        Hex.concat(
            Lz4Frames.rawBlock("minecraft".getBytes(StandardCharsets.US_ASCII), 6),
            Lz4Frames.block(0x20 | LEVEL_15, body.length, 1 << 25, 0, body),
            Lz4Frames.endMark(6));

    long allocated =
        allocatedBy(() -> Failures.expect(CorruptStreamException.class, () -> decode(stream)));

    assertThat(allocated).isLessThan(ALLOCATION_BUDGET);
  }

  /**
   * Bytes the current thread allocates while the call runs. The JDK documents the counter as
   * approximate, so the callers compare it with a margin of a factor 32; the test is skipped when
   * the JVM cannot measure.
   */
  private static long allocatedBy(Runnable call) {
    var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    assumeTrue(
        threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled(),
        "this JVM does not measure the allocation of a thread");
    long id = Thread.currentThread().threadId();
    long before = threads.getThreadAllocatedBytes(id);
    assertThat(before).as("allocated bytes before the call").isNotEqualTo(-1L);
    call.run();
    return threads.getThreadAllocatedBytes(id) - before;
  }

  @Test
  void aCutStreamWithAnImpossibleAnnouncedLengthStaysACorruptStream() {
    // spec: AR-R15 (a body cut short) and AR-R29: whichever of the two the decoder meets first,
    // the error is a corrupt stream, never the limit and never another exception
    byte[] body = new byte[10];
    byte[] stream = Lz4Frames.block(0x2A, 100, 255 * 100 + 1, 0, body);

    Failures.expect(CorruptStreamException.class, () -> decode(stream));
    Failures.expect(CorruptStreamException.class, () -> decode(Arrays.copyOf(stream, 21)));
    Failures.expect(CorruptStreamException.class, () -> decode(Arrays.copyOf(stream, 25)));
  }

  @Test
  void aCompressedSizeBeyondTheStreamIsACorruptStreamWhateverItsValue() {
    // spec: AR-R15: the largest and the smallest 32 bit sizes do not overflow the checks
    for (int compressed : new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE - 20, 1 << 30}) {
      byte[] stream = Lz4Frames.block(0x20 | LEVEL_15, compressed, 1 << 25, 0, new byte[3]);

      Failures.expect(CorruptStreamException.class, () -> decode(stream));
    }
  }
}
