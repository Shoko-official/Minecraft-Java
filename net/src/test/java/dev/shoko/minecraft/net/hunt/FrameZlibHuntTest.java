package dev.shoko.minecraft.net.hunt;

import static dev.shoko.minecraft.net.testing.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.frame.FrameDecoder;
import dev.shoko.minecraft.net.frame.FrameLimits;
import dev.shoko.minecraft.net.testing.Allocation;
import dev.shoko.minecraft.net.testing.Feeds;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Wire;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: net-protocol-777 R29 (the body is "un flux zlib"), R30 c and d, R91
class FrameZlibHuntTest {

  private static final int SIZE = 300;

  private static byte[] body() {
    byte[] body = new byte[SIZE];
    Arrays.fill(body, (byte) 5);
    body[0] = 4;
    return body;
  }

  private static FrameDecoder decoder() {
    FrameDecoder decoder = FrameDecoder.create();
    decoder.setCompressionThreshold(256);
    return decoder;
  }

  private static byte[] raw(int dataLength, byte[] stream) {
    byte[] inner = Hex.concat(Wire.varInt(dataLength), stream);
    return Hex.concat(Wire.varInt(inner.length), inner);
  }

  private static Reason reasonOfRefusal(byte[] frame) {
    return failure(() -> Feeds.whole(decoder(), frame)).reason();
  }

  @Test
  void provisionalBytesAfterTheEndOfTheZlibStreamAreRefused() {
    // spec: R29, R30 c. The extra bytes belong to no field; accepting them hides a corrupt frame.
    byte[] extra = Hex.concat(Wire.zlib(body()), Hex.bytes("de ad be ef"));
    assertThat(reasonOfRefusal(raw(SIZE, extra)))
        .isIn(Reason.CORRUPT_COMPRESSION, Reason.DECOMPRESSED_SIZE_MISMATCH);
  }

  @Test
  void provisionalASecondZlibMemberIsRefused() {
    // spec: R29 (one zlib stream), R30 c
    byte[] good = Wire.zlib(body());
    assertThat(reasonOfRefusal(raw(SIZE, Hex.concat(good, good))))
        .isIn(Reason.CORRUPT_COMPRESSION, Reason.DECOMPRESSED_SIZE_MISMATCH);
  }

  @Test
  void aStreamCutBeforeItsAdlerChecksumIsRefusedEvenWithTheRightSize() {
    // spec: R29 (a valid zlib stream, RFC 1950, ends with its Adler-32), R30 c
    byte[] good = Wire.zlib(body());
    byte[] cut = Arrays.copyOf(good, good.length - 4);
    assertThat(reasonOfRefusal(raw(SIZE, cut)))
        .isIn(Reason.CORRUPT_COMPRESSION, Reason.DECOMPRESSED_SIZE_MISMATCH);
  }

  @Test
  void aStreamWithABadChecksumIsCorrupt() {
    // spec: R29, API contract of the decoder (CORRUPT_COMPRESSION)
    byte[] bad = Wire.zlib(body());
    bad[bad.length - 1] ^= 0x55;
    assertThat(reasonOfRefusal(raw(SIZE, bad))).isEqualTo(Reason.CORRUPT_COMPRESSION);
  }

  @Test
  void aStreamThatNeedsAPresetDictionaryIsCorrupt() {
    // spec: R29, API contract of the decoder (CORRUPT_COMPRESSION, not a size mismatch)
    Deflater deflater = new Deflater();
    deflater.setDictionary(new byte[] {1, 2, 3});
    deflater.setInput(body());
    deflater.finish();
    byte[] buffer = new byte[1000];
    int length = deflater.deflate(buffer);
    deflater.end();
    byte[] stream = Arrays.copyOf(buffer, length);
    assertThat(reasonOfRefusal(raw(SIZE, stream))).isEqualTo(Reason.CORRUPT_COMPRESSION);
  }

  @Test
  @Timeout(30)
  void provisionalALargeAnnouncementOverASmallStreamCostsNothingProportional() {
    // spec: R30 d, R90 (no allocation sized from an announced length), here the largest legal one
    byte[] frame = raw(FrameLimits.MAX_DECOMPRESSED_LENGTH, Wire.zlib(body()));
    Allocation.bytesAllocatedBy(() -> Feeds.whole(decoder(), frame));
    long allocated = Allocation.bytesAllocatedBy(() -> Feeds.whole(decoder(), frame));
    assertThat(allocated).isLessThan(1 << 20);
    assertThat(reasonOfRefusal(frame)).isEqualTo(Reason.DECOMPRESSED_SIZE_MISMATCH);
  }

  @Test
  @Timeout(30)
  void aLegalLargeFrameIsDecodedWithAllocationWithinAFewTimesItsSize() throws ProtocolException {
    // spec: R30 d. 8 MiB out of a few KiB: the inflater may grow its buffer, not without bound.
    int limit = FrameLimits.MAX_DECOMPRESSED_LENGTH;
    byte[] zeros = new byte[limit];
    zeros[0] = 1;
    byte[] frame = raw(limit, Wire.zlib(zeros));
    zeros = null;
    Allocation.bytesAllocatedBy(() -> Feeds.whole(decoder(), frame));
    long allocated = Allocation.bytesAllocatedBy(() -> Feeds.whole(decoder(), frame));
    assertThat(allocated).isLessThan(6L * limit);
    List<Frame> frames = Feeds.whole(decoder(), frame);
    assertThat(frames).hasSize(1);
    assertThat(frames.get(0).payload()).hasSize(limit - 1);
  }
}
