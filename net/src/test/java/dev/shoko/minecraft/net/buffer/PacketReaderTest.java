package dev.shoko.minecraft.net.buffer;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static dev.shoko.minecraft.net.testing.Failures.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.testing.Buffers;
import dev.shoko.minecraft.net.testing.Buffers.Read;
import dev.shoko.minecraft.net.testing.Hex;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// spec: net-protocol-777 R5, R6, R7, R8, R90 (read side), vectors N01 to N22
class PacketReaderTest {

  @ParameterizedTest
  @CsvSource({
    "00, 0",
    "01, 1",
    "7f, 127",
    "80 01, 128",
    "ff 01, 255",
    "89 06, 777",
    "dd c7 01, 25565",
    "ff ff 7f, 2097151",
    "80 80 80 01, 2097152",
    "ff ff ff ff 07, 2147483647",
    "ff ff ff ff 0f, -1",
    "80 80 80 80 08, -2147483648"
  })
  void readVarIntMatchesTheVectors(String hex, int value) throws ProtocolException {
    // spec: R6, vectors N01 to N12
    assertThat(Buffers.readAll(hex, PacketReader::readVarInt)).isEqualTo(value);
  }

  @ParameterizedTest
  @CsvSource({
    "00, 0",
    "7f, 127",
    "80 01, 128",
    "ff ff ff ff ff ff ff ff 7f, 9223372036854775807",
    "ff ff ff ff ff ff ff ff ff 01, -1",
    "80 80 80 80 f8 ff ff ff ff 01, -2147483648",
    "80 80 80 80 80 80 80 80 80 01, -9223372036854775808"
  })
  void readVarLongMatchesTheVectors(String hex, long value) throws ProtocolException {
    // spec: R7, vectors N13 to N19
    assertThat(Buffers.readAll(hex, PacketReader::readVarLong)).isEqualTo(value);
  }

  @Test
  void provisionalOverlongVarIntsAreAcceptedWithinFiveBytes() throws ProtocolException {
    // spec: R8, vector N20
    assertThat(Buffers.readAll("80 80 00", PacketReader::readVarInt)).isZero();
    assertThat(Buffers.readAll("80 80 80 80 00", PacketReader::readVarInt)).isZero();
    assertThat(Buffers.readAll("81 80 80 80 00", PacketReader::readVarInt)).isEqualTo(1);
  }

  @Test
  void provisionalOverlongVarLongsAreAcceptedWithinTenBytes() throws ProtocolException {
    // spec: R8 (VarLong: 10 bytes)
    assertThat(Buffers.readAll("80 80 00", PacketReader::readVarLong)).isZero();
    assertThat(Buffers.readAll("80 80 80 80 80 80 80 80 80 00", PacketReader::readVarLong))
        .isZero();
  }

  @Test
  void provisionalAVarIntOfSixBytesIsTooBig() {
    // spec: R8, vector N21
    assertFails(
        () -> Buffers.readAll("80 80 80 80 80 80 00", PacketReader::readVarInt),
        Reason.VAR_INT_TOO_BIG);
    assertFails(
        () -> Buffers.readAll("ff ff ff ff ff ff ff ff ff ff", PacketReader::readVarInt),
        Reason.VAR_INT_TOO_BIG);
  }

  @Test
  void provisionalAVarLongOfElevenBytesIsTooBig() {
    // spec: R8 (VarLong: 10 bytes)
    assertFails(
        () -> Buffers.readAll("80 80 80 80 80 80 80 80 80 80 00", PacketReader::readVarLong),
        Reason.VAR_INT_TOO_BIG);
  }

  @Test
  void provisionalBitsOfTheFifthByteBeyondThe32ndAreDropped() throws ProtocolException {
    // spec: R8, vector N22
    assertThat(Buffers.readAll("ff ff ff ff 7f", PacketReader::readVarInt)).isEqualTo(-1);
    assertThat(Buffers.readAll("80 80 80 80 70", PacketReader::readVarInt)).isZero();
  }

  @Test
  void aVarIntCutShortIsAnUnexpectedEnd() {
    // spec: R90, API contract of readVarInt and readVarLong
    assertFails(() -> Buffers.reader("").readVarInt(), Reason.UNEXPECTED_END);
    assertFails(() -> Buffers.reader("80").readVarInt(), Reason.UNEXPECTED_END);
    assertFails(() -> Buffers.reader("ff ff ff").readVarInt(), Reason.UNEXPECTED_END);
    assertFails(() -> Buffers.reader("").readVarLong(), Reason.UNEXPECTED_END);
    assertFails(() -> Buffers.reader("ff ff ff ff ff ff").readVarLong(), Reason.UNEXPECTED_END);
  }

  @Test
  void aVarIntConsumesExactlyItsBytes() throws ProtocolException {
    // spec: R6
    PacketReader in = Buffers.reader("dd c7 01 aa bb");
    assertThat(in.readVarInt()).isEqualTo(25565);
    assertThat(in.remaining()).isEqualTo(2);
    assertThat(in.readUnsignedByte()).isEqualTo(0xaa);
  }

  @Test
  void scalarsAreReadBigEndian() throws ProtocolException {
    // spec: R5
    assertThat(Buffers.readAll("12 34", PacketReader::readShort)).isEqualTo((short) 0x1234);
    assertThat(Buffers.readAll("ff fe", PacketReader::readShort)).isEqualTo((short) -2);
    assertThat(Buffers.readAll("ff fe", PacketReader::readUnsignedShort)).isEqualTo(65534);
    assertThat(Buffers.readAll("63 dd", PacketReader::readUnsignedShort)).isEqualTo(25565);
    assertThat(Buffers.readAll("01 02 03 04", PacketReader::readInt)).isEqualTo(0x01020304);
    assertThat(Buffers.readAll("ff ff ff fe", PacketReader::readInt)).isEqualTo(-2);
    assertThat(Buffers.readAll("01 23 45 67 89 ab cd ef", PacketReader::readLong))
        .isEqualTo(0x0123456789abcdefL);
    assertThat(Buffers.readAll("3f 80 00 00", PacketReader::readFloat)).isEqualTo(1.0f);
    assertThat(Buffers.readAll("3f f0 00 00 00 00 00 00", PacketReader::readDouble)).isEqualTo(1.0);
  }

  @Test
  void bytesAreSignedOrUnsignedByRequest() throws ProtocolException {
    // spec: R5
    assertThat(Buffers.readAll("ff", PacketReader::readByte)).isEqualTo((byte) -1);
    assertThat(Buffers.readAll("ff", PacketReader::readUnsignedByte)).isEqualTo(255);
    assertThat(Buffers.readAll("7f", PacketReader::readByte)).isEqualTo((byte) 127);
  }

  @Test
  void floatingPointKeepsTheExactBitPattern() throws ProtocolException {
    // spec: R5 (bit pattern kept)
    float nan = Buffers.readAll("7f c0 00 01", PacketReader::readFloat);
    assertThat(Float.floatToRawIntBits(nan)).isEqualTo(0x7fc00001);
    double doubleNan = Buffers.readAll("7f f8 00 00 00 00 00 01", PacketReader::readDouble);
    assertThat(Double.doubleToRawLongBits(doubleNan)).isEqualTo(0x7ff8000000000001L);
    float negativeZero = Buffers.readAll("80 00 00 00", PacketReader::readFloat);
    assertThat(Float.floatToRawIntBits(negativeZero)).isEqualTo(0x80000000);
  }

  @Test
  void booleanZeroIsFalseAndOneIsTrue() throws ProtocolException {
    // spec: R5
    assertThat(Buffers.readAll("00", PacketReader::readBoolean)).isFalse();
    assertThat(Buffers.readAll("01", PacketReader::readBoolean)).isTrue();
  }

  @Test
  void provisionalAnyNonZeroBooleanIsTrue() throws ProtocolException {
    // spec: R5 (probe PN7, input 02)
    assertThat(Buffers.readAll("02", PacketReader::readBoolean)).isTrue();
    assertThat(Buffers.readAll("ff", PacketReader::readBoolean)).isTrue();
    assertThat(Buffers.readAll("80", PacketReader::readBoolean)).isTrue();
  }

  @Test
  void everyScalarReadPastTheEndIsAnUnexpectedEnd() {
    // spec: R90
    List<NamedRead> reads =
        List.of(
            new NamedRead("boolean", 1, PacketReader::readBoolean),
            new NamedRead("byte", 1, PacketReader::readByte),
            new NamedRead("unsigned byte", 1, PacketReader::readUnsignedByte),
            new NamedRead("short", 2, PacketReader::readShort),
            new NamedRead("unsigned short", 2, PacketReader::readUnsignedShort),
            new NamedRead("int", 4, PacketReader::readInt),
            new NamedRead("long", 8, PacketReader::readLong),
            new NamedRead("float", 4, PacketReader::readFloat),
            new NamedRead("double", 8, PacketReader::readDouble),
            new NamedRead("uuid", 16, PacketReader::readUuid),
            new NamedRead("position", 8, PacketReader::readPosition));
    for (NamedRead named : reads) {
      byte[] shortInput = new byte[named.size() - 1];
      assertFails(() -> named.read().accept(PacketReader.wrap(shortInput)), Reason.UNEXPECTED_END);
    }
  }

  record NamedRead(String label, int size, Read<?> read) {
    @Override
    public String toString() {
      return label;
    }
  }

  @Test
  void remainingCountsTheUnreadBytesAndRequireFullyConsumedChecksIt() throws ProtocolException {
    // spec: R90
    PacketReader in = Buffers.reader("01 02 03");
    assertThat(in.remaining()).isEqualTo(3);
    assertFails(in::requireFullyConsumed, Reason.TRAILING_BYTES);
    PacketReader again = Buffers.reader("01 02 03");
    again.readUnsignedByte();
    assertThat(again.remaining()).isEqualTo(2);
    again.readShort();
    assertThat(again.remaining()).isZero();
    again.requireFullyConsumed();
  }

  @Test
  void anEmptyPayloadIsFullyConsumed() throws ProtocolException {
    // spec: R90
    PacketReader in = PacketReader.wrap(new byte[0]);
    assertThat(in.remaining()).isZero();
    in.requireFullyConsumed();
  }

  @Test
  void aReaderOnlySeesItsRange() throws ProtocolException {
    // spec: R90 (a packet is decoded from an array bounded to its frame)
    byte[] data = Hex.bytes("11 22 33 44 55");
    PacketReader in = PacketReader.wrap(data, 1, 3);
    assertThat(in.remaining()).isEqualTo(3);
    assertThat(in.readUnsignedByte()).isEqualTo(0x22);
    assertThat(in.readUnsignedByte()).isEqualTo(0x33);
    assertThat(in.readUnsignedByte()).isEqualTo(0x44);
    assertFails(in::readBoolean, Reason.UNEXPECTED_END);
  }

  @Test
  void aRangeOutsideTheArrayIsRefused() {
    // spec: API contract of wrap
    byte[] data = new byte[4];
    assertThrows(IndexOutOfBoundsException.class, () -> PacketReader.wrap(data, -1, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> PacketReader.wrap(data, 0, 5));
    assertThrows(IndexOutOfBoundsException.class, () -> PacketReader.wrap(data, 3, 2));
    assertThrows(IndexOutOfBoundsException.class, () -> PacketReader.wrap(data, 1, -1));
    assertThrows(IndexOutOfBoundsException.class, () -> PacketReader.wrap(data, 5, 0));
  }

  @Test
  void aWholeArrayAndAnEmptyRangeAreAccepted() throws ProtocolException {
    // spec: API contract of wrap
    assertThat(PacketReader.wrap(new byte[4]).remaining()).isEqualTo(4);
    assertThat(PacketReader.wrap(new byte[4], 4, 0).remaining()).isZero();
    assertThat(PacketReader.wrap(new byte[4], 0, 0).remaining()).isZero();
  }

  @Test
  void readRemainingBytesTakesEverythingLeft() throws ProtocolException {
    // spec: R14 (array without a prefix), R87
    PacketReader in = Buffers.reader("aa bb cc dd");
    in.readUnsignedByte();
    ByteBlob rest = in.readRemainingBytes(16);
    assertThat(rest).isEqualTo(ByteBlob.of(Hex.bytes("bb cc dd")));
    assertThat(in.remaining()).isZero();
    assertThat(Buffers.reader("").readRemainingBytes(0).length()).isZero();
  }

  @Test
  void readRemainingBytesRefusesMoreThanTheMaximum() throws ProtocolException {
    // spec: R14, API contract of readRemainingBytes
    assertThat(Buffers.reader("01 02 03").readRemainingBytes(3).length()).isEqualTo(3);
    assertFails(() -> Buffers.reader("01 02 03").readRemainingBytes(2), Reason.BYTES_TOO_LONG);
  }
}
