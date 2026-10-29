package dev.shoko.minecraft.net.frame;

import static dev.shoko.minecraft.net.testing.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.testing.Allocation;
import dev.shoko.minecraft.net.testing.Feeds;
import dev.shoko.minecraft.net.testing.Fuzz;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Wire;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: net-protocol-777 R27, R30, R90, R91 (adversarial): fixed seeds, typed failures only, no
// allocation sized from an announced length, bombs refused before they are inflated
class FrameAttackTest {

  private static final long TINY_BUDGET = 1_048_576;

  private static FrameDecoder decoder(int threshold) {
    FrameDecoder decoder = FrameDecoder.create();
    decoder.setCompressionThreshold(threshold);
    return decoder;
  }

  private static byte[] rawFrame(int dataLength, byte[] body) {
    byte[] inner = Hex.concat(Wire.varInt(dataLength), body);
    return Hex.concat(Wire.varInt(inner.length), inner);
  }

  private static void feedRandomly(FrameDecoder decoder, byte[] data, Random random)
      throws ProtocolException {
    int position = 0;
    int fed = 0;
    while (position < data.length) {
      int step = Math.min(data.length - position, 1 + random.nextInt(9));
      decoder.feed(data, position, step);
      position += step;
      fed += step;
      assertThat(decoder.bufferedBytes()).isBetween(0, fed);
    }
  }

  @Test
  @Timeout(60)
  void randomBytesOnlyEverFailWithAProtocolExceptionThenTheDecoderIsUnusable() {
    // spec: R27, R90 (a decoder never fails with anything but a typed error)
    Random random = new Random(0xA77ACL);
    int[] thresholds = {-1, 0, 256};
    for (int i = 0; i < 6000; i++) {
      FrameDecoder decoder = decoder(thresholds[i % thresholds.length]);
      byte[] data = Fuzz.bytes(random, 64);
      try {
        feedRandomly(decoder, data, random);
      } catch (ProtocolException typed) {
        assertThat(typed.reason()).isNotNull();
        assertThatThrownBy(() -> decoder.feed(new byte[1], 0, 1))
            .isInstanceOf(IllegalStateException.class);
      }
    }
  }

  @Test
  @Timeout(60)
  void mutatedValidCompressedStreamsOnlyEverFailWithAProtocolException() {
    // spec: R29, R30, R90 (bit flips, truncation and extra bytes on valid frames)
    Random random = new Random(0xB17F11BL);
    for (int i = 0; i < 3000; i++) {
      byte[] valid =
          Hex.concat(
              Wire.compressedFrame(random.nextInt(200), Fuzz.bytes(random, 400), 64),
              Wire.compressedFrame(random.nextInt(200), Fuzz.bytes(random, 40), 64));
      byte[] mutated = Fuzz.mutate(random, valid);
      FrameDecoder decoder = decoder(64);
      try {
        feedRandomly(decoder, mutated, random);
      } catch (ProtocolException typed) {
        assertThat(typed.reason()).isNotNull();
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalTinyInputAnnouncingTheMaximumFrameDoesNotPreallocateIt()
      throws ProtocolException {
    // spec: R90, R91 (fragmentation: the buffer grows with the bytes received, not the
    // announcement)
    byte[] announcement = Hex.bytes("ff ff 7f");
    Allocation.bytesAllocatedBy(() -> Feeds.whole(FrameDecoder.create(), announcement));
    FrameDecoder decoder = FrameDecoder.create();
    long allocated = Allocation.bytesAllocatedBy(() -> Feeds.whole(decoder, announcement));
    assertThat(allocated).isLessThan(65_536);
    assertThat(decoder.bufferedBytes()).isEqualTo(3);
  }

  @Test
  @Timeout(60)
  void provisionalAnAnnouncedSizeAboveTheLimitIsRefusedBeforeAnythingIsAllocated() {
    // spec: R30 d (the limit applies before the allocation)
    byte[] tiny = Wire.zlib(Hex.bytes("7c 01 02"));
    byte[] huge = rawFrame(FrameLimits.MAX_DECOMPRESSED_LENGTH + 1, tiny);
    byte[] maxInt = rawFrame(Integer.MAX_VALUE, tiny);
    for (byte[] frame : List.of(huge, maxInt)) {
      Allocation.bytesAllocatedBy(() -> Feeds.whole(decoder(256), frame));
      long allocated = Allocation.bytesAllocatedBy(() -> Feeds.whole(decoder(256), frame));
      assertThat(allocated).isLessThan(TINY_BUDGET);
      assertThat(failure(() -> Feeds.whole(decoder(256), frame)).reason())
          .isEqualTo(Reason.DECOMPRESSED_TOO_LARGE);
    }
  }

  @Test
  @Timeout(60)
  void provisionalAFalseSmallAnnouncementOverABombIsRefusedWithoutInflatingIt() {
    // spec: R30 c, d (announced 1000 bytes, the stream inflates to 32 MiB: about 32 KB sent)
    byte[] bomb = Wire.zlib(new byte[32 * 1024 * 1024]);
    assertThat(bomb.length).as("the bomb is small on the wire").isLessThan(200_000);
    byte[] frame = rawFrame(1000, bomb);
    Allocation.bytesAllocatedBy(() -> Feeds.whole(decoder(256), frame));
    long allocated = Allocation.bytesAllocatedBy(() -> Feeds.whole(decoder(256), frame));
    assertThat(allocated).isLessThan(TINY_BUDGET);
    assertThat(failure(() -> Feeds.whole(decoder(256), frame)).reason())
        .isEqualTo(Reason.DECOMPRESSED_SIZE_MISMATCH);
  }

  @Test
  @Timeout(60)
  void provisionalANegativeAnnouncedSizeIsATypedFailureWithoutAllocation() {
    // spec: R30, R90 (a VarInt data length of -1 is not a size)
    byte[] frame = rawFrame(-1, Wire.zlib(Hex.bytes("7c 01 02")));
    Allocation.bytesAllocatedBy(() -> Feeds.whole(decoder(256), frame));
    long allocated = Allocation.bytesAllocatedBy(() -> Feeds.whole(decoder(256), frame));
    assertThat(allocated).isLessThan(TINY_BUDGET);
    assertThat(failure(() -> Feeds.whole(decoder(256), frame)).reason()).isNotNull();
  }

  @Test
  @Timeout(60)
  void manyTinyFramesInOneSegmentAreAllDecoded() throws ProtocolException {
    // spec: R91 (volume), test plan L1 (several frames in one segment)
    byte[] one = Hex.bytes("01 00");
    byte[] data = new byte[2 * 50_000];
    for (int i = 0; i < 50_000; i++) {
      System.arraycopy(one, 0, data, 2 * i, 2);
    }
    assertThat(Feeds.whole(FrameDecoder.create(), data)).hasSize(50_000);
  }
}
