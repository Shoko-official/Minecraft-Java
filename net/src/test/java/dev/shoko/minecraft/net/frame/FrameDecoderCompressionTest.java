package dev.shoko.minecraft.net.frame;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static dev.shoko.minecraft.net.testing.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.testing.Cfb8Model;
import dev.shoko.minecraft.net.testing.Feeds;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Wire;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R28 to R30 (decoding with compression), vectors F09 to F14, I03 to I05
class FrameDecoderCompressionTest {

  private static FrameDecoder decoder(int threshold) {
    FrameDecoder decoder = FrameDecoder.create();
    decoder.setCompressionThreshold(threshold);
    return decoder;
  }

  /**
   * A frame of the compressed format with an explicit data length and body, as sent on the wire.
   */
  private static byte[] rawFrame(int dataLength, byte[] body) {
    byte[] inner = Hex.concat(Wire.varInt(dataLength), body);
    return Hex.concat(Wire.varInt(inner.length), inner);
  }

  @Test
  void aBodyBelowTheThresholdWithADataLengthOfZeroIsAPlainFrame() throws ProtocolException {
    // spec: R29, vector F09
    byte[] frame = Hex.bytes("0c 00 7c 00 01 02 03 04 05 06 07 08 09");
    assertThat(Feeds.whole(decoder(256), frame)).containsExactly(Frame.of(0x7c, Hex.ramp(10)));
  }

  @Test
  void theLiteralCompressedFrameOfVectorF10IsDecoded() throws ProtocolException {
    // spec: R29, vector F10 (any valid zlib stream is read, this one comes from another zlib)
    byte[] frame = Hex.bytes("10 ad 02 78 9c ab 71 1c 05 8e c4 02 00 5d 7a 4c a9");
    assertThat(Feeds.whole(decoder(256), frame))
        .containsExactly(Frame.of(0x7c, Hex.filled('A', 300)));
  }

  @Test
  void theLiteralCompressedFrameOfVectorF13IsDecodedWithAThresholdOfZero()
      throws ProtocolException {
    // spec: R29, vector F13 (threshold 0, empty payload, id 0)
    byte[] frame = Hex.bytes("0a 01 78 9c 63 00 00 00 01 00 01");
    assertThat(Feeds.whole(decoder(0), frame)).containsExactly(Frame.of(0, new byte[0]));
  }

  @Test
  void aBodyOfTheThresholdOrMoreIsReadAsCompressedAtTheBoundary() throws ProtocolException {
    // spec: R29, vectors F11 (255 bytes plain) and F12 (256 bytes compressed)
    byte[] below = Hex.filled('B', 254);
    byte[] at = Hex.filled('B', 255);
    assertThat(Feeds.whole(decoder(256), Wire.compressedFrame(0x7c, below, 256)))
        .containsExactly(Frame.of(0x7c, below));
    assertThat(Feeds.whole(decoder(256), Wire.compressedFrame(0x7c, at, 256)))
        .containsExactly(Frame.of(0x7c, at));
  }

  @Test
  void provisionalAPlainBodyAtOrAboveTheThresholdIsAccepted() throws ProtocolException {
    // spec: R30 a, vector I03 (data length 0 with a 300 byte plain body)
    byte[] body = Hex.concat(Hex.bytes("7c"), Hex.filled('A', 299));
    assertThat(Feeds.whole(decoder(256), rawFrame(0, body)))
        .containsExactly(Frame.of(0x7c, Hex.filled('A', 299)));
  }

  @Test
  void provisionalADataLengthBelowTheThresholdIsRefused() {
    // spec: R30 b, vector I05 (data length 10, threshold 256, with a zlib stream)
    byte[] frame = Hex.bytes("0c 0a 78 9c ab 71 82 01 00 10 7c 02 cf");
    assertFails(() -> Feeds.whole(decoder(256), frame), Reason.DATA_LENGTH_BELOW_THRESHOLD);
  }

  @Test
  void provisionalTheDataLengthBoundaryIsTheThreshold() throws ProtocolException {
    // spec: R30 b (1 to threshold - 1 refused), R29 (threshold or more compressed)
    byte[] justBelow = Hex.concat(Hex.bytes("7c"), Hex.filled('C', 254));
    byte[] atThreshold = Hex.concat(Hex.bytes("7c"), Hex.filled('C', 255));
    assertFails(
        () -> Feeds.whole(decoder(256), rawFrame(255, Wire.zlib(justBelow))),
        Reason.DATA_LENGTH_BELOW_THRESHOLD);
    assertThat(Feeds.whole(decoder(256), rawFrame(256, Wire.zlib(atThreshold)))).hasSize(1);
    assertFails(
        () -> Feeds.whole(decoder(256), rawFrame(1, Wire.zlib(Hex.bytes("7c")))),
        Reason.DATA_LENGTH_BELOW_THRESHOLD);
  }

  @Test
  void provisionalAnnouncedSizeLargerThanTheStreamIsRefused() {
    // spec: R30 c, vector I04 (data length 301, the stream holds 300 bytes)
    byte[] body = Hex.concat(Hex.bytes("7c"), Hex.filled('A', 299));
    assertFails(
        () -> Feeds.whole(decoder(256), rawFrame(301, Wire.zlib(body))),
        Reason.DECOMPRESSED_SIZE_MISMATCH);
  }

  @Test
  void provisionalAnnouncedSizeSmallerThanTheStreamIsRefused() {
    // spec: R30 c
    byte[] body = Hex.concat(Hex.bytes("7c"), Hex.filled('A', 299));
    assertFails(
        () -> Feeds.whole(decoder(256), rawFrame(299, Wire.zlib(body))),
        Reason.DECOMPRESSED_SIZE_MISMATCH);
  }

  @Test
  void provisionalAStreamThatIsNotZlibIsCorruptWithTheZlibFailureAsCause() {
    // spec: R29, API contract of the decoder (CORRUPT_COMPRESSION with a cause)
    ProtocolException failure =
        failure(() -> Feeds.whole(decoder(256), rawFrame(300, Hex.filled(0xff, 40))));
    assertThat(failure.reason()).isEqualTo(Reason.CORRUPT_COMPRESSION);
    assertThat(failure.getCause()).isNotNull();
  }

  @Test
  void provisionalTheDecompressedLimitIsExclusiveOfTheLimitItself() throws ProtocolException {
    // spec: R30 d (above the limit is refused, the limit itself is accepted), with a small limit
    FrameLimits limits = new FrameLimits(FrameLimits.MAX_FRAME_LENGTH, 1000);
    byte[] atLimit = Hex.concat(Hex.bytes("7c"), Hex.filled('D', 999));
    byte[] over = Hex.concat(Hex.bytes("7c"), Hex.filled('D', 1000));
    FrameDecoder accepting = FrameDecoder.create(limits);
    accepting.setCompressionThreshold(256);
    assertThat(Feeds.whole(accepting, rawFrame(1000, Wire.zlib(atLimit)))).hasSize(1);
    FrameDecoder refusing = FrameDecoder.create(limits);
    refusing.setCompressionThreshold(256);
    assertFails(
        () -> Feeds.whole(refusing, rawFrame(1001, Wire.zlib(over))),
        Reason.DECOMPRESSED_TOO_LARGE);
  }

  @Test
  void provisionalTheDefaultDecompressedLimitIsTwoToTheTwentyThree() throws ProtocolException {
    // spec: R30 d (2^23 = 8 388 608 bytes, accepted at the limit, refused above)
    int limit = FrameLimits.MAX_DECOMPRESSED_LENGTH;
    byte[] atLimit = Hex.concat(Hex.bytes("7c"), new byte[limit - 1]);
    List<Frame> frames = Feeds.whole(decoder(256), rawFrame(limit, Wire.zlib(atLimit)));
    assertThat(frames).hasSize(1);
    assertThat(frames.get(0).payload()).hasSize(limit - 1);
    byte[] over = Hex.concat(Hex.bytes("7c"), new byte[limit]);
    assertFails(
        () -> Feeds.whole(decoder(256), rawFrame(limit + 1, Wire.zlib(over))),
        Reason.DECOMPRESSED_TOO_LARGE);
  }

  @Test
  void provisionalTheCompressedLengthIsBoundedByTheFrameLimit() {
    // spec: R30 ("le plafond de longueur 2^21 - 1 s'applique a la longueur compressee")
    FrameLimits limits = new FrameLimits(60, FrameLimits.MAX_DECOMPRESSED_LENGTH);
    FrameDecoder decoder = FrameDecoder.create(limits);
    decoder.setCompressionThreshold(256);
    assertFails(() -> Feeds.whole(decoder, Hex.bytes("3d")), Reason.FRAME_LENGTH_INVALID);
  }

  @Test
  void aNegativeThresholdSwitchesTheCompressedFormatOff() throws ProtocolException {
    // spec: R28, vector F14
    FrameDecoder decoder = decoder(256);
    assertThat(Feeds.whole(decoder, Hex.bytes("0c 00 7c 00 01 02 03 04 05 06 07 08 09")))
        .hasSize(1);
    decoder.setCompressionThreshold(-1);
    assertThat(Feeds.whole(decoder, Hex.bytes("01 00"))).containsExactly(Frame.of(0, new byte[0]));
  }

  @Test
  void theThresholdAppliesToTheFramesFedAfterLoginCompression() throws ProtocolException {
    // spec: R28 (login_compression is the last uncompressed packet), vector F07
    FrameDecoder decoder = FrameDecoder.create();
    assertThat(Feeds.whole(decoder, Hex.bytes("03 03 80 02")))
        .containsExactly(Frame.of(3, Hex.bytes("80 02")));
    decoder.setCompressionThreshold(256);
    assertThat(Feeds.whole(decoder, Hex.bytes("0c 00 7c 00 01 02 03 04 05 06 07 08 09")))
        .containsExactly(Frame.of(0x7c, Hex.ramp(10)));
  }

  @Test
  void compressedFramesInOneSegmentOrOneByteAtATimeGiveTheSameFrames() throws ProtocolException {
    // spec: test plan L1 (idempotence of the splitting into frames), R33
    byte[] first = Wire.compressedFrame(1, Hex.filled('E', 700), 256);
    byte[] second = Wire.compressedFrame(2, Hex.ramp(20), 256);
    byte[] third = Wire.compressedFrame(3, Hex.filled('F', 256), 256);
    byte[] data = Hex.concat(first, second, third);
    List<Frame> expected =
        List.of(
            Frame.of(1, Hex.filled('E', 700)),
            Frame.of(2, Hex.ramp(20)),
            Frame.of(3, Hex.filled('F', 256)));
    assertThat(Feeds.whole(decoder(256), data)).containsExactlyElementsOf(expected);
    assertThat(Feeds.byteByByte(decoder(256), data)).containsExactlyElementsOf(expected);
    assertThat(Feeds.chunked(decoder(256), data, 13)).containsExactlyElementsOf(expected);
  }

  @Test
  void aThresholdOfZeroAcceptsAPlainBodyWithADataLengthOfZero() throws ProtocolException {
    // spec: R30 a (data length 0 is accepted whatever the size of the body)
    assertThat(Feeds.whole(decoder(0), rawFrame(0, Hex.bytes("05 aa"))))
        .containsExactly(Frame.of(5, Hex.bytes("aa")));
  }

  @Test
  void compressionAndDecryptionTogetherReadCompressedAfterDecrypting() throws ProtocolException {
    // spec: R31 (compress then encrypt on sending, so decrypt then decompress on receiving)
    byte[] secret = Hex.ramp(16);
    byte[] plain =
        Hex.concat(
            Wire.compressedFrame(1, Hex.filled('G', 500), 256),
            Wire.compressedFrame(2, Hex.ramp(5), 256));
    FrameDecoder decoder = decoder(256);
    decoder.enableDecryption(Cfb8Ciphers.decryptor(secret));
    assertThat(Feeds.chunked(decoder, Cfb8Model.encrypt(secret, plain), 9))
        .containsExactly(Frame.of(1, Hex.filled('G', 500)), Frame.of(2, Hex.ramp(5)));
  }

  @Test
  void provisionalAFailedCompressedFrameLeavesTheDecoderUnusable() {
    // spec: API contract of FrameDecoder
    FrameDecoder decoder = decoder(256);
    byte[] body = Hex.concat(Hex.bytes("7c"), Hex.filled('A', 299));
    assertFails(
        () -> Feeds.whole(decoder, rawFrame(301, Wire.zlib(body))),
        Reason.DECOMPRESSED_SIZE_MISMATCH);
    assertThatThrownBy(() -> Feeds.whole(decoder, Hex.bytes("01 00")))
        .isInstanceOf(IllegalStateException.class);
  }
}
