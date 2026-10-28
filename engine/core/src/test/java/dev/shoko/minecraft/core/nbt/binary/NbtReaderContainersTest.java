package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Failures.assertFailsWithoutOffset;
import static dev.shoko.minecraft.core.nbt.Tags.root;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Container checks on reading (R61, R62), vectors C08 to C12 and the C02 to C07 mutations applied
 * to gzip members built in the test (the exact C01 to C07 need the reference file: see {@code
 * ReferenceStructuresOracleTest}). A refused container gives {@code CORRUPT_CONTAINER} and no
 * value.
 */
class NbtReaderContainersTest {

  private static final String R16_HEADER = "1f 8b 08 00 00 00 00 00 00 ff";

  static int dataVersion(NbtCompression compression, byte[] data) throws NbtException {
    var tag = (CompoundTag) NbtReader.of(compression).readNamed(data).orElseThrow().tag();
    return tag.getInt("DataVersion");
  }

  static void assertCorrupt(NbtCompression compression, byte[] data) {
    assertFailsWithoutOffset(
        () -> NbtReader.of(compression).readNamed(data), Reason.CORRUPT_CONTAINER);
  }

  @Test
  void storedGzipIsAccepted() throws NbtException {
    // spec: core-nbt R61, C08
    assertThat(dataVersion(NbtCompression.GZIP, Containers.gzipStored(ProbeFiles.f1())))
        .isEqualTo(5023);
  }

  @Test
  void payloadByteChangedUnderOriginalCrcIsRefused() {
    // spec: core-nbt R61, C09 (without the check, DataVersion would read 5024)
    assertCorrupt(NbtCompression.GZIP, ProbeFiles.c09());
    assertFailsWithoutOffset(
        () ->
            NbtReader.of(NbtCompression.GZIP).readNamed(new ByteArrayInputStream(ProbeFiles.c09())),
        Reason.CORRUPT_CONTAINER);
  }

  @Test
  void storedZlibIsAccepted() throws NbtException {
    // spec: core-nbt R18, R61, C10
    assertThat(dataVersion(NbtCompression.ZLIB, Containers.zlibStored(ProbeFiles.f1())))
        .isEqualTo(5023);
  }

  @Test
  void wrongOrMissingAdlerIsRefused() {
    // spec: core-nbt R18, R61, C11 (Adler-32 inverted) and C12 (Adler-32 removed)
    byte[] zlib = Containers.zlibStored(ProbeFiles.f1());
    assertCorrupt(NbtCompression.ZLIB, Containers.invert(zlib, zlib.length - 4, zlib.length));
    assertCorrupt(NbtCompression.ZLIB, Arrays.copyOf(zlib, zlib.length - 4));
  }

  static Stream<Named<Supplier<byte[]>>> members() {
    return Stream.of(
        Named.of("stored member", () -> Containers.gzipStored(ProbeFiles.f1())),
        Named.of("java.util.zip member", () -> Containers.gzip(ProbeFiles.f1())));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("members")
  void wrongCrcIsRefused(Supplier<byte[]> member) {
    // spec: core-nbt R61, as C02 (CRC-32 inverted, bytes -8 to -5)
    byte[] gzip = member.get();
    assertCorrupt(NbtCompression.GZIP, Containers.invert(gzip, gzip.length - 8, gzip.length - 4));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("members")
  void wrongIsizeIsRefused(Supplier<byte[]> member) {
    // spec: core-nbt R61, as C03 (ISIZE inverted, last 4 bytes)
    byte[] gzip = member.get();
    assertCorrupt(NbtCompression.GZIP, Containers.invert(gzip, gzip.length - 4, gzip.length));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("members")
  void missingOrTruncatedTrailerIsRefused(Supplier<byte[]> member) {
    // spec: core-nbt R61, as C04 (trailer removed) and C05 (4 bytes of trailer removed)
    byte[] gzip = member.get();
    assertCorrupt(NbtCompression.GZIP, Arrays.copyOf(gzip, gzip.length - 8));
    assertCorrupt(NbtCompression.GZIP, Arrays.copyOf(gzip, gzip.length - 4));
  }

  @Test
  void unterminatedDeflateStreamIsRefused() {
    // spec: core-nbt R61 (DEFLATE stream not terminated): last stored block without BFINAL
    byte[] gzip = Containers.gzipStored(ProbeFiles.f1());
    gzip[10] = 0;
    assertCorrupt(NbtCompression.GZIP, Arrays.copyOf(gzip, gzip.length - 8));
  }

  @ParameterizedTest(name = "FLG bit {0}")
  @ValueSource(ints = {0x20, 0x40, 0x80})
  void reservedFlagBitIsIgnored(int bit) throws NbtException {
    // spec: core-nbt R62, as C06, provisional: reserved FLG bits accepted and ignored
    byte[] gzip = Containers.gzipStored(ProbeFiles.f1());
    gzip[3] = (byte) (gzip[3] | bit);
    assertThat(dataVersion(NbtCompression.GZIP, gzip)).isEqualTo(5023);
  }

  @Test
  void optionalHeaderFieldsAreSkipped() throws NbtException {
    // spec: core-nbt R62: FTEXT, FEXTRA, FNAME, FCOMMENT and FHCRC accepted, fields skipped, the
    // header CRC (here 00 00) is not checked
    byte[] header =
        Hex.concat(
            Hex.bytes("1f 8b 08 1f 00 00 00 00 00 ff"),
            Hex.bytes("03 00 61 62 63"),
            Hex.bytes("66 31 2e 6e 62 74 00"),
            Hex.bytes("68 69 00"),
            Hex.bytes("00 00"));
    byte[] gzip = Containers.gzipStored(header, ProbeFiles.f1());
    assertThat(dataVersion(NbtCompression.GZIP, gzip)).isEqualTo(5023);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("members")
  void bytesAfterMemberAreIgnored(Supplier<byte[]> member) throws NbtException {
    // spec: core-nbt R61, as C07 (4 zero bytes after the member), provisional; also other bytes
    byte[] gzip = member.get();
    assertThat(dataVersion(NbtCompression.GZIP, Hex.concat(gzip, new byte[4]))).isEqualTo(5023);
    assertThat(dataVersion(NbtCompression.GZIP, Hex.concat(gzip, Hex.bytes("ff 00 13 37"))))
        .isEqualTo(5023);
  }

  @Test
  void onlyFirstGzipMemberIsRead() throws NbtException {
    // spec: core-nbt R61 (a single gzip member is read): a second member holding other bytes is
    // not part of the payload
    byte[] first = Containers.gzipStored(ProbeFiles.f1());
    byte[] second = Containers.gzipStored(Hex.bytes("0a 00 00 03 00 01 61 00 00 00 01 00"));
    byte[] both = Hex.concat(first, second);
    assertThat(NbtReader.of(NbtCompression.GZIP).readNamed(both))
        .contains(root(ProbeFiles.f1Tree()));
  }

  @Test
  void bytesAfterZlibStreamAreIgnored() throws NbtException {
    // spec: core-nbt R61 (bytes after the end of the container ignored), provisional
    byte[] zlib = Containers.zlibStored(ProbeFiles.f1());
    assertThat(dataVersion(NbtCompression.ZLIB, Hex.concat(zlib, new byte[4]))).isEqualTo(5023);
  }

  @Test
  void payloadAfterRootIsConsumedAndTrailerChecked() throws NbtException {
    // spec: core-nbt R34 (payload bytes after the root ignored) and R61 (container consumed to
    // its end): a correct trailer is accepted, a wrong one refused although the root is complete
    byte[] payload = Hex.bytes("0a 00 00 00 ff ff ff ff");
    byte[] gzip = Containers.gzipStored(payload);
    assertThat(NbtReader.of(NbtCompression.GZIP).readNamed(gzip))
        .contains(root(CompoundTag.empty()));
    assertCorrupt(NbtCompression.GZIP, Containers.invert(gzip, gzip.length - 8, gzip.length - 4));
    byte[] zlib = Containers.zlibStored(payload);
    assertCorrupt(NbtCompression.ZLIB, Containers.invert(zlib, zlib.length - 4, zlib.length));
  }

  @Test
  void unnamedRootIsCheckedToo() {
    // spec: core-nbt R20, R61
    byte[] gzip = Containers.gzipStored(Hex.bytes("0a 00"));
    assertFailsWithoutOffset(
        () ->
            NbtReader.of(NbtCompression.GZIP)
                .readUnnamed(Containers.invert(gzip, gzip.length - 8, gzip.length - 4)),
        Reason.CORRUPT_CONTAINER);
  }

  @Test
  void storedMemberUsesReferenceHeader() {
    // fixture check: C08 starts with the header of R16
    assertThat(Hex.format(Arrays.copyOf(Containers.gzipStored(ProbeFiles.f1()), 10)))
        .isEqualTo(R16_HEADER);
  }
}
