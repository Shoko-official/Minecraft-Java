package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Failures.failure;
import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.root;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static dev.shoko.minecraft.core.nbt.Tags.text;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.binary.BinaryVectors.Vector;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Binary writing without container: exact bytes (R1 to R11, R22, R30, R35, R47). */
class NbtWriterTest {

  static NbtWriter writer() {
    return NbtWriter.of(NbtCompression.NONE);
  }

  static String writeRoot(NbtTag tag) throws NbtException {
    return Hex.format(writer().writeNamed(root(tag)));
  }

  static List<Vector> namedVectors() {
    return BinaryVectors.named();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("namedVectors")
  void writesNamedVector(Vector vector) throws NbtException {
    // spec: core-nbt R1 to R11, R30, R35 (vectors V02 to V23)
    assertThat(Hex.format(writer().writeNamed(vector.value().get())))
        .isEqualTo(Hex.format(vector.bytes()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("namedVectors")
  void writesNamedVectorToStream(Vector vector) throws NbtException {
    // spec: core-nbt R1 to R11 (same bytes on a stream as in an array)
    var out = new ByteArrayOutputStream();
    writer().writeNamed(vector.value().get(), out);
    assertThat(Hex.format(out.toByteArray())).isEqualTo(Hex.format(vector.bytes()));
  }

  @Test
  void writesAbsentRootAsSingleEndByte() throws NbtException {
    // spec: core-nbt R20, vector V01
    var out = new ByteArrayOutputStream();
    writer().writeAbsentRoot(out);
    assertThat(Hex.format(out.toByteArray())).isEqualTo("00");
  }

  @Test
  void writesUnnamedNetworkRoot() throws NbtException {
    // spec: core-nbt R20, vector V20
    assertThat(Hex.format(writer().writeUnnamed(CompoundTag.empty()))).isEqualTo("0a 00");
    assertThat(Hex.format(writer().writeUnnamed(intTag(5)))).isEqualTo("03 00 00 00 05");
    var out = new ByteArrayOutputStream();
    writer().writeUnnamed(compound("a", byteTag(1)), out);
    assertThat(Hex.format(out.toByteArray())).isEqualTo("0a 01 00 01 61 01 00");
  }

  @Test
  void writesNonEmptyRootName() throws NbtException {
    // spec: core-nbt R5, R15 (name kept so that rewriting gives the same bytes)
    var named = new NamedTag("x", CompoundTag.empty());
    assertThat(Hex.format(writer().writeNamed(named))).isEqualTo("0a 00 01 78 00");
  }

  @Test
  void writesReferenceFileF1FromItsTree() throws NbtException {
    // spec: core-nbt R30, core-nbt-vectors F1 (148 bytes)
    assertThat(writeRoot(ProbeFiles.f1Tree())).isEqualTo(Hex.format(ProbeFiles.f1()));
  }

  @Test
  void writesEmptyListWithEndType() throws NbtException {
    // spec: core-nbt R22, vectors V10 and V24, probe PW1 (key "empty")
    assertThat(writeRoot(ListTag.of(List.of()))).isEqualTo("09 00 00 00 00 00 00 00");
    assertThat(writeRoot(compound("empty", ListTag.empty())))
        .isEqualTo("0a 00 00 09 00 05 65 6d 70 74 79 00 00 00 00 00 00");
  }

  @Test
  void writesListOfEmptyListsWithEndTypes() throws NbtException {
    // spec: core-nbt R7, R22
    assertThat(writeRoot(list(ListTag.empty(), ListTag.empty())))
        .isEqualTo("09 00 00 09 00 00 00 02 00 00 00 00 00 00 00 00 00 00");
  }

  @Test
  void writesKeysInInsertionOrder() throws NbtException {
    // spec: core-nbt R30 (order of insertion, not lexicographic)
    assertThat(writeRoot(compound("b", byteTag(1), "a", byteTag(2), "c", byteTag(3))))
        .isEqualTo("0a 00 00 01 00 01 62 01 01 00 01 61 02 01 00 01 63 03 00");
  }

  @Test
  void writesHeterogeneousListAsWrappedCompounds() throws NbtException {
    // spec: core-nbt R47, probe PW1 (key "het": [1,"a"] as [{"":1}, {"":"a"}])
    assertThat(writeRoot(compound("het", list(intTag(1), stringTag("a")))))
        .isEqualTo(
            "0a 00 00 09 00 03 68 65 74 0a 00 00 00 02"
                + " 03 00 00 00 00 00 01 00 08 00 00 00 01 61 00 00");
  }

  @Test
  void writesByteAndIntListAsWrappedCompounds() throws NbtException {
    // spec: core-nbt R47, T38 [1b,2], provisional: no promotion, [{"":1b}, {"":2}]
    assertThat(writeRoot(list(byteTag(1), intTag(2))))
        .isEqualTo("09 00 00 0a 00 00 00 02 01 00 00 01 00 03 00 00 00 00 00 02 00");
  }

  @Test
  void wrapsCompoundElementsOfHeterogeneousList() throws NbtException {
    // spec: core-nbt R47, provisional: [{a:1}, 2] becomes [{"":{a:1}}, {"":2}]
    assertThat(writeRoot(list(compound("a", intTag(1)), intTag(2))))
        .isEqualTo(
            "09 00 00 0a 00 00 00 02"
                + " 0a 00 00 03 00 01 61 00 00 00 01 00 00"
                + " 03 00 00 00 00 00 02 00");
  }

  @Test
  void doesNotWrapHomogeneousListOfCompounds() throws NbtException {
    // spec: core-nbt R7, R47 (wrapping only for different types)
    assertThat(writeRoot(list(compound("a", byteTag(1)), CompoundTag.empty())))
        .isEqualTo("09 00 00 0a 00 00 00 02 01 00 01 61 01 00 00");
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "Float NaN 7fc00001, 7fc00001, 05 00 00 7f c0 00 01",
    "Float NaN ffc00000, ffc00000, 05 00 00 ff c0 00 00",
    "Float NaN 7f800001, 7f800001, 05 00 00 7f 80 00 01",
    "Float -0.0, 80000000, 05 00 00 80 00 00 00",
    "Float -infinity, ff800000, 05 00 00 ff 80 00 00"
  })
  void writesExactFloatBitPattern(String name, String bits, String hex) throws NbtException {
    // spec: core-nbt R35, probe PW2 (n1 to n3), PW1 (nzf)
    var tag = new FloatTag(Integer.parseUnsignedInt(bits, 16));
    assertThat(writeRoot(tag)).isEqualTo(hex);
  }

  @Test
  void writesExactDoubleBitPattern() throws NbtException {
    // spec: core-nbt R35, probe PW2 (n4 = 7ff0000000000001)
    assertThat(writeRoot(new DoubleTag(0x7ff0000000000001L)))
        .isEqualTo("06 00 00 7f f0 00 00 00 00 00 01");
  }

  @Test
  void writesComputedNanAsCanonicalPattern() throws NbtException {
    // spec: core-nbt R35, provisional (7fc00000 and 7ff8000000000000)
    assertThat(writeRoot(FloatTag.of(Float.NaN))).isEqualTo("05 00 00 7f c0 00 00");
    assertThat(writeRoot(DoubleTag.of(Double.NaN))).isEqualTo("06 00 00 7f f8 00 00 00 00 00 00");
  }

  @Test
  void writesSupplementaryCharacterAsTwoSurrogates() throws NbtException {
    // spec: core-nbt R11 (never a 4-byte sequence), probe PW1 (s3)
    assertThat(writeRoot(stringTag(new String(Character.toChars(0x1F600)))))
        .isEqualTo("08 00 00 00 06 ed a0 bd ed b8 80");
  }

  @Test
  void writesUnpairedSurrogateAsThreeBytes() throws NbtException {
    // spec: core-nbt R11, R12 (lone surrogate accepted as is, so it must be writable)
    assertThat(writeRoot(stringTag("\uD83D"))).isEqualTo("08 00 00 00 03 ed a0 bd");
  }

  @Test
  void writesNonAsciiKeysAndNames() throws NbtException {
    // spec: core-nbt R5, R11
    var named = new NamedTag(text(0xE9), compound(text(0x20AC), byteTag(1)));
    assertThat(Hex.format(writer().writeNamed(named)))
        .isEqualTo("0a 00 02 c3 a9 01 00 03 e2 82 ac 01 00");
  }

  @Test
  void doesNotCloseStream() throws NbtException {
    // API contract of NbtWriter: the stream is not closed
    var out = new Containers.CloseTrackingOutputStream(new ByteArrayOutputStream());
    writer().writeNamed(root(CompoundTag.empty()), out);
    assertThat(out.closed()).isFalse();
  }

  @Test
  void streamFailureIsReportedWithItsCause() {
    // API contract: IO_FAILURE has the IOException as cause and no offset
    NbtException thrown =
        failure(
            () ->
                writer()
                    .writeNamed(root(ProbeFiles.f1Tree()), new Containers.FailingOutputStream()));
    assertThat(thrown.reason()).isEqualTo(Reason.IO_FAILURE);
    assertThat(thrown.offset()).isEmpty();
    assertThat(thrown).hasCauseInstanceOf(IOException.class);
  }
}
