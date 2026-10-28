package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Failures.assertFails;
import static dev.shoko.minecraft.core.nbt.Failures.failure;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.binary.BinaryVectors.Vector;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Invalid binary input (R12, R24, R25, R33): table E, the rejected probes of PB1 and truncations.
 * Every case checks the reason and the offset documented by {@code NbtException.Reason}.
 */
class NbtReaderErrorsTest {

  private static void assertReadFails(String hex, Reason reason, long offset) {
    assertReadFails(Hex.bytes(hex), reason, offset);
  }

  private static void assertReadFails(byte[] data, Reason reason, long offset) {
    assertFails(() -> NbtReader.of(NbtCompression.NONE).readNamed(data), reason, offset);
  }

  @Test
  void truncatedIntFails() {
    // spec: core-nbt R33, vector E01 (6 bytes available)
    assertReadFails("03 00 00 01 02 03", Reason.UNEXPECTED_END, 6);
  }

  @Test
  void unknownRootTypeFails() {
    // spec: core-nbt R33, vector E02
    assertReadFails("0d 00 00", Reason.UNKNOWN_TYPE, 0);
    assertReadFails("ff 00 00", Reason.UNKNOWN_TYPE, 0);
  }

  @Test
  void unknownTypeInsideContainerFails() {
    // spec: core-nbt R33 (as a compound entry or a list element type), probe p08
    assertReadFails("0a 00 00 0d 00 01 71 00", Reason.UNKNOWN_TYPE, 3);
    assertReadFails("09 00 00 0d 00 00 00 00", Reason.UNKNOWN_TYPE, 3);
    assertReadFails("09 00 00 80 00 00 00 01", Reason.UNKNOWN_TYPE, 3);
    assertReadFails(
        ProbeFiles.probe("p08_extra_unknown_type_13"), Reason.UNKNOWN_TYPE, ProbeFiles.ROOT_END);
  }

  @Test
  void nonEmptyListOfEndFails() {
    // spec: core-nbt R24, vector E03, probe p04, provisional
    assertReadFails("09 00 00 00 00 00 00 01", Reason.END_LIST_NOT_EMPTY, 3);
    assertReadFails("0a 00 00 09 00 01 6c 00 7f ff ff ff 00", Reason.END_LIST_NOT_EMPTY, 7);
    assertReadFails(
        ProbeFiles.probe("p04_entities_end_len_1"),
        Reason.END_LIST_NOT_EMPTY,
        ProbeFiles.ENTITIES_ELEMENT_TYPE);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "E04 Byte_Array length -1, 07 00 00 ff ff ff ff",
    "Int_Array length -1, 0b 00 00 ff ff ff ff",
    "Long_Array length -2^31, 0c 00 00 80 00 00 00",
    "Int_Array length -1 with elements, 0b 00 00 ff ff ff ff 00 00 00 01"
  })
  void negativeArrayLengthFails(String name, String hex) {
    // spec: core-nbt R25, vector E04, provisional
    assertReadFails(hex, Reason.NEGATIVE_ARRAY_LENGTH, 3);
  }

  @Test
  void negativeArrayLengthInFileFails() {
    // spec: core-nbt R2, R25, probe p13: the length starts after "07 00 01 71"
    assertReadFails(
        ProbeFiles.probe("p13_extra_bytearray_len_minus1"),
        Reason.NEGATIVE_ARRAY_LENGTH,
        ProbeFiles.ROOT_END + 4);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "E06 raw 00 byte, 08 00 00 00 01 00, 5",
    "E07 standard 4-byte UTF-8, 08 00 00 00 04 f0 9f 98 80, 5",
    "lone continuation byte, 08 00 00 00 01 80, 5",
    "2-byte lead with ASCII next, 08 00 00 00 02 c3 41, 5",
    "2-byte sequence cut by length, 08 00 00 00 01 c3, 5",
    "3-byte sequence cut by length, 08 00 00 00 02 e2 82, 5",
    "invalid byte after ASCII, 08 00 00 00 02 41 ff, 6",
    "raw 00 after ASCII, 08 00 00 00 03 41 42 00, 7",
    "lead byte f8, 08 00 00 00 01 f8, 5"
  })
  void invalidModifiedUtf8StringFails(String name, String hex, long offset) {
    // spec: core-nbt R11, R12, vectors E06 and E07, provisional; offset: first byte of the sequence
    assertReadFails(hex, Reason.INVALID_STRING, offset);
  }

  @Test
  void invalidKeyOrRootNameFails() {
    // spec: core-nbt R5 (names are coded as strings), R12
    assertReadFails("0a 00 00 01 00 01 00 05 00", Reason.INVALID_STRING, 6);
    assertReadFails("0a 00 01 00 00", Reason.INVALID_STRING, 3);
  }

  @Test
  void invalidStringInFileFails() {
    // spec: core-nbt R12, probes p09 and p10; the value starts after "08 00 01 71 00 0n"
    assertReadFails(
        ProbeFiles.probe("p09_extra_string_raw_nul"),
        Reason.INVALID_STRING,
        ProbeFiles.ROOT_END + 6);
    assertReadFails(
        ProbeFiles.probe("p10_extra_string_utf8_4byte"),
        Reason.INVALID_STRING,
        ProbeFiles.ROOT_END + 6);
  }

  @Test
  void compoundWithoutEndFails() {
    // spec: core-nbt R33, vector E08 (8 bytes available)
    assertReadFails("0a 00 00 01 00 01 61 01", Reason.UNEXPECTED_END, 8);
  }

  @Test
  void truncatedFileFails() {
    // spec: core-nbt R33, probe p07 (147 bytes available)
    assertReadFails(ProbeFiles.probe("p07_truncated"), Reason.UNEXPECTED_END, 147);
  }

  @Test
  void emptyInputFails() {
    // spec: core-nbt R33 (premature end: not even the root type)
    assertReadFails(new byte[0], Reason.UNEXPECTED_END, 0);
    assertFails(
        () -> NbtReader.of(NbtCompression.NONE).readUnnamed(new byte[0]), Reason.UNEXPECTED_END, 0);
  }

  static List<Vector> namedVectors() {
    return BinaryVectors.named();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("namedVectors")
  void everyTruncationOfVectorFailsAtEndOfData(Vector vector) {
    // spec: core-nbt R33 (premature end of stream, for every type); offset = bytes available
    byte[] full = vector.bytes();
    for (int length = 0; length < full.length; length++) {
      byte[] prefix = Arrays.copyOf(full, length);
      assertReadFails(prefix, Reason.UNEXPECTED_END, length);
    }
  }

  @Test
  void truncatedStreamFailsLikeTruncatedArray() {
    // spec: core-nbt R33
    var in = new ByteArrayInputStream(Hex.bytes("0b 00 00 00 00 00 02 00 00 00 01 00 00"));
    assertFails(() -> NbtReader.of(NbtCompression.NONE).readNamed(in), Reason.UNEXPECTED_END, 13);
  }

  @Test
  void truncatedUnnamedRootFails() {
    // spec: core-nbt R20, R33
    assertFails(
        () -> NbtReader.of(NbtCompression.NONE).readUnnamed(Hex.bytes("0a")),
        Reason.UNEXPECTED_END,
        1);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "Byte_Array of 2^31-1 declared, 07 00 00 7f ff ff ff, 7",
    "Int_Array of 2^31-1 declared, 0b 00 00 7f ff ff ff 00 00 00 01, 11",
    "Long_Array of 2^31-1 declared, 0c 00 00 7f ff ff ff, 7",
    "List of Long of 2^31-1 declared, 09 00 00 04 7f ff ff ff, 8",
    "String of 65535 declared, 08 00 00 ff ff 61, 6"
  })
  void hugeDeclaredLengthWithoutDataFailsAtEndOfData(String name, String hex, long offset) {
    // spec: core-nbt R33 (premature end), "Entrees / sorties" (length bounded by memory only)
    assertReadFails(hex, Reason.UNEXPECTED_END, offset);
  }

  @Test
  void failureGivesNoPartialResult() {
    // spec: core-nbt R33 (no partial result): the failure is thrown, nothing is returned
    var thrown =
        failure(
            () -> NbtReader.of(NbtCompression.NONE).readNamed(ProbeFiles.probe("p07_truncated")));
    assertThat(thrown.reason()).isEqualTo(Reason.UNEXPECTED_END);
  }
}
