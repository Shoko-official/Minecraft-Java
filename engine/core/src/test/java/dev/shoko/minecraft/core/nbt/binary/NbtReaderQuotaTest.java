package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Failures.assertFails;
import static dev.shoko.minecraft.core.nbt.Tags.root;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Quota of estimated memory (R32, R60), vectors Q0 to Q9 and probes p19, p20. Units: 1 per
 * decompressed byte consumed from the root type to its last byte, plus 24 per tag object. The
 * explicit quota is the {@code long} of {@code NbtReader.of(compression, quota)}; the offset of
 * {@code TOO_LARGE} is not pinned down by the spec and is not checked.
 */
class NbtReaderQuotaTest {

  /** Default quota of R32: 134 217 728 units (128 Mio). */
  private static final long DEFAULT_QUOTA = 134_217_728L;

  /** Q6: {@code {a:[B; n zeros]}} counts n + 60 units, exactly the default quota. */
  private static final int Q6_ARRAY_LENGTH = (int) (DEFAULT_QUOTA - 60);

  /** R32: the explicit quota 2^63-1 means no limit. */
  private static final long UNLIMITED = Long.MAX_VALUE;

  static NbtReader withQuota(long quota) {
    return NbtReader.of(NbtCompression.NONE, quota);
  }

  static byte[] int32(int value) {
    return ByteBuffer.allocate(4).putInt(value).array();
  }

  /** {@code {a:[B; n zeros]}}, as {@code ba(n)} of {@code nbt_ref.py quota}. */
  static byte[] byteArrayCompound(int n) {
    return Hex.concat(Hex.bytes("0a 00 00 07 00 01 61"), int32(n), new byte[n], new byte[1]);
  }

  /** Same as {@link #byteArrayCompound(int)}, streamed for large {@code n}. */
  static InputStream byteArrayCompoundStream(int n) {
    return ProbeFiles.concat(
        Hex.concat(Hex.bytes("0a 00 00 07 00 01 61"), int32(n)), ProbeFiles.zeros(n), new byte[1]);
  }

  /** {@code {a:[n Int zeros]}} (List of Int), as {@code il(n)} of {@code nbt_ref.py quota}. */
  static byte[] intListCompound(int n) {
    return Hex.concat(Hex.bytes("0a 00 00 09 00 01 61 03"), int32(n), new byte[4 * n], new byte[1]);
  }

  @Test
  void p19IsAcceptedUnderDefaultQuota() throws NbtException {
    // spec: core-nbt R32, R60, Q0 (p19: 67 109 452 units, the array counts for its size)
    var probe =
        (CompoundTag)
            NbtReader.of(NbtCompression.NONE).readNamed(ProbeFiles.p19Stream()).orElseThrow().tag();
    assertThat(probe.get("q"))
        .get()
        .isInstanceOfSatisfying(
            ByteArrayTag.class, q -> assertThat(q.size()).isEqualTo(ProbeFiles.P19_ARRAY_LENGTH));
  }

  @Test
  void p20IsRefusedUnderDefaultQuota() {
    // spec: core-nbt R32, R60, PB1 p20 (500 000 589 units), built in memory as a gzip file
    byte[] gzip = Containers.gzip(ProbeFiles.p20Stream());
    assertFails(() -> NbtReader.of(NbtCompression.GZIP).readNamed(gzip), Reason.TOO_LARGE);
  }

  @Test
  void f1CountsBytesAndTagObjectsExactly() throws NbtException {
    // spec: core-nbt R60, Q1 (148 bytes + 17 tag objects x 24 = 556 units)
    assertThat(withQuota(556).readNamed(ProbeFiles.f1())).contains(root(ProbeFiles.f1Tree()));
    assertFails(() -> withQuota(555).readNamed(ProbeFiles.f1()), Reason.TOO_LARGE);
  }

  @Test
  void rootNameBytesAreCounted() throws NbtException {
    // spec: core-nbt R60 (name of the root counted in named mode): p01 is F1 plus 1 byte
    byte[] probe = ProbeFiles.probe("p01_root_name_x");
    assertThat(withQuota(557).readNamed(probe)).isPresent();
    assertFails(() -> withQuota(556).readNamed(probe), Reason.TOO_LARGE);
  }

  @Test
  void unnamedRootCountsTypeAndPayload() throws NbtException {
    // spec: core-nbt R20, R60: "0a 00" is 2 bytes + the root tag object = 26 units
    assertThat(withQuota(26).readUnnamed(Hex.bytes("0a 00"))).contains(CompoundTag.empty());
    assertFails(() -> withQuota(25).readUnnamed(Hex.bytes("0a 00")), Reason.TOO_LARGE);
  }

  @Test
  void bytesAfterRootAreNotCounted() throws NbtException {
    // spec: core-nbt R34, R60 (octets apres la racine non comptes), probe p06
    assertThat(withQuota(556).readNamed(ProbeFiles.probe("p06_trailing_byte"))).isPresent();
  }

  @Test
  void quotaCountsDecompressedBytes() throws NbtException {
    // spec: core-nbt R60 (bytes of the decompressed payload): the gzip size does not matter
    byte[] gzip = Containers.gzip(ProbeFiles.f1());
    assertThat(NbtReader.of(NbtCompression.GZIP, 556).readNamed(gzip)).isPresent();
    assertFails(() -> NbtReader.of(NbtCompression.GZIP, 555).readNamed(gzip), Reason.TOO_LARGE);
  }

  @Test
  void totalEqualToQuotaIsAccepted() throws NbtException {
    // spec: core-nbt R60 (accepted if and only if total <= quota), Q2 (1024 units)
    assertThat(withQuota(1024).readNamed(byteArrayCompound(964))).isPresent();
  }

  @Test
  void totalOneUnitOverQuotaIsRefused() {
    // spec: core-nbt R60, Q3 (1025 units, refused at the End byte of the root)
    assertFails(() -> withQuota(1024).readNamed(byteArrayCompound(965)), Reason.TOO_LARGE);
  }

  @Test
  void listOfIntWithinQuotaIsAccepted() throws NbtException {
    // spec: core-nbt R60, Q4 (1013 units)
    assertThat(withQuota(1024).readNamed(intListCompound(34))).isPresent();
  }

  @Test
  void listHeaderOverQuotaIsRefusedBeforeItsElements() {
    // spec: core-nbt R60, Q5 (60 + 35 x 28 = 1040 > 1024 at the list header); the same header
    // followed by nothing is refused the same way, not as a premature end
    byte[] full = intListCompound(35);
    byte[] headerOnly = Arrays.copyOf(full, 12);
    assertFails(() -> withQuota(1024).readNamed(full), Reason.TOO_LARGE);
    assertFails(() -> withQuota(1024).readNamed(headerOnly), Reason.TOO_LARGE);
  }

  @Test
  void defaultQuotaAcceptsTotalOf128Mebibytes() throws NbtException {
    // spec: core-nbt R32, Q6 (134 217 728 units, equal to the default quota); 128 MiB read
    var tag =
        (CompoundTag)
            NbtReader.of(NbtCompression.NONE)
                .readNamed(byteArrayCompoundStream(Q6_ARRAY_LENGTH))
                .orElseThrow()
                .tag();
    assertThat(tag.get("a")).get().isInstanceOf(ByteArrayTag.class);
  }

  @Test
  void defaultQuotaRefusesOneUnitMore() {
    // spec: core-nbt R32, Q7 (134 217 729 units)
    assertFails(
        () ->
            NbtReader.of(NbtCompression.NONE)
                .readNamed(byteArrayCompoundStream(Q6_ARRAY_LENGTH + 1)),
        Reason.TOO_LARGE);
  }

  @Test
  void explicitMaximumQuotaMeansNoLimit() throws NbtException {
    // spec: core-nbt R32 (2^63-1 = no limit, asked explicitly): Q7 is then accepted
    assertThat(withQuota(UNLIMITED).readNamed(byteArrayCompoundStream(Q6_ARRAY_LENGTH + 1)))
        .isPresent();
  }

  @Test
  void twentyMillionByteListIsRefused() {
    // spec: core-nbt R32, R60, Q8 ({b:[20 000 000 Byte]}, 500 000 061 units, refused at the
    // list header), compressed in memory
    byte[] head = Hex.bytes("0a 00 00 09 00 01 62 01 01 31 2d 00");
    byte[] gzip =
        Containers.gzip(ProbeFiles.concat(head, ProbeFiles.zeros(20_000_000), new byte[1]));
    assertFails(() -> NbtReader.of(NbtCompression.GZIP).readNamed(gzip), Reason.TOO_LARGE);
  }

  @Test
  void truncatedListDeclaringMaximumLengthIsRefusedAtHeader() {
    // spec: core-nbt R60, Q9 (12 bytes): TOO_LARGE, not a premature end of data
    byte[] q9 = Hex.bytes("0a 00 00 09 00 01 62 01 7f ff ff ff");
    assertFails(() -> NbtReader.of(NbtCompression.NONE).readNamed(q9), Reason.TOO_LARGE);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "Byte_Array of 2^31-1 declared, 07 00 00 7f ff ff ff",
    "Int_Array of 2^31-1 declared, 0b 00 00 7f ff ff ff 00 00 00 01",
    "Long_Array of 2^31-1 declared, 0c 00 00 7f ff ff ff",
    "List of Long of 2^31-1 declared, 09 00 00 04 7f ff ff ff"
  })
  void hugeDeclaredLengthIsRefusedAtHeaderByDefault(String name, String hex) {
    // spec: core-nbt R60 (pre-check at the header, even if the input is truncated afterwards)
    assertFails(
        () -> NbtReader.of(NbtCompression.NONE).readNamed(Hex.bytes(hex)), Reason.TOO_LARGE);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "Byte_Array of 2^31-1 declared, 07 00 00 7f ff ff ff, 7",
    "Int_Array of 2^31-1 declared, 0b 00 00 7f ff ff ff 00 00 00 01, 11",
    "Long_Array of 2^31-1 declared, 0c 00 00 7f ff ff ff, 7",
    "List of Long of 2^31-1 declared, 09 00 00 04 7f ff ff ff, 8"
  })
  void hugeDeclaredLengthWithoutLimitFailsAtEndOfData(String name, String hex, long available) {
    // spec: core-nbt R33, "Notes d'implementation" (no allocation from a declared length): with
    // no limit, a truncated input is a premature end, not an exhausted memory
    assertFails(
        () -> withQuota(UNLIMITED).readNamed(Hex.bytes(hex)), Reason.UNEXPECTED_END, available);
  }

  @Test
  void compressedBombIsRefusedByExplicitQuota() {
    // spec: core-nbt R32, R60: 2 MiB Byte_Array compressed to a few KiB, quota of 1 Mi units
    byte[] gzip = Containers.gzip(Hex.concat(Hex.bytes("07 00 00 00 20 00 00"), new byte[2 << 20]));
    assertFails(() -> NbtReader.of(NbtCompression.GZIP, 1 << 20).readNamed(gzip), Reason.TOO_LARGE);
  }

  @ParameterizedTest(name = "quota {0}")
  @ValueSource(longs = {0, -1, Long.MIN_VALUE})
  void quotaBelowOneIsRejected(long quota) {
    // API contract of NbtReader.of(compression, maxBytes)
    assertThatThrownBy(() -> NbtReader.of(NbtCompression.NONE, quota))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
