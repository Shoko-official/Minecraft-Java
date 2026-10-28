package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Failures.assertFails;
import static dev.shoko.minecraft.core.nbt.Failures.assertFailsWithoutOffset;
import static dev.shoko.minecraft.core.nbt.Failures.failure;
import static dev.shoko.minecraft.core.nbt.Tags.root;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Nesting depth (R31), containers (R16 to R19) and stream failures. The size limit of R32 is in
 * NbtReaderQuotaTest, the container checks of R61 and R62 in NbtReaderContainersTest.
 */
class NbtReaderLimitsTest {

  static NbtReader reader() {
    return NbtReader.of(NbtCompression.NONE);
  }

  @Test
  void maximumDepthIs512() {
    // spec: core-nbt R31
    assertThat(NbtReader.MAX_DEPTH).isEqualTo(512);
  }

  @Test
  void reads512NestedLists() throws NbtException {
    // spec: core-nbt R31, vector E09a, provisional: root counts as one level
    assertThat(reader().readNamed(BinaryVectors.nestedListsRoot(512))).isPresent();
  }

  @Test
  void rejects513NestedLists() {
    // spec: core-nbt R31, vector E09b; offset: first payload byte of container 513 = 3 + 5 * 512
    assertFails(
        () -> reader().readNamed(BinaryVectors.nestedListsRoot(513)), Reason.TOO_DEEP, 2563);
  }

  @Test
  void reads512NestedCompounds() throws NbtException {
    // spec: core-nbt R31 (lists and compounds both count), provisional
    assertThat(reader().readNamed(BinaryVectors.nestedCompoundsRoot(512))).isPresent();
  }

  @Test
  void rejects513NestedCompounds() {
    // spec: core-nbt R31; offset: first payload byte of container 513 = 3 + 3 * 512
    assertFails(
        () -> reader().readNamed(BinaryVectors.nestedCompoundsRoot(513)), Reason.TOO_DEEP, 1539);
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"p15_extra_deep_510", "p16_extra_deep_511"})
  void acceptsDeepListInFileUpTo512Containers(String probe) throws NbtException {
    // spec: core-nbt R31, probes p15 (511 containers) and p16 (512 containers), provisional
    assertThat(reader().readNamed(ProbeFiles.probe(probe))).isPresent();
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"p17_extra_deep_512", "p18_extra_deep_513"})
  void rejectsDeepListInFileBeyond512Containers(String probe) {
    // spec: core-nbt R31, probes p17 (513) and p18 (514); container 513 starts its payload at
    // the payload of q (container 2) plus 5 bytes per list level: 151 + 5 * 511
    long offset = ProbeFiles.insertedPayloadOffset() + 5L * 511;
    assertFails(() -> reader().readNamed(ProbeFiles.probe(probe)), Reason.TOO_DEEP, offset);
  }

  @Test
  void readsGzipContainer() throws NbtException {
    // spec: core-nbt R16, R17
    byte[] gzip = Containers.gzip(ProbeFiles.f1());
    assertThat(NbtReader.of(NbtCompression.GZIP).readNamed(gzip))
        .contains(root(ProbeFiles.f1Tree()));
    assertThat(NbtReader.of(NbtCompression.GZIP).readNamed(new ByteArrayInputStream(gzip)))
        .contains(root(ProbeFiles.f1Tree()));
  }

  @Test
  void readsZlibContainer() throws NbtException {
    // spec: core-nbt R18
    byte[] zlib = Containers.zlib(ProbeFiles.f1());
    assertThat(NbtReader.of(NbtCompression.ZLIB).readNamed(zlib))
        .contains(root(ProbeFiles.f1Tree()));
  }

  @Test
  void readsUnnamedRootInContainer() throws NbtException {
    // spec: core-nbt R18, R20
    assertThat(NbtReader.of(NbtCompression.ZLIB).readUnnamed(Containers.zlib(Hex.bytes("0a 00"))))
        .contains(CompoundTag.empty());
    assertThat(NbtReader.of(NbtCompression.GZIP).readNamed(Containers.gzip(Hex.bytes("00"))))
        .isEmpty();
  }

  @Test
  void truncationInsideContainerIsDetected() {
    // spec: core-nbt R33: a complete gzip stream holding a truncated root
    byte[] gzip = Containers.gzip(Hex.bytes("0a 00 00 01 00 01 61 01"));
    assertFails(() -> NbtReader.of(NbtCompression.GZIP).readNamed(gzip), Reason.UNEXPECTED_END, 8);
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = NbtCompression.class,
      names = {"GZIP", "ZLIB"})
  void undecodableContainerFails(NbtCompression compression) {
    // spec: core-nbt "Conteneurs" (chosen by the caller, no detection): raw bytes are not gzip
    // or zlib
    byte[] raw = Hex.bytes("0a 00 00 00");
    assertFailsWithoutOffset(
        () -> NbtReader.of(compression).readNamed(raw), Reason.CORRUPT_CONTAINER);
  }

  @Test
  void corruptGzipFails() {
    // spec: core-nbt R16: gzip magic followed by an invalid compression method
    byte[] corrupt = Hex.bytes("1f 8b 63 00 00 00 00 00 00 ff 00 00 00");
    assertFailsWithoutOffset(
        () -> NbtReader.of(NbtCompression.GZIP).readNamed(corrupt), Reason.CORRUPT_CONTAINER);
  }

  @Test
  void streamFailureIsReportedWithItsCause() {
    // API contract: IO_FAILURE has the IOException as cause and no offset
    NbtException thrown = failure(() -> reader().readNamed(new Containers.FailingInputStream()));
    assertThat(thrown.reason()).isEqualTo(Reason.IO_FAILURE);
    assertThat(thrown.offset()).isEmpty();
    assertThat(thrown).hasCauseInstanceOf(IOException.class);
  }
}
