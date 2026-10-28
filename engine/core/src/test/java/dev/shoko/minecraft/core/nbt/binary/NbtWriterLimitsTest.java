package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Failures.assertFailsWithoutOffset;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.root;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static dev.shoko.minecraft.core.nbt.Tags.text;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** String length limit (R10, R13) and containers (R16 to R19) on writing. */
class NbtWriterLimitsTest {

  private static final String GZIP_HEADER = "1f 8b 08 00 00 00 00 00 00 ff";

  @Test
  void writesStringOf65535Bytes() throws NbtException {
    // spec: core-nbt R10, "Cas limites" (String of 65535 bytes)
    byte[] written =
        NbtWriter.of(NbtCompression.NONE).writeNamed(root(stringTag("a".repeat(65535))));
    assertThat(written).hasSize(5 + 65535);
    assertThat(Hex.format(Arrays.copyOf(written, 5))).isEqualTo("08 00 00 ff ff");
  }

  @Test
  void writesThreeByteCharactersUpTo65535Bytes() throws NbtException {
    // spec: core-nbt R10, R11: 21845 times U+20AC is exactly 65535 bytes
    byte[] written =
        NbtWriter.of(NbtCompression.NONE).writeNamed(root(stringTag(text(0x20AC).repeat(21845))));
    assertThat(written).hasSize(5 + 65535);
  }

  @Test
  void writesNulCharactersAsTwoBytesEach() throws NbtException {
    // spec: core-nbt R11 (U+0000 is c0 80): 32767 characters give 65534 bytes
    byte[] written =
        NbtWriter.of(NbtCompression.NONE).writeNamed(root(stringTag("\0".repeat(32767))));
    assertThat(Hex.format(Arrays.copyOf(written, 7))).isEqualTo("08 00 00 ff fe c0 80");
  }

  static Stream<Named<Supplier<NamedTag>>> tooLongStrings() {
    return Stream.of(
        Named.of("65536 ASCII bytes", () -> root(stringTag("a".repeat(65536)))),
        Named.of(
            "21846 euro signs, 65538 bytes", () -> root(stringTag(text(0x20AC).repeat(21846)))),
        Named.of("32768 NUL characters, 65536 bytes", () -> root(stringTag("\0".repeat(32768)))),
        Named.of("too long key", () -> root(compound("k".repeat(65536), intTag(1)))),
        Named.of("too long root name", () -> new NamedTag("n".repeat(65536), CompoundTag.empty())),
        Named.of(
            "too long string after valid entries",
            () ->
                root(
                    compound(
                        "first", intTag(1),
                        "nested", list(compound("s", stringTag("a".repeat(70000))))))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("tooLongStrings")
  void stringOver65535BytesFails(Supplier<NamedTag> root) {
    // spec: core-nbt R13, provisional: STRING_TOO_LONG, no offset
    assertFailsWithoutOffset(
        () -> NbtWriter.of(NbtCompression.NONE).writeNamed(root.get()), Reason.STRING_TOO_LONG);
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(NbtCompression.class)
  void failedWriteLeavesNothingInStream(NbtCompression compression) {
    // spec: core-nbt R13, provisional: no partial file, even inside a container
    var out = new ByteArrayOutputStream();
    NamedTag tooLong =
        root(compound("first", intTag(1), "big", stringTag("a".repeat(65536)), "last", intTag(2)));
    assertFailsWithoutOffset(
        () -> NbtWriter.of(compression).writeNamed(tooLong, out), Reason.STRING_TOO_LONG);
    assertThat(out.size()).isZero();
  }

  @Test
  void failedUnnamedWriteLeavesNothingInStream() {
    // spec: core-nbt R13, R20
    var out = new ByteArrayOutputStream();
    assertFailsWithoutOffset(
        () -> NbtWriter.of(NbtCompression.NONE).writeUnnamed(stringTag("a".repeat(65536)), out),
        Reason.STRING_TOO_LONG);
    assertThat(out.size()).isZero();
  }

  @Test
  void gzipOutputHasReferenceHeader() throws NbtException {
    // spec: core-nbt R16 (header 1f 8b 08 00 00 00 00 00 00 ff), "Conteneurs"
    byte[] gzip = NbtWriter.of(NbtCompression.GZIP).writeNamed(root(ProbeFiles.f1Tree()));
    assertThat(Hex.format(Arrays.copyOf(gzip, 10))).isEqualTo(GZIP_HEADER);
  }

  @Test
  void gzipOutputIsOneCompleteMember() throws NbtException {
    // spec: core-nbt R16 (single member): the trailer size covers all the decompressed bytes
    byte[] gzip = NbtWriter.of(NbtCompression.GZIP).writeNamed(root(ProbeFiles.f1Tree()));
    assertThat(Hex.format(Containers.gunzip(gzip))).isEqualTo(Hex.format(ProbeFiles.f1()));
    int trailerSize =
        ByteBuffer.wrap(gzip, gzip.length - 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    assertThat(trailerSize).isEqualTo(148);
  }

  @Test
  void gzipStreamOutputIsFinished() throws NbtException {
    // API contract of NbtWriter: the container is finished, the stream is not closed
    var target = new ByteArrayOutputStream();
    var out = new Containers.CloseTrackingOutputStream(target);
    NbtWriter.of(NbtCompression.GZIP).writeNamed(root(CompoundTag.empty()), out);
    assertThat(out.closed()).isFalse();
    assertThat(Hex.format(Containers.gunzip(target.toByteArray()))).isEqualTo("0a 00 00 00");
  }

  @Test
  void zlibOutputDecodes() throws NbtException {
    // spec: core-nbt R18
    byte[] zlib = NbtWriter.of(NbtCompression.ZLIB).writeNamed(root(ProbeFiles.f1Tree()));
    assertThat(Hex.format(Containers.inflate(zlib))).isEqualTo(Hex.format(ProbeFiles.f1()));
  }

  @Test
  void containersWrapUnnamedAndAbsentRoots() throws NbtException {
    // spec: core-nbt R20 inside a container
    byte[] unnamed = NbtWriter.of(NbtCompression.ZLIB).writeUnnamed(CompoundTag.empty());
    assertThat(Hex.format(Containers.inflate(unnamed))).isEqualTo("0a 00");
    var absent = new ByteArrayOutputStream();
    NbtWriter.of(NbtCompression.GZIP).writeAbsentRoot(absent);
    assertThat(Hex.format(Containers.gunzip(absent.toByteArray()))).isEqualTo("00");
  }
}
