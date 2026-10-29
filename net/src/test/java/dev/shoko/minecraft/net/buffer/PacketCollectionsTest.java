package dev.shoko.minecraft.net.buffer;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.testing.Buffers;
import dev.shoko.minecraft.net.testing.Hex;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R14 (arrays, optionals, byte arrays), R16 (BitSet, vector K12)
class PacketCollectionsTest {

  private static BitSet bits(int... indexes) {
    BitSet set = new BitSet();
    for (int index : indexes) {
      set.set(index);
    }
    return set;
  }

  @Test
  void anEmptyPrefixedArrayIsASingleZeroByte() throws ProtocolException {
    // spec: R14
    byte[] written =
        Buffers.write(
            out -> out.writePrefixedArray(List.<Integer>of(), 8, PacketWriter::writeVarInt));
    assertThat(Hex.format(written)).isEqualTo("00");
    assertThat(
            Buffers.<List<Integer>>readAll(
                written, in -> in.readPrefixedArray(8, PacketReader::readVarInt)))
        .isEmpty();
  }

  @Test
  void aPrefixedArrayIsACountThenTheElements() throws ProtocolException {
    // spec: R14
    List<Integer> values = List.of(1, 300, -1);
    byte[] written =
        Buffers.write(out -> out.writePrefixedArray(values, 8, PacketWriter::writeVarInt));
    assertThat(Hex.format(written)).isEqualTo("03 01 ac 02 ff ff ff ff 0f");
    assertThat(
            Buffers.<List<Integer>>readAll(
                written, in -> in.readPrefixedArray(8, PacketReader::readVarInt)))
        .containsExactlyElementsOf(values);
  }

  @Test
  void aPrefixedArrayOfStringsKeepsWireOrder() throws ProtocolException {
    // spec: R14
    List<String> values = List.of("b", "a", "");
    byte[] written =
        Buffers.write(out -> out.writePrefixedArray(values, 3, (w, v) -> w.writeString(v, 8)));
    assertThat(Hex.format(written)).isEqualTo("03 01 62 01 61 00");
    assertThat(
            Buffers.<List<String>>readAll(
                written, in -> in.readPrefixedArray(3, r -> r.readString(8))))
        .containsExactly("b", "a", "");
  }

  @Test
  void theArrayReadIsUnmodifiable() throws ProtocolException {
    // spec: API contract of readPrefixedArray (unmodifiable)
    List<Integer> read =
        Buffers.readAll("02 01 02", in -> in.readPrefixedArray(8, PacketReader::readVarInt));
    assertThatThrownBy(() -> read.add(3)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void provisionalAnArrayAtMaxLengthIsAcceptedAndOneMoreIsTooLong() throws ProtocolException {
    // spec: R14 (a case at n and a case at n + 1), e.g. PrefixedArray(20)
    List<Integer> atLimit = List.of(1, 2, 3);
    List<Integer> over = List.of(1, 2, 3, 4);
    byte[] written =
        Buffers.write(out -> out.writePrefixedArray(atLimit, 3, PacketWriter::writeVarInt));
    assertThat(
            Buffers.<List<Integer>>readAll(
                written, in -> in.readPrefixedArray(3, PacketReader::readVarInt)))
        .hasSize(3);
    assertFails(
        () -> Buffers.write(out -> out.writePrefixedArray(over, 3, PacketWriter::writeVarInt)),
        Reason.ARRAY_TOO_LONG);
    assertFails(
        () ->
            Buffers.readAll(
                "04 01 02 03 04", in -> in.readPrefixedArray(3, PacketReader::readVarInt)),
        Reason.ARRAY_TOO_LONG);
  }

  @Test
  void provisionalAnArrayWithMoreElementsThanBytesLeftIsAnUnexpectedEnd() {
    // spec: R90, API contract of readPrefixedArray (an element takes at least one byte)
    assertFails(
        () -> Buffers.reader("05 01 02").readPrefixedArray(100, PacketReader::readVarInt),
        Reason.UNEXPECTED_END);
    assertFails(
        () ->
            Buffers.reader("ff ff ff ff 07")
                .readPrefixedArray(Integer.MAX_VALUE, PacketReader::readByte),
        Reason.UNEXPECTED_END);
  }

  @Test
  void provisionalANegativeCountIsRefused() {
    // spec: R14, API contract (NEGATIVE_LENGTH)
    assertFails(
        () -> Buffers.reader("ff ff ff ff 0f").readPrefixedArray(8, PacketReader::readVarInt),
        Reason.NEGATIVE_LENGTH);
  }

  @Test
  void anElementFailureIsPassedOn() {
    // spec: API contract of ElementReader and ElementWriter
    assertFails(
        () -> Buffers.reader("01 02 61").readPrefixedArray(8, r -> r.readString(8)),
        Reason.UNEXPECTED_END);
    assertFails(
        () ->
            Buffers.write(
                out ->
                    out.writePrefixedArray(List.of("toolong"), 8, (w, v) -> w.writeString(v, 3))),
        Reason.STRING_TOO_LONG);
  }

  @Test
  void anOptionalIsABooleanThenTheValueWhenTrue() throws ProtocolException {
    // spec: R14
    byte[] absent = Buffers.write(PacketWriter::writeAbsent);
    assertThat(Hex.format(absent)).isEqualTo("00");
    assertThat(
            Buffers.<Optional<Integer>>readAll(
                absent, in -> in.readPrefixedOptional(PacketReader::readVarInt)))
        .isEqualTo(Optional.empty());
    byte[] present = Buffers.write(out -> out.writePresent(300, PacketWriter::writeVarInt));
    assertThat(Hex.format(present)).isEqualTo("01 ac 02");
    assertThat(
            Buffers.<Optional<Integer>>readAll(
                present, in -> in.readPrefixedOptional(PacketReader::readVarInt)))
        .isEqualTo(Optional.of(300));
  }

  @Test
  void provisionalAnOptionalWithANonZeroBooleanIsPresent() throws ProtocolException {
    // spec: R5 (any non-zero Boolean is true), R14
    assertThat(
            Buffers.<Optional<Byte>>readAll(
                "02 07", in -> in.readPrefixedOptional(PacketReader::readByte)))
        .isEqualTo(Optional.of((byte) 7));
  }

  @Test
  void aMissingOptionalValueIsAnUnexpectedEnd() {
    // spec: R90
    assertFails(
        () -> Buffers.reader("01").readPrefixedOptional(PacketReader::readVarInt),
        Reason.UNEXPECTED_END);
    assertFails(
        () -> Buffers.reader("").readPrefixedOptional(PacketReader::readVarInt),
        Reason.UNEXPECTED_END);
  }

  @Test
  void prefixedBytesAreALengthThenTheBytes() throws ProtocolException {
    // spec: R14, vector CH04 shape (public key) and F05 style
    ByteBlob blob = ByteBlob.of(Hex.bytes("de ad be ef"));
    byte[] written = Buffers.write(out -> out.writePrefixedBytes(blob, 16));
    assertThat(Hex.format(written)).isEqualTo("04 de ad be ef");
    assertThat(Buffers.<ByteBlob>readAll(written, in -> in.readPrefixedBytes(16))).isEqualTo(blob);
    assertThat(
            Hex.format(Buffers.write(out -> out.writePrefixedBytes(ByteBlob.of(new byte[0]), 0))))
        .isEqualTo("00");
  }

  @Test
  void provisionalPrefixedBytesAtMaxLengthAreAcceptedAndOneMoreIsTooLong()
      throws ProtocolException {
    // spec: R14 (ByteArray(256), cookies (5120): a case at n and a case at n + 1)
    ByteBlob atLimit = ByteBlob.of(new byte[256]);
    byte[] written = Buffers.write(out -> out.writePrefixedBytes(atLimit, 256));
    assertThat(Buffers.readAll(written, in -> in.readPrefixedBytes(256)).length()).isEqualTo(256);
    ByteBlob over = ByteBlob.of(new byte[257]);
    assertFails(
        () -> Buffers.write(out -> out.writePrefixedBytes(over, 256)), Reason.BYTES_TOO_LONG);
    byte[] overBytes = Hex.concat(Hex.bytes("81 02"), new byte[257]);
    assertFails(
        () -> Buffers.readAll(overBytes, in -> in.readPrefixedBytes(256)), Reason.BYTES_TOO_LONG);
  }

  @Test
  void provisionalPrefixedBytesAnnouncingMoreThanTheBytesLeftAreAnUnexpectedEnd() {
    // spec: R90 (compared with the bytes left before any allocation)
    assertFails(() -> Buffers.reader("05 01 02").readPrefixedBytes(100), Reason.UNEXPECTED_END);
    assertFails(
        () -> Buffers.reader("ff ff ff ff 07").readPrefixedBytes(Integer.MAX_VALUE),
        Reason.UNEXPECTED_END);
  }

  @Test
  void provisionalPrefixedBytesWithANegativeLengthAreRefused() {
    // spec: R14 (NEGATIVE_LENGTH)
    assertFails(
        () -> Buffers.reader("ff ff ff ff 0f").readPrefixedBytes(100), Reason.NEGATIVE_LENGTH);
  }

  @Test
  void aBitSetIsACountOfLongsThenTheLongs() throws ProtocolException {
    // spec: R16, vector K12 (mask with bits 0 and 25 set)
    BitSet mask = bits(0, 25);
    byte[] written = Buffers.write(out -> out.writeBitSet(mask));
    assertThat(Hex.format(written)).isEqualTo("01 00 00 00 00 02 00 00 01");
    assertThat(Buffers.readAll(written, PacketReader::readBitSet)).isEqualTo(mask);
  }

  @Test
  void anEmptyBitSetRoundTrips() throws ProtocolException {
    // spec: R16
    byte[] written = Buffers.write(out -> out.writeBitSet(new BitSet()));
    assertThat(Buffers.readAll(written, PacketReader::readBitSet)).isEqualTo(new BitSet());
    assertThat(Buffers.readAll("00", PacketReader::readBitSet)).isEqualTo(new BitSet());
  }

  @Test
  void bitIIsBitIModuloSixtyFourOfLongIDividedBySixtyFour() throws ProtocolException {
    // spec: R16 (bit i is (Long[i / 64] >> (i % 64)) & 1)
    BitSet read =
        Buffers.readAll(
            "02 80 00 00 00 00 00 00 00 00 00 00 00 00 00 00 05", PacketReader::readBitSet);
    assertThat(read).isEqualTo(bits(63, 64, 66));
    BitSet wide = bits(0, 63, 64, 127, 128);
    byte[] written = Buffers.write(out -> out.writeBitSet(wide));
    assertThat(Buffers.readAll(written, PacketReader::readBitSet)).isEqualTo(wide);
    assertThat(Hex.format(written)).startsWith("03 ");
  }

  @Test
  void provisionalABitSetCountLargerThanTheBytesLeftIsRefused() {
    // spec: R90, API contract of readBitSet
    assertFails(
        () -> Buffers.reader("02 00 00 00 00 00 00 00 01").readBitSet(), Reason.UNEXPECTED_END);
    assertFails(() -> Buffers.reader("ff ff ff ff 07").readBitSet(), Reason.UNEXPECTED_END);
    assertFails(() -> Buffers.reader("ff ff ff ff 0f").readBitSet(), Reason.NEGATIVE_LENGTH);
  }

  @Test
  void aFixedBitSetIsCeilBitsOverEightBytesWithBitIInByteIOverEight() throws ProtocolException {
    // spec: R16 (BitSet fixe(n): ceil(n / 8) bytes, bit i = (byte[i / 8] >> (i % 8)) & 1)
    BitSet set = bits(0, 9, 19);
    byte[] written = Buffers.write(out -> out.writeFixedBitSet(set, 20));
    assertThat(Hex.format(written)).isEqualTo("01 02 08");
    assertThat(Buffers.<BitSet>readAll(written, in -> in.readFixedBitSet(20))).isEqualTo(set);
    assertThat(Buffers.write(out -> out.writeFixedBitSet(new BitSet(), 0))).isEmpty();
    assertThat(Buffers.write(out -> out.writeFixedBitSet(new BitSet(), 8))).hasSize(1);
    assertThat(Buffers.write(out -> out.writeFixedBitSet(new BitSet(), 9))).hasSize(2);
    assertThat(Buffers.<BitSet>readAll(new byte[0], in -> in.readFixedBitSet(0)))
        .isEqualTo(new BitSet());
  }

  @Test
  void aFixedBitSetIsNotAVariableBitSet() throws ProtocolException {
    // spec: R16 ("Les deux ne sont pas equivalents")
    BitSet set = bits(0, 25);
    byte[] fixed = Buffers.write(out -> out.writeFixedBitSet(set, 26));
    assertThat(Hex.format(fixed)).isEqualTo("01 00 00 02");
    assertThat(fixed).isNotEqualTo(Buffers.write(out -> out.writeBitSet(set)));
  }

  @Test
  void bitsBeyondTheDeclaredSizeInTheLastByteAreIgnoredOnRead() throws ProtocolException {
    // spec: API contract of readFixedBitSet
    assertThat(Buffers.<BitSet>readAll("ff ff ff", in -> in.readFixedBitSet(20)))
        .isEqualTo(bits(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19));
  }

  @Test
  void provisionalABitBeyondTheFixedSizeIsRefusedOnWrite() {
    // spec: API contract of writeFixedBitSet (VALUE_OUT_OF_RANGE)
    assertFails(
        () -> Buffers.write(out -> out.writeFixedBitSet(bits(20), 20)), Reason.VALUE_OUT_OF_RANGE);
    assertFails(
        () -> Buffers.write(out -> out.writeFixedBitSet(bits(0), 0)), Reason.VALUE_OUT_OF_RANGE);
  }

  @Test
  void aFixedBitSetCutShortIsAnUnexpectedEnd() {
    // spec: R90
    assertFails(() -> Buffers.reader("01 02").readFixedBitSet(20), Reason.UNEXPECTED_END);
  }
}
