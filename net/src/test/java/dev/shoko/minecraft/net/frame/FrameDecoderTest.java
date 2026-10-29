package dev.shoko.minecraft.net.frame;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static dev.shoko.minecraft.net.testing.Failures.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.testing.Cfb8Model;
import dev.shoko.minecraft.net.testing.Feeds;
import dev.shoko.minecraft.net.testing.FrameVectors;
import dev.shoko.minecraft.net.testing.FrameVectors.Vector;
import dev.shoko.minecraft.net.testing.Hex;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// spec: net-protocol-777 R27, R31 to R33 (decoding side without compression), vectors F01 to F08,
// F15, F16, I01, I02, I11, CH01
class FrameDecoderTest {

  private static final byte[] SECRET = Hex.ramp(16);

  static Stream<Vector> vectors() {
    return FrameVectors.uncompressed().stream();
  }

  private static List<Frame> expected(List<Vector> vectors) {
    List<Frame> frames = new ArrayList<>();
    for (Vector vector : vectors) {
      frames.add(Frame.of(vector.id(), vector.payload()));
    }
    return frames;
  }

  private static byte[] stream(List<Vector> vectors) {
    byte[] out = new byte[0];
    for (Vector vector : vectors) {
      out = Hex.concat(out, vector.frame());
    }
    return out;
  }

  @ParameterizedTest
  @MethodSource("vectors")
  void aFrameIsDecodedFromLengthIdPayload(Vector vector) throws ProtocolException {
    // spec: R27, vectors F01 to F08, F15, F16
    List<Frame> frames = Feeds.whole(FrameDecoder.create(), vector.frame());
    assertThat(frames).containsExactly(Frame.of(vector.id(), vector.payload()));
  }

  @Test
  void severalFramesInOneSegmentAreAllReturnedInOrder() throws ProtocolException {
    // spec: test plan L1 (several frames in one segment)
    List<Vector> vectors = FrameVectors.uncompressed();
    FrameDecoder decoder = FrameDecoder.create();
    assertThat(Feeds.whole(decoder, stream(vectors))).containsExactlyElementsOf(expected(vectors));
    assertThat(decoder.bufferedBytes()).isZero();
  }

  @Test
  void feedingOneByteAtATimeGivesTheSameFrames() throws ProtocolException {
    // spec: test plan L1 (a frame sent one byte at a time), R33
    List<Vector> vectors = FrameVectors.uncompressed();
    assertThat(Feeds.byteByByte(FrameDecoder.create(), stream(vectors)))
        .containsExactlyElementsOf(expected(vectors));
  }

  @Test
  void anySplitOfTheStreamInTwoGivesTheSameFrames() throws ProtocolException {
    // spec: test plan L1 (idempotence of the splitting into frames)
    List<Vector> vectors = FrameVectors.uncompressed();
    byte[] data = stream(vectors);
    for (int cut = 0; cut <= data.length; cut++) {
      FrameDecoder decoder = FrameDecoder.create();
      List<Frame> frames = new ArrayList<>(decoder.feed(data, 0, cut));
      frames.addAll(decoder.feed(data, cut, data.length - cut));
      assertThat(frames).as("cut at %d", cut).containsExactlyElementsOf(expected(vectors));
    }
  }

  @Test
  void aFrameIsReturnedAtTheCallThatCompletesIt() throws ProtocolException {
    // spec: API contract of feed (an incomplete frame stays buffered, no error, no frame)
    byte[] frame = Hex.bytes("09 04 11 22 33 44 55 66 77 88");
    FrameDecoder decoder = FrameDecoder.create();
    assertThat(decoder.feed(frame, 0, frame.length - 1)).isEmpty();
    assertThat(decoder.bufferedBytes()).isEqualTo(frame.length - 1);
    assertThat(decoder.feed(frame, frame.length - 1, 1))
        .containsExactly(Frame.of(4, Hex.bytes("11 22 33 44 55 66 77 88")));
    assertThat(decoder.bufferedBytes()).isZero();
  }

  @Test
  void aTruncatedFrameStaysBufferedWithoutErrorOrFrame() throws ProtocolException {
    // spec: R33, vector I11 (length 9 then 3 bytes)
    FrameDecoder decoder = FrameDecoder.create();
    byte[] truncated = Hex.bytes("09 01 02 03");
    assertThat(Feeds.whole(decoder, truncated)).isEmpty();
    assertThat(decoder.bufferedBytes()).isEqualTo(4);
  }

  @Test
  void bytesOfTheNextFrameAreBufferedAfterACompleteOne() throws ProtocolException {
    // spec: API contract of bufferedBytes
    FrameDecoder decoder = FrameDecoder.create();
    byte[] data = Hex.bytes("01 00 09 01");
    assertThat(Feeds.whole(decoder, data)).containsExactly(Frame.of(0, new byte[0]));
    assertThat(decoder.bufferedBytes()).isEqualTo(2);
  }

  @Test
  void aLengthSplitOverTwoCallsIsStillOneLength() throws ProtocolException {
    // spec: R27 (VarInt length), test plan L1
    byte[] payload = Hex.ramp(200);
    byte[] frame = Hex.concat(Hex.bytes("c9 01 05"), payload);
    FrameDecoder decoder = FrameDecoder.create();
    assertThat(decoder.feed(frame, 0, 1)).isEmpty();
    assertThat(decoder.feed(frame, 1, frame.length - 1)).containsExactly(Frame.of(5, payload));
  }

  @Test
  void aLengthOfOneIsAFrameWithAnIdAndNoPayload() throws ProtocolException {
    // spec: R27, vector F02
    assertThat(Feeds.whole(FrameDecoder.create(), Hex.bytes("01 07")))
        .containsExactly(Frame.of(7, new byte[0]));
  }

  @Test
  void aMultiByteIdIsAVarInt() throws ProtocolException {
    // spec: R27
    assertThat(Feeds.whole(FrameDecoder.create(), Hex.bytes("05 c8 01 aa bb cc")))
        .containsExactly(Frame.of(200, Hex.bytes("aa bb cc")));
  }

  @Test
  void anEmptyFeedReturnsNothing() throws ProtocolException {
    FrameDecoder decoder = FrameDecoder.create();
    assertThat(decoder.feed(new byte[0], 0, 0)).isEmpty();
    assertThat(decoder.bufferedBytes()).isZero();
  }

  @Test
  void onlyTheGivenRangeIsRead() throws ProtocolException {
    // spec: API contract of feed
    byte[] data = Hex.bytes("ff ff 01 00 ff ff");
    assertThat(FrameDecoder.create().feed(data, 2, 2)).containsExactly(Frame.of(0, new byte[0]));
  }

  @Test
  void aRangeOutsideTheArrayIsRefused() {
    // spec: API contract of feed
    FrameDecoder decoder = FrameDecoder.create();
    byte[] data = new byte[4];
    assertThrows(IndexOutOfBoundsException.class, () -> decoder.feed(data, -1, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> decoder.feed(data, 0, 5));
    assertThrows(IndexOutOfBoundsException.class, () -> decoder.feed(data, 3, 2));
    assertThrows(IndexOutOfBoundsException.class, () -> decoder.feed(data, 0, -1));
  }

  @Test
  void theMaximumFrameLengthIsAccepted() throws ProtocolException {
    // spec: R27, R85 (a packet of 2^21 - 1 octets is valid)
    byte[] payload = new byte[FrameLimits.MAX_FRAME_LENGTH - 1];
    payload[0] = 1;
    payload[payload.length - 1] = 2;
    byte[] data = Hex.concat(Hex.bytes("ff ff 7f 09"), payload);
    List<Frame> frames = Feeds.chunked(FrameDecoder.create(), data, 65_536);
    assertThat(frames).hasSize(1);
    assertThat(frames.get(0).packetId()).isEqualTo(9);
    assertThat(frames.get(0).payload()).isEqualTo(payload);
  }

  @Test
  void provisionalAFourByteLengthIsRefusedAsSoonAsItIsKnown() {
    // spec: R27, vector I01 (80 80 80 01, no body byte follows)
    FrameDecoder decoder = FrameDecoder.create();
    assertFails(() -> Feeds.whole(decoder, Hex.bytes("80 80 80 01")), Reason.FRAME_LENGTH_INVALID);
  }

  @Test
  void provisionalALengthWithAContinuationOnItsThirdByteIsRefusedByTheFourthByte() {
    // spec: R27 (the length tolerates 3 bytes), vector I01, fed one byte at a time
    FrameDecoder decoder = FrameDecoder.create();
    assertFails(
        () -> Feeds.byteByByte(decoder, Hex.bytes("ff ff ff 01 00 00 00")),
        Reason.FRAME_LENGTH_INVALID);
  }

  @Test
  void provisionalALengthOfZeroIsRefused() {
    // spec: R27, vector I02 (length 0: no packet id)
    assertFails(
        () -> Feeds.whole(FrameDecoder.create(), Hex.bytes("00")), Reason.FRAME_LENGTH_INVALID);
    assertFails(
        () -> Feeds.whole(FrameDecoder.create(), Hex.bytes("01 00 00")),
        Reason.FRAME_LENGTH_INVALID);
  }

  @Test
  void provisionalALengthAboveTheMaximumIsRefused() throws ProtocolException {
    // spec: R27, API contract of FrameLimits (maxFrameLength)
    FrameLimits limits = new FrameLimits(100, FrameLimits.MAX_DECOMPRESSED_LENGTH);
    byte[] atLimit = Hex.concat(Hex.bytes("64 00"), new byte[99]);
    assertThat(Feeds.whole(FrameDecoder.create(limits), atLimit)).hasSize(1);
    assertFails(
        () -> Feeds.whole(FrameDecoder.create(limits), Hex.bytes("65")),
        Reason.FRAME_LENGTH_INVALID);
  }

  @Test
  void provisionalAFailedDecoderIsUnusable() {
    // spec: API contract of FrameDecoder (after any exception every later call throws)
    FrameDecoder decoder = FrameDecoder.create();
    assertFails(() -> Feeds.whole(decoder, Hex.bytes("00")), Reason.FRAME_LENGTH_INVALID);
    assertThrows(IllegalStateException.class, () -> Feeds.whole(decoder, Hex.bytes("01 00")));
  }

  @Test
  void provisionalFramesCompletedBeforeAFaultInTheSameCallAreLostWithIt() {
    // spec: API contract of feed
    FrameDecoder decoder = FrameDecoder.create();
    assertFails(() -> Feeds.whole(decoder, Hex.bytes("01 00 00")), Reason.FRAME_LENGTH_INVALID);
  }

  @Test
  void theCallerArrayIsNeverModified() throws ProtocolException {
    // spec: API contract of feed (not modified, not kept), also with decryption
    byte[] data = Cfb8Model.encrypt(SECRET, Hex.bytes("01 00 01 03"));
    byte[] before = data.clone();
    FrameDecoder decoder = FrameDecoder.create();
    decoder.enableDecryption(Cfb8Ciphers.decryptor(SECRET));
    assertThat(Feeds.whole(decoder, data)).hasSize(2);
    assertThat(data).isEqualTo(before);
  }

  @Test
  void decryptionReadsTheEncryptedFrameOfVectorCh01() throws ProtocolException {
    // spec: R31, vector CH01
    FrameDecoder decoder = FrameDecoder.create();
    decoder.enableDecryption(Cfb8Ciphers.decryptor(SECRET));
    byte[] encrypted =
        Hex.bytes("1d 16 6a dd 3a ef 9a bb 54 7c 35 d3 d2 49 fa d3 5a a2 be 70 f7 52 0f da");
    Vector notch = FrameVectors.uncompressed().get(4);
    assertThat(Feeds.byteByByte(decoder, encrypted))
        .containsExactly(Frame.of(notch.id(), notch.payload()));
  }

  @Test
  void decryptionContinuesFromOneFrameToTheNextAcrossAnyChunking() throws ProtocolException {
    // spec: R31 (one continuous stream, never restarted), R32
    List<Vector> vectors = FrameVectors.uncompressed();
    byte[] encrypted = Cfb8Model.encrypt(SECRET, stream(vectors));
    for (int chunk : new int[] {1, 2, 3, 7, 16, 1000}) {
      FrameDecoder decoder = FrameDecoder.create();
      decoder.enableDecryption(Cfb8Ciphers.decryptor(SECRET));
      assertThat(Feeds.chunked(decoder, encrypted, chunk))
          .as("chunk %d", chunk)
          .containsExactlyElementsOf(expected(vectors));
    }
  }

  @Test
  void decryptionStartedMidStreamDecryptsOnlyTheFollowingBytes() throws ProtocolException {
    // spec: R32
    FrameDecoder decoder = FrameDecoder.create();
    assertThat(Feeds.whole(decoder, Hex.bytes("01 00"))).hasSize(1);
    decoder.enableDecryption(Cfb8Ciphers.decryptor(SECRET));
    byte[] encrypted = Cfb8Model.encrypt(SECRET, Hex.bytes("01 03"));
    assertThat(Feeds.whole(decoder, encrypted)).containsExactly(Frame.of(3, new byte[0]));
  }

  @Test
  void enablingDecryptionTwiceIsRefused() {
    // spec: API contract of enableDecryption
    FrameDecoder decoder = FrameDecoder.create();
    decoder.enableDecryption(Cfb8Ciphers.decryptor(SECRET));
    assertThrows(
        IllegalStateException.class, () -> decoder.enableDecryption(Cfb8Ciphers.decryptor(SECRET)));
  }
}
