package dev.shoko.minecraft.net.buffer;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static dev.shoko.minecraft.net.testing.Failures.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.testing.Buffers;
import dev.shoko.minecraft.net.testing.Buffers.Write;
import dev.shoko.minecraft.net.testing.Hex;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// spec: net-protocol-777 R5, R6, R7, R11 (write side), vectors N01 to N19
class PacketWriterTest {

  @Test
  void aNewWriterIsEmpty() {
    // spec: R27 (the default bound is the frame maximum)
    PacketWriter out = PacketWriter.create();
    assertThat(out.size()).isZero();
    assertThat(out.toByteArray()).isEmpty();
    assertThat(PacketWriter.DEFAULT_MAX_SIZE).isEqualTo(2_097_151);
  }

  @ParameterizedTest
  @CsvSource({
    "0, 00",
    "1, 01",
    "127, 7f",
    "128, 80 01",
    "255, ff 01",
    "777, 89 06",
    "25565, dd c7 01",
    "2097151, ff ff 7f",
    "2097152, 80 80 80 01",
    "2147483647, ff ff ff ff 07",
    "-1, ff ff ff ff 0f",
    "-2147483648, 80 80 80 80 08"
  })
  void writeVarIntMatchesTheVectors(int value, String hex) throws ProtocolException {
    // spec: R6, vectors N01 to N12
    byte[] bytes = Buffers.write(out -> out.writeVarInt(value));
    assertThat(Hex.format(bytes)).isEqualTo(hex);
  }

  @ParameterizedTest
  @CsvSource({
    "0, 00",
    "127, 7f",
    "128, 80 01",
    "9223372036854775807, ff ff ff ff ff ff ff ff 7f",
    "-1, ff ff ff ff ff ff ff ff ff 01",
    "-2147483648, 80 80 80 80 f8 ff ff ff ff 01",
    "-9223372036854775808, 80 80 80 80 80 80 80 80 80 01"
  })
  void writeVarLongMatchesTheVectors(long value, String hex) throws ProtocolException {
    // spec: R7, vectors N13 to N19
    byte[] bytes = Buffers.write(out -> out.writeVarLong(value));
    assertThat(Hex.format(bytes)).isEqualTo(hex);
  }

  @Test
  void negativeVarIntAlwaysTakesFiveBytesAndNegativeVarLongTen() throws ProtocolException {
    // spec: R6, R7
    assertThat(Buffers.write(out -> out.writeVarInt(-1))).hasSize(5);
    assertThat(Buffers.write(out -> out.writeVarInt(Integer.MIN_VALUE))).hasSize(5);
    assertThat(Buffers.write(out -> out.writeVarLong(-1L))).hasSize(10);
    assertThat(Buffers.write(out -> out.writeVarLong(Long.MIN_VALUE))).hasSize(10);
  }

  @Test
  void scalarsAreBigEndian() throws ProtocolException {
    // spec: R5
    assertThat(Hex.format(Buffers.write(out -> out.writeShort(0x1234)))).isEqualTo("12 34");
    assertThat(Hex.format(Buffers.write(out -> out.writeInt(0x01020304)))).isEqualTo("01 02 03 04");
    assertThat(Hex.format(Buffers.write(out -> out.writeLong(0x0123456789abcdefL))))
        .isEqualTo("01 23 45 67 89 ab cd ef");
    assertThat(Hex.format(Buffers.write(out -> out.writeFloat(1.0f)))).isEqualTo("3f 80 00 00");
    assertThat(Hex.format(Buffers.write(out -> out.writeDouble(1.0))))
        .isEqualTo("3f f0 00 00 00 00 00 00");
    assertThat(Hex.format(Buffers.write(out -> out.writeInt(-2)))).isEqualTo("ff ff ff fe");
  }

  @Test
  void booleanIsWrittenAsZeroOrOne() throws ProtocolException {
    // spec: R5
    assertThat(Hex.format(Buffers.write(out -> out.writeBoolean(false)))).isEqualTo("00");
    assertThat(Hex.format(Buffers.write(out -> out.writeBoolean(true)))).isEqualTo("01");
  }

  @Test
  void byteAndShortKeepTheLowBitsSoSignedAndUnsignedBothWork() throws ProtocolException {
    // spec: API contract of writeByte and writeShort, R5
    assertThat(Hex.format(Buffers.write(out -> out.writeByte(0xff)))).isEqualTo("ff");
    assertThat(Hex.format(Buffers.write(out -> out.writeByte(-1)))).isEqualTo("ff");
    assertThat(Hex.format(Buffers.write(out -> out.writeByte(0x1ab)))).isEqualTo("ab");
    assertThat(Hex.format(Buffers.write(out -> out.writeShort(65535)))).isEqualTo("ff ff");
    assertThat(Hex.format(Buffers.write(out -> out.writeShort(-2)))).isEqualTo("ff fe");
    assertThat(Hex.format(Buffers.write(out -> out.writeShort(0x12345)))).isEqualTo("23 45");
  }

  @Test
  void floatingPointKeepsTheExactBitPattern() throws ProtocolException {
    // spec: R5 (IEEE 754, bit pattern kept)
    float payloadNan = Float.intBitsToFloat(0x7fc00001);
    double payloadDoubleNan = Double.longBitsToDouble(0x7ff8000000000001L);
    assertThat(Hex.format(Buffers.write(out -> out.writeFloat(payloadNan))))
        .isEqualTo("7f c0 00 01");
    assertThat(Hex.format(Buffers.write(out -> out.writeDouble(payloadDoubleNan))))
        .isEqualTo("7f f8 00 00 00 00 00 01");
    assertThat(Hex.format(Buffers.write(out -> out.writeFloat(-0.0f)))).isEqualTo("80 00 00 00");
  }

  @Test
  void uuidIsMostSignificantLongThenLeastSignificantLong() throws ProtocolException {
    // spec: R11, vector U01 bytes (the offline UUID of Notch as it appears in F05)
    UUID notch = UUID.fromString("b50ad385-829d-3141-a216-7e7d7539ba7f");
    assertThat(Hex.format(Buffers.write(out -> out.writeUuid(notch))))
        .isEqualTo("b5 0a d3 85 82 9d 31 41 a2 16 7e 7d 75 39 ba 7f");
    UUID mixed = new UUID(0x0123456789abcdefL, 0xfedcba9876543210L);
    assertThat(Hex.format(Buffers.write(out -> out.writeUuid(mixed))))
        .isEqualTo("01 23 45 67 89 ab cd ef fe dc ba 98 76 54 32 10");
  }

  @Test
  void toByteArrayReturnsACopyOfWhatWasWritten() throws ProtocolException {
    // spec: API contract of toByteArray and size
    PacketWriter out = PacketWriter.create();
    out.writeInt(0x01020304);
    byte[] first = out.toByteArray();
    first[0] = 0x7f;
    assertThat(out.size()).isEqualTo(4);
    assertThat(Hex.format(out.toByteArray())).isEqualTo("01 02 03 04");
    out.writeByte(5);
    assertThat(out.size()).isEqualTo(5);
    assertThat(Hex.format(out.toByteArray())).isEqualTo("01 02 03 04 05");
  }

  @Test
  void writeRemainingBytesAddsTheBytesWithoutALength() throws ProtocolException {
    // spec: R14 ("tableau sans prefixe"), R87 (data up to the end of the frame)
    byte[] bytes =
        Buffers.write(out -> out.writeRemainingBytes(ByteBlob.of(Hex.bytes("aa bb cc"))));
    assertThat(Hex.format(bytes)).isEqualTo("aa bb cc");
  }

  @Test
  void aNegativeBoundIsRefused() {
    // spec: API contract of create(int)
    assertThrows(IllegalArgumentException.class, () -> PacketWriter.create(-1));
  }

  record SizedWrite(String label, int size, Write write) {
    @Override
    public String toString() {
      return label;
    }
  }

  static List<SizedWrite> sizedWrites() {
    BitSet empty = new BitSet();
    return List.of(
        new SizedWrite("boolean", 1, out -> out.writeBoolean(true)),
        new SizedWrite("byte", 1, out -> out.writeByte(1)),
        new SizedWrite("short", 2, out -> out.writeShort(1)),
        new SizedWrite("int", 4, out -> out.writeInt(1)),
        new SizedWrite("long", 8, out -> out.writeLong(1)),
        new SizedWrite("float", 4, out -> out.writeFloat(1)),
        new SizedWrite("double", 8, out -> out.writeDouble(1)),
        new SizedWrite("varint", 2, out -> out.writeVarInt(300)),
        new SizedWrite("varlong", 2, out -> out.writeVarLong(300)),
        new SizedWrite("string", 3, out -> out.writeString("hi", 16)),
        new SizedWrite("uuid", 16, out -> out.writeUuid(new UUID(1, 2))),
        new SizedWrite(
            "prefixed bytes", 4, out -> out.writePrefixedBytes(ByteBlob.of(new byte[3]), 16)),
        new SizedWrite(
            "remaining bytes", 3, out -> out.writeRemainingBytes(ByteBlob.of(new byte[3]))),
        new SizedWrite("bit set", 1, out -> out.writeBitSet(empty)),
        new SizedWrite("fixed bit set", 3, out -> out.writeFixedBitSet(empty, 20)),
        new SizedWrite("nbt", 5, out -> out.writeNbt(new StringTag("hi"))),
        new SizedWrite("absent nbt", 1, out -> out.writeAbsentNbt()),
        new SizedWrite("absent optional", 1, out -> out.writeAbsent()),
        new SizedWrite(
            "array", 3, out -> out.writePrefixedArray(List.of(1, 2), 4, (w, v) -> w.writeByte(v))));
  }

  @Test
  void aWriteThatFitsTheBoundExactlySucceedsAndOneByteLessFails() throws ProtocolException {
    // spec: API contract of the writer bound (PAYLOAD_TOO_LARGE), R27
    for (SizedWrite sized : sizedWrites()) {
      PacketWriter exact = PacketWriter.create(sized.size());
      sized.write().accept(exact);
      assertThat(exact.size()).as(sized.label()).isEqualTo(sized.size());
      PacketWriter tight = PacketWriter.create(sized.size() - 1);
      assertFails(() -> sized.write().accept(tight), Reason.PAYLOAD_TOO_LARGE);
    }
  }

  @Test
  void aSecondWriteThatCrossesTheBoundFails() throws ProtocolException {
    // spec: API contract of the writer bound
    PacketWriter out = PacketWriter.create(4);
    out.writeInt(7);
    assertFails(() -> out.writeByte(1), Reason.PAYLOAD_TOO_LARGE);
  }

  @Test
  void aZeroBoundWriterAcceptsNothing() throws ProtocolException {
    // spec: API contract of create(int), maxSize 0 or more
    PacketWriter out = PacketWriter.create(0);
    assertThat(out.size()).isZero();
    assertFails(() -> out.writeBoolean(true), Reason.PAYLOAD_TOO_LARGE);
  }
}
