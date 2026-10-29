package dev.shoko.minecraft.net.buffer;

import static dev.shoko.minecraft.net.testing.Failures.assertFails;
import static dev.shoko.minecraft.net.testing.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import dev.shoko.minecraft.net.testing.Buffers;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Wire;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R9 (String(n)), R10 (Identifier), R11 (UUID)
class PacketStringTest {

  private static final String E_ACUTE = String.valueOf((char) 0xe9);
  private static final String EMOJI = new String(Character.toChars(0x1F600));

  @Test
  void aStringIsAByteLengthThenUtf8() throws ProtocolException {
    // spec: R9, vector F05 (the name Notch) and F01 (localhost)
    assertThat(Hex.format(Buffers.write(out -> out.writeString("Notch", 16))))
        .isEqualTo("05 4e 6f 74 63 68");
    assertThat(Hex.format(Buffers.write(out -> out.writeString("", 16)))).isEqualTo("00");
    assertThat(Buffers.<String>readAll("09 6c 6f 63 61 6c 68 6f 73 74", in -> in.readString(255)))
        .isEqualTo("localhost");
    assertThat(Buffers.<String>readAll("00", in -> in.readString(0))).isEmpty();
  }

  @Test
  void theLengthIsInBytesAndTheLimitInUtf16Units() throws ProtocolException {
    // spec: R9 (length in bytes, maximum counted in UTF-16 code units)
    String accent = "caf" + E_ACUTE;
    byte[] written = Buffers.write(out -> out.writeString(accent, 4));
    assertThat(Hex.format(written)).isEqualTo("05 63 61 66 c3 a9");
    assertThat(Buffers.<String>readAll(written, in -> in.readString(4))).isEqualTo(accent);
    String chinese = new String(new char[] {0x4e2d, 0x6587});
    assertThat(Hex.format(Buffers.write(out -> out.writeString(chinese, 2))))
        .isEqualTo("06 e4 b8 ad e6 96 87");
  }

  @Test
  void aCodePointAboveFFFFCountsAsTwoUnits() throws ProtocolException {
    // spec: R9
    byte[] written = Buffers.write(out -> out.writeString(EMOJI, 2));
    assertThat(Hex.format(written)).isEqualTo("04 f0 9f 98 80");
    assertThat(Buffers.<String>readAll(written, in -> in.readString(2))).isEqualTo(EMOJI);
    assertFails(() -> Buffers.write(out -> out.writeString(EMOJI, 1)), Reason.STRING_TOO_LONG);
    assertFails(() -> Buffers.readAll(written, in -> in.readString(1)), Reason.STRING_TOO_LONG);
  }

  @Test
  void provisionalAStringOfMaxCharsIsAcceptedAndOneMoreIsTooLong() throws ProtocolException {
    // spec: R9, R14 (a case at n and a case at n + 1), vector I10 (17 characters)
    String atLimit = "A".repeat(16);
    String over = "A".repeat(17);
    byte[] written = Buffers.write(out -> out.writeString(atLimit, 16));
    assertThat(Buffers.<String>readAll(written, in -> in.readString(16))).isEqualTo(atLimit);
    assertFails(() -> Buffers.write(out -> out.writeString(over, 16)), Reason.STRING_TOO_LONG);
    byte[] overBytes = Wire.string(over);
    assertFails(() -> Buffers.readAll(overBytes, in -> in.readString(16)), Reason.STRING_TOO_LONG);
  }

  @Test
  void aStringOfTheLargestMaximumRoundTrips() throws ProtocolException {
    // spec: R9 (n at most 32767)
    String longest = "x".repeat(32767);
    byte[] written = Buffers.write(out -> out.writeString(longest, 32767));
    assertThat(Buffers.<String>readAll(written, in -> in.readString(32767))).isEqualTo(longest);
    assertFails(
        () -> Buffers.write(out -> out.writeString(longest + "x", 32767)), Reason.STRING_TOO_LONG);
  }

  @Test
  void provisionalAnEncodedSizeAbove3nPlus3IsRefusedBeforeTheBytesAreRead() {
    // spec: R9 (3n + 3 bytes), API contract of readString: checked before the bytes are read
    // n = 4: 15 bytes may be announced, 16 may not, and no byte follows the announcement.
    assertFails(() -> Buffers.reader("10").readString(4), Reason.STRING_TOO_LONG);
    assertFails(() -> Buffers.reader("ff ff ff ff 07").readString(4), Reason.STRING_TOO_LONG);
    assertFails(() -> Buffers.reader("0f").readString(4), Reason.UNEXPECTED_END);
  }

  @Test
  void provisionalAByteLengthWithinTheEncodedLimitButTooManyUnitsIsTooLong() {
    // spec: R9 (the length in UTF-16 code units must not exceed n)
    assertFails(() -> Buffers.reader("03 61 62 63").readString(2), Reason.STRING_TOO_LONG);
  }

  @Test
  void provisionalANegativeLengthIsRefused() {
    // spec: R9, R14 (API contract: NEGATIVE_LENGTH)
    assertFails(() -> Buffers.reader("ff ff ff ff 0f").readString(16), Reason.NEGATIVE_LENGTH);
    assertFails(() -> Buffers.reader("80 80 80 80 08").readString(16), Reason.NEGATIVE_LENGTH);
  }

  @Test
  void provisionalAStringCutShortIsAnUnexpectedEnd() {
    // spec: R90
    assertFails(() -> Buffers.reader("05 4e 6f").readString(16), Reason.UNEXPECTED_END);
    assertFails(() -> Buffers.reader("").readString(16), Reason.UNEXPECTED_END);
  }

  @Test
  void provisionalInvalidUtf8IsRefused() {
    // spec: R9 (invalid UTF-8 is a protocol error), probe PN7 (input ff)
    assertFails(() -> Buffers.reader("01 ff").readString(16), Reason.INVALID_UTF8);
    assertFails(() -> Buffers.reader("02 c0 80").readString(16), Reason.INVALID_UTF8);
    assertFails(() -> Buffers.reader("02 e2 82").readString(16), Reason.INVALID_UTF8);
    assertFails(() -> Buffers.reader("01 80").readString(16), Reason.INVALID_UTF8);
    assertFails(() -> Buffers.reader("03 ed a0 80").readString(16), Reason.INVALID_UTF8);
  }

  @Test
  void aStringIsReadFromWhereTheReaderIs() throws ProtocolException {
    // spec: R9
    PacketReader in = Buffers.reader("02 68 69 01 7a 99");
    assertThat(in.readString(8)).isEqualTo("hi");
    assertThat(in.readString(8)).isEqualTo("z");
    assertThat(in.remaining()).isEqualTo(1);
  }

  @Test
  void identifiersAreWrittenInTheirFullForm() throws ProtocolException {
    // spec: R10 (namespace:path), R87 (minecraft:brand)
    Identifier brand = Identifier.of("minecraft", "brand");
    byte[] written = Buffers.write(out -> out.writeIdentifier(brand));
    assertThat(written).isEqualTo(Wire.string("minecraft:brand"));
    assertThat(Buffers.readAll(written, PacketReader::readIdentifier)).isEqualTo(brand);
    Identifier nested = Identifier.of("a.b-c_d0", "p/q/r");
    assertThat(
            Buffers.readAll(
                Buffers.write(out -> out.writeIdentifier(nested)), PacketReader::readIdentifier))
        .isEqualTo(nested);
  }

  @Test
  void provisionalAnInvalidIdentifierIsRefused() {
    // spec: R10, probe PN7 (an Identifier with capitals in custom_payload)
    for (String text :
        new String[] {"Minecraft:brand", "minecraft:Brand", "a:b:c", "a b:c", "x:y*"}) {
      byte[] bytes = Wire.string(text);
      assertFails(
          () -> Buffers.readAll(bytes, PacketReader::readIdentifier), Reason.INVALID_IDENTIFIER);
    }
  }

  @Test
  void anIdentifierStringStillFollowsTheStringLimit() {
    // spec: R10 (String(32767)), R9
    byte[] tooLong = Wire.string("a:" + "b".repeat(32767));
    assertThat(failure(() -> Buffers.readAll(tooLong, PacketReader::readIdentifier)).reason())
        .isEqualTo(Reason.STRING_TOO_LONG);
  }

  @Test
  void uuidsRoundTripAndKeepTheOrderOfTheTwoLongs() throws ProtocolException {
    // spec: R11, vector U01
    UUID notch = UUID.fromString("b50ad385-829d-3141-a216-7e7d7539ba7f");
    assertThat(
            Buffers.readAll(
                "b5 0a d3 85 82 9d 31 41 a2 16 7e 7d 75 39 ba 7f", PacketReader::readUuid))
        .isEqualTo(notch);
    UUID mixed = new UUID(0x0123456789abcdefL, 0xfedcba9876543210L);
    assertThat(Buffers.readAll(Wire.uuid(mixed), PacketReader::readUuid)).isEqualTo(mixed);
    assertThat(Buffers.readAll(new byte[16], PacketReader::readUuid)).isEqualTo(new UUID(0, 0));
  }
}
