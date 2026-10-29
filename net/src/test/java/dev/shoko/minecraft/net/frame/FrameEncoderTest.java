package dev.shoko.minecraft.net.frame;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static dev.shoko.minecraft.net.testing.Failures.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.buffer.PacketReader;
import dev.shoko.minecraft.net.testing.Cfb8Model;
import dev.shoko.minecraft.net.testing.FrameVectors;
import dev.shoko.minecraft.net.testing.FrameVectors.Vector;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Wire;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// spec: net-protocol-777 R27 to R32 (encoding side), vectors F01 to F16, CH01
class FrameEncoderTest {

  private static final byte[] SECRET = Hex.ramp(16);

  static Stream<Vector> vectors() {
    return FrameVectors.uncompressed().stream();
  }

  /**
   * Checks the compressed form of R29 on the bytes of one frame: the length covers the rest, then
   * the data length, then a zlib stream that inflates to the id and payload.
   */
  private static void assertCompressedFrame(
      byte[] frame, int id, byte[] payload, int expectedDataLength) throws ProtocolException {
    PacketReader in = PacketReader.wrap(frame);
    int length = in.readVarInt();
    assertThat(in.remaining()).as("length field covers the rest").isEqualTo(length);
    int dataLength = in.readVarInt();
    assertThat(dataLength).isEqualTo(expectedDataLength);
    int streamStart = frame.length - in.remaining();
    byte[] body = Wire.inflate(frame, streamStart, dataLength);
    assertThat(body).isEqualTo(Hex.concat(Wire.varInt(id), payload));
  }

  @ParameterizedTest
  @MethodSource("vectors")
  void withoutCompressionAFrameIsLengthIdPayload(Vector vector) throws ProtocolException {
    // spec: R27, vectors F01 to F08, F15, F16
    byte[] bytes = FrameEncoder.create().encode(Frame.of(vector.id(), vector.payload()));
    assertThat(Hex.format(bytes)).isEqualTo(Hex.format(vector.frame()));
  }

  @Test
  void aMultiByteIdIsAVarInt() throws ProtocolException {
    // spec: R27 (VarInt packet id)
    byte[] bytes = FrameEncoder.create().encode(Frame.of(200, Hex.bytes("aa bb cc")));
    assertThat(Hex.format(bytes)).isEqualTo("05 c8 01 aa bb cc");
  }

  @Test
  void aLengthAbove127TakesTwoVarIntBytes() throws ProtocolException {
    // spec: R27 (the length is a VarInt)
    byte[] payload = Hex.ramp(255);
    byte[] bytes = FrameEncoder.create().encode(Frame.of(0, Hex.concat(payload, payload)));
    assertThat(Hex.format(Arrays.copyOf(bytes, 4))).isEqualTo("ff 03 00 00");
    assertThat(bytes).hasSize(2 + 511);
  }

  @Test
  void aNegativeThresholdMeansNoCompression() throws ProtocolException {
    // spec: R28, vector F14 (same bytes as F02)
    FrameEncoder encoder = FrameEncoder.create();
    encoder.setCompressionThreshold(-1);
    assertThat(Hex.format(encoder.encode(Frame.of(0, new byte[0])))).isEqualTo("01 00");
    encoder.setCompressionThreshold(Integer.MIN_VALUE);
    assertThat(Hex.format(encoder.encode(Frame.of(0, new byte[0])))).isEqualTo("01 00");
  }

  @Test
  void aBodyBelowTheThresholdIsSentPlainWithADataLengthOfZero() throws ProtocolException {
    // spec: R29, vector F09 (10 byte payload, id 0x7c, threshold 256)
    FrameEncoder encoder = FrameEncoder.create();
    encoder.setCompressionThreshold(256);
    byte[] bytes = encoder.encode(Frame.of(0x7c, Hex.ramp(10)));
    assertThat(Hex.format(bytes)).isEqualTo("0c 00 7c 00 01 02 03 04 05 06 07 08 09");
  }

  @Test
  void aBodyOneByteBelowTheThresholdIsStillPlain() throws ProtocolException {
    // spec: R29, vector F11 (body of 255 bytes, threshold 256)
    FrameEncoder encoder = FrameEncoder.create();
    encoder.setCompressionThreshold(256);
    byte[] payload = Hex.filled('B', 254);
    byte[] bytes = encoder.encode(Frame.of(0x7c, payload));
    assertThat(Hex.format(Arrays.copyOf(bytes, 4))).isEqualTo("80 02 00 7c");
    assertThat(bytes).hasSize(2 + 256);
    assertThat(Arrays.copyOfRange(bytes, 4, bytes.length)).isEqualTo(payload);
  }

  @Test
  void aBodyAtTheThresholdIsCompressedWithItsUncompressedSize() throws ProtocolException {
    // spec: R29, vector F12 (body of 256 bytes, threshold 256)
    FrameEncoder encoder = FrameEncoder.create();
    encoder.setCompressionThreshold(256);
    byte[] payload = Hex.filled('B', 255);
    assertCompressedFrame(encoder.encode(Frame.of(0x7c, payload)), 0x7c, payload, 256);
  }

  @Test
  void aBodyAboveTheThresholdIsCompressed() throws ProtocolException {
    // spec: R29, vector F10 (300 bytes of 'A', id 0x7c: data length 301, "ad 02")
    FrameEncoder encoder = FrameEncoder.create();
    encoder.setCompressionThreshold(256);
    byte[] payload = Hex.filled('A', 300);
    byte[] bytes = encoder.encode(Frame.of(0x7c, payload));
    assertThat(bytes.length).as("the 300 identical bytes compress well").isLessThan(60);
    assertCompressedFrame(bytes, 0x7c, payload, 301);
  }

  @Test
  void aThresholdOfZeroCompressesEveryBodyEvenAnEmptyPayload() throws ProtocolException {
    // spec: R29, vector F13 (threshold 0, empty payload, id 0: data length 1)
    FrameEncoder encoder = FrameEncoder.create();
    encoder.setCompressionThreshold(0);
    assertCompressedFrame(encoder.encode(Frame.of(0, new byte[0])), 0, new byte[0], 1);
  }

  @Test
  void aThresholdOfOneCompressesABodyOfOneByte() throws ProtocolException {
    // spec: R29 (a body of at least the threshold is compressed)
    FrameEncoder encoder = FrameEncoder.create();
    encoder.setCompressionThreshold(1);
    assertCompressedFrame(encoder.encode(Frame.of(5, new byte[0])), 5, new byte[0], 1);
    assertCompressedFrame(encoder.encode(Frame.of(5, new byte[] {9})), 5, new byte[] {9}, 2);
  }

  @Test
  void theThresholdAppliesFromTheFollowingFrames() throws ProtocolException {
    // spec: R28 (login_compression is sent plain, the next packets follow its threshold)
    FrameEncoder encoder = FrameEncoder.create();
    byte[] plain = encoder.encode(Frame.of(3, Hex.bytes("80 02")));
    assertThat(Hex.format(plain)).isEqualTo("03 03 80 02");
    encoder.setCompressionThreshold(256);
    assertThat(Hex.format(encoder.encode(Frame.of(0x7c, Hex.ramp(10)))))
        .isEqualTo("0c 00 7c 00 01 02 03 04 05 06 07 08 09");
    encoder.setCompressionThreshold(-1);
    assertThat(Hex.format(encoder.encode(Frame.of(0, new byte[0])))).isEqualTo("01 00");
  }

  @Test
  void theCompressedStreamOfAnIncompressiblePayloadInflatesBack() throws ProtocolException {
    // spec: R29 (the decompressor reads any valid zlib stream)
    byte[] payload = new byte[5000];
    new Random(0xF1AL).nextBytes(payload);
    FrameEncoder encoder = FrameEncoder.create();
    encoder.setCompressionThreshold(256);
    assertCompressedFrame(encoder.encode(Frame.of(9, payload)), 9, payload, 5001);
  }

  @Test
  void sameInputGivesSameBytes() throws ProtocolException {
    // spec: ARCHITECTURE determinism (two encoders, one input, the same bytes)
    byte[] payload = Hex.filled('Z', 1000);
    FrameEncoder first = FrameEncoder.create();
    FrameEncoder second = FrameEncoder.create();
    first.setCompressionThreshold(256);
    second.setCompressionThreshold(256);
    assertThat(first.encode(Frame.of(1, payload))).isEqualTo(second.encode(Frame.of(1, payload)));
  }

  @Test
  void theMaximumFrameLengthIsAccepted() throws ProtocolException {
    // spec: R27, R85 (2^21 - 1 octets is valid), vector N08 (ff ff 7f)
    byte[] payload = new byte[FrameLimits.MAX_FRAME_LENGTH - 1];
    byte[] bytes = FrameEncoder.create().encode(Frame.of(0, payload));
    assertThat(Hex.format(Arrays.copyOf(bytes, 4))).isEqualTo("ff ff 7f 00");
    assertThat(bytes).hasSize(3 + FrameLimits.MAX_FRAME_LENGTH);
  }

  @Test
  void aLengthAboveTheMaximumIsRefused() {
    // spec: R27 (FRAME_LENGTH_INVALID), vector N09 (2097152 needs 4 bytes)
    byte[] payload = new byte[FrameLimits.MAX_FRAME_LENGTH];
    assertFails(
        () -> FrameEncoder.create().encode(Frame.of(0, payload)), Reason.FRAME_LENGTH_INVALID);
  }

  @Test
  void anExplicitLimitIsHonored() throws ProtocolException {
    // spec: API contract of FrameLimits (a smaller value lets a test exercise a limit)
    FrameEncoder encoder =
        FrameEncoder.create(new FrameLimits(100, FrameLimits.MAX_DECOMPRESSED_LENGTH));
    assertThat(encoder.encode(Frame.of(0, new byte[99]))).hasSize(1 + 100);
    assertFails(() -> encoder.encode(Frame.of(0, new byte[100])), Reason.FRAME_LENGTH_INVALID);
  }

  @Test
  void withCompressionTheLimitAppliesToTheCompressedLength() throws ProtocolException {
    // spec: R30 ("le plafond de longueur de R27 s'applique a la longueur compressee")
    FrameEncoder encoder =
        FrameEncoder.create(new FrameLimits(60, FrameLimits.MAX_DECOMPRESSED_LENGTH));
    encoder.setCompressionThreshold(0);
    byte[] repetitive = Hex.filled('A', 1000);
    assertCompressedFrame(encoder.encode(Frame.of(1, repetitive)), 1, repetitive, 1001);
    byte[] random = new byte[200];
    new Random(0x1DL).nextBytes(random);
    assertFails(() -> encoder.encode(Frame.of(1, random)), Reason.FRAME_LENGTH_INVALID);
  }

  @Test
  void encryptionCoversEveryByteOfTheFrame() throws ProtocolException {
    // spec: R31, vector CH01 (the encrypted frame of F05)
    FrameEncoder encoder = FrameEncoder.create();
    encoder.enableEncryption(Cfb8Ciphers.encryptor(SECRET));
    Vector notch = FrameVectors.uncompressed().get(4);
    byte[] bytes = encoder.encode(Frame.of(notch.id(), notch.payload()));
    assertThat(Hex.format(bytes))
        .isEqualTo("1d 16 6a dd 3a ef 9a bb 54 7c 35 d3 d2 49 fa d3 5a a2 be 70 f7 52 0f da");
  }

  @Test
  void theEncryptionStreamContinuesFromOneFrameToTheNext() throws ProtocolException {
    // spec: R31 (never restarted between packets)
    List<Vector> vectors = FrameVectors.uncompressed();
    FrameEncoder encoder = FrameEncoder.create();
    encoder.enableEncryption(Cfb8Ciphers.encryptor(SECRET));
    byte[] plain = new byte[0];
    byte[] encrypted = new byte[0];
    for (Vector vector : vectors) {
      plain = Hex.concat(plain, vector.frame());
      encrypted = Hex.concat(encrypted, encoder.encode(Frame.of(vector.id(), vector.payload())));
    }
    assertThat(encrypted).isEqualTo(Cfb8Model.encrypt(SECRET, plain));
  }

  @Test
  void compressionIsAppliedBeforeEncryption() throws ProtocolException {
    // spec: R31 ("compresser puis chiffrer"), R32
    FrameEncoder encoder = FrameEncoder.create();
    encoder.setCompressionThreshold(256);
    encoder.enableEncryption(Cfb8Ciphers.encryptor(SECRET));
    byte[] bytes = encoder.encode(Frame.of(0x7c, Hex.ramp(10)));
    byte[] plain = Hex.bytes("0c 00 7c 00 01 02 03 04 05 06 07 08 09");
    assertThat(bytes).isEqualTo(Cfb8Model.encrypt(SECRET, plain));
  }

  @Test
  void encryptionStartedMidStreamEncryptsOnlyTheFollowingFrames() throws ProtocolException {
    // spec: R32 (the encryption starts after the key exchange)
    FrameEncoder encoder = FrameEncoder.create();
    byte[] before = encoder.encode(Frame.of(0, new byte[0]));
    assertThat(Hex.format(before)).isEqualTo("01 00");
    encoder.enableEncryption(Cfb8Ciphers.encryptor(SECRET));
    byte[] after = encoder.encode(Frame.of(3, new byte[0]));
    assertThat(after).isEqualTo(Cfb8Model.encrypt(SECRET, Hex.bytes("01 03")));
  }

  @Test
  void enablingEncryptionTwiceIsRefused() {
    // spec: API contract of enableEncryption
    FrameEncoder encoder = FrameEncoder.create();
    encoder.enableEncryption(Cfb8Ciphers.encryptor(SECRET));
    assertThrows(
        IllegalStateException.class, () -> encoder.enableEncryption(Cfb8Ciphers.encryptor(SECRET)));
  }
}
