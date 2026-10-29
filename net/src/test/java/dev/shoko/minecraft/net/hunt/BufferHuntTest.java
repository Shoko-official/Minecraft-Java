package dev.shoko.minecraft.net.hunt;

import static dev.shoko.minecraft.net.testing.Failures.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.within;

import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.buffer.PacketReader;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.frame.FrameDecoder;
import dev.shoko.minecraft.net.packet.PacketCodecs;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;
import dev.shoko.minecraft.net.testing.Buffers;
import dev.shoko.minecraft.net.testing.Failures;
import dev.shoko.minecraft.net.testing.Feeds;
import dev.shoko.minecraft.net.testing.Hex;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R18 (LpVec3 boundaries), R27 (frame length), R16 and API contracts of
// Frame.of and readFixedBitSet (invalid arguments)
class BufferHuntTest {

  private static final double MIN_MAGNITUDE = 1.0 / 32766;

  @Test
  void aVectorAtExactlyOneOver32766IsNotTheZeroVector() throws ProtocolException {
    // spec: R18 ("nul si ce maximum est inferieur a 1/32766": equal is not below)
    byte[] atLimit = Buffers.write(out -> out.writeLpVec3(new Vec3(MIN_MAGNITUDE, 0, 0)));
    assertThat(atLimit).hasSize(6);
    byte[] negative = Buffers.write(out -> out.writeLpVec3(new Vec3(0, -MIN_MAGNITUDE, 0)));
    assertThat(negative).hasSize(6);
  }

  @Test
  void aVectorJustBelowOneOver32766IsTheZeroVector() throws ProtocolException {
    // spec: R18
    double below = Math.nextDown(MIN_MAGNITUDE);
    assertThat(Hex.format(Buffers.write(out -> out.writeLpVec3(new Vec3(below, -below, below)))))
        .isEqualTo("00");
  }

  /** The 48-bit word of R18 for a scale of 1 with three raw 15-bit components, as 6 bytes. */
  private static String word(long x, long y, long z) {
    long packed = 1 | (x << 3) | (y << 18) | (z << 33);
    return String.format(
        "%02x %02x %02x %02x %02x %02x",
        packed & 0xff,
        (packed >> 8) & 0xff,
        (packed >> 40) & 0xff,
        (packed >> 32) & 0xff,
        (packed >> 24) & 0xff,
        (packed >> 16) & 0xff);
  }

  @Test
  void aRawComponentOf32767DecodesAsTheMaximum() throws ProtocolException {
    // spec: R18 (decoding of a component: min(q, 32766) * 2 / 32766 - 1, times the scale)
    Vec3 decoded = Buffers.readAll(word(32767, 32767, 32767), PacketReader::readLpVec3);
    assertThat(decoded.x()).isCloseTo(1.0, within(1e-12));
    assertThat(decoded.y()).isCloseTo(1.0, within(1e-12));
    assertThat(decoded.z()).isCloseTo(1.0, within(1e-12));
    Vec3 maximum = Buffers.readAll(word(32766, 32766, 32766), PacketReader::readLpVec3);
    assertThat(maximum).isEqualTo(decoded);
  }

  @Test
  void aRawComponentJustBelowTheMaximumIsNotClamped() throws ProtocolException {
    // spec: R18
    Vec3 decoded = Buffers.readAll(word(32765, 0, 16383), PacketReader::readLpVec3);
    assertThat(decoded.x()).isLessThan(1.0).isCloseTo(1.0, within(1e-3));
    assertThat(decoded.y()).isCloseTo(-1.0, within(1e-12));
    assertThat(decoded.z()).isCloseTo(0.0, within(1e-12));
  }

  @Test
  void aFrameLengthWithAContinuationOnItsThirdByteIsRefusedAtOnce() {
    // spec: R27 (at most 3 bytes): the third byte still continues, no fourth byte is awaited
    FrameDecoder decoder = FrameDecoder.create();
    Failures.assertFails(
        () -> decoder.feed(Hex.bytes("80 80 80"), 0, 3),
        ProtocolException.Reason.FRAME_LENGTH_INVALID);
    FrameDecoder oneByOne = FrameDecoder.create();
    Failures.assertFails(
        () -> Feeds.byteByByte(oneByOne, Hex.bytes("ff ff ff")),
        ProtocolException.Reason.FRAME_LENGTH_INVALID);
  }

  @Test
  void aNegativePacketIdIsRefusedByFrameOf() {
    // spec: API contract of Frame.of ("the VarInt packet id, 0 or more")
    for (int id : new int[] {-1, -2, Integer.MIN_VALUE}) {
      assertThrows(IllegalArgumentException.class, () -> Frame.of(id, new byte[0]));
    }
    assertThat(Frame.of(0, new byte[0]).packetId()).isZero();
    assertThat(Frame.of(Integer.MAX_VALUE, new byte[0]).packetId()).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void aNegativeBitCountOfAFixedBitSetIsTheCallersErrorNotAnIndexError() {
    // spec: R16, API contract of readFixedBitSet (bits is 0 or more): a typed protocol error or an
    // illegal argument, never an index out of bounds from inside the reader
    for (int bits : new int[] {-1, -8, Integer.MIN_VALUE}) {
      Throwable thrown = catchThrowable(() -> Buffers.reader("ff ff ff ff").readFixedBitSet(bits));
      assertThat(thrown)
          .as("bits = %d", bits)
          .isNotNull()
          .isNotInstanceOf(IndexOutOfBoundsException.class)
          .isInstanceOfAny(ProtocolException.class, IllegalArgumentException.class);
    }
  }

  @Test
  void aWireFrameWithANegativeIdVarIntIsRefusedAsAnUnknownPacketId() throws ProtocolException {
    // spec: R2, R49. The id 5 bytes "ff ff ff ff 0f" is -1: no packet has it, and no Frame can.
    FrameDecoder decoder = FrameDecoder.create();
    ProtocolException refused =
        Failures.failure(
            () -> {
              for (Frame frame : decoder.feed(Hex.bytes("05 ff ff ff ff 0f"), 0, 6)) {
                PacketCodecs.decode(ConnectionState.STATUS, PacketDirection.SERVERBOUND, frame);
              }
            });
    assertThat(refused.reason()).isEqualTo(ProtocolException.Reason.UNKNOWN_PACKET_ID);
  }
}
