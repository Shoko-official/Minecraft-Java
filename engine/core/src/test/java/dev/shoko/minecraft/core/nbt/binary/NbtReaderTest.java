package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.root;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static dev.shoko.minecraft.core.nbt.Tags.text;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.TagType;
import dev.shoko.minecraft.core.nbt.binary.BinaryVectors.Vector;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Valid binary input: vectors V, file F1 and the accepted probes of PB1. */
class NbtReaderTest {

  static NbtReader reader() {
    return NbtReader.of(NbtCompression.NONE);
  }

  static NamedTag read(byte[] data) throws NbtException {
    return reader().readNamed(data).orElseThrow();
  }

  static CompoundTag readCompound(byte[] data) throws NbtException {
    NbtTag tag = read(data).tag();
    assertThat(tag).isInstanceOf(CompoundTag.class);
    return (CompoundTag) tag;
  }

  static List<Vector> namedVectors() {
    return BinaryVectors.named();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("namedVectors")
  void readsNamedVector(Vector vector) throws NbtException {
    // spec: core-nbt R1 to R11, R35 (vectors V02 to V23)
    assertThat(reader().readNamed(vector.bytes())).contains(vector.value().get());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("namedVectors")
  void readsNamedVectorFromStream(Vector vector) throws NbtException {
    // spec: core-nbt R1 to R11 (same result from a stream as from an array)
    var in = new ByteArrayInputStream(vector.bytes());
    assertThat(reader().readNamed(in)).contains(vector.value().get());
  }

  @Test
  void singleEndByteIsAnAbsentRoot() throws NbtException {
    // spec: core-nbt R20, vector V01
    assertThat(reader().readNamed(Hex.bytes("00"))).isEmpty();
    assertThat(reader().readUnnamed(Hex.bytes("00"))).isEmpty();
    assertThat(reader().readNamed(new ByteArrayInputStream(Hex.bytes("00")))).isEmpty();
  }

  @Test
  void readsUnnamedNetworkRoot() throws NbtException {
    // spec: core-nbt R20, vector V20 (type then payload, no name)
    assertThat(reader().readUnnamed(Hex.bytes("0a 00"))).contains(CompoundTag.empty());
    assertThat(reader().readUnnamed(Hex.bytes("03 00 00 00 05"))).contains(intTag(5));
    assertThat(reader().readUnnamed(new ByteArrayInputStream(Hex.bytes("0a 01 00 01 61 01 00"))))
        .contains(compound("a", byteTag(1)));
  }

  @Test
  void keepsKeyReadOrder() throws NbtException {
    // spec: core-nbt R30, vector V21 (z then a)
    var tag = readCompound(Hex.bytes("0a 00 00 01 00 01 7a 01 01 00 01 61 02 00"));
    assertThat(tag.entries().sequencedKeySet()).containsExactly("z", "a");
  }

  @Test
  void decodesReferenceFileF1() throws NbtException {
    // spec: core-nbt R14, R30, core-nbt-vectors "Arbre attendu de F1"
    NamedTag f1 = read(ProbeFiles.f1());
    assertThat(f1.name()).isEmpty();
    assertThat(f1.tag()).isEqualTo(ProbeFiles.f1Tree());
    var tag = (CompoundTag) f1.tag();
    assertThat(tag.entries().sequencedKeySet())
        .containsExactly("size", "entities", "blocks", "palette", "DataVersion");
    assertThat(tag.getInt("DataVersion")).isEqualTo(5023);
  }

  @Test
  void keepsNonEmptyRootName() throws NbtException {
    // spec: core-nbt R15, probe p01, provisional: accepted, name kept for rewriting (R30)
    NamedTag probe = read(ProbeFiles.probe("p01_root_name_x"));
    assertThat(probe.name()).isEqualTo("x");
    assertThat(probe.tag()).isEqualTo(ProbeFiles.f1Tree());
  }

  @Test
  void emptyListOfAnotherTypeReadsAsEmptyList() throws NbtException {
    // spec: core-nbt R23, vector V24, probe p05
    NbtTag v24 = read(Hex.bytes("09 00 00 01 00 00 00 00")).tag();
    assertThat(v24).isEqualTo(ListTag.empty());
    assertThat(((ListTag) v24).elementType()).isEqualTo(TagType.END);
    assertThat(readCompound(ProbeFiles.probe("p05_entities_byte_len_0")))
        .isEqualTo(ProbeFiles.f1Tree());
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "09 00 00 01 ff ff ff ff",
        "09 00 00 00 ff ff ff ff",
        "09 00 00 03 80 00 00 00",
        "09 00 00 0a ff ff ff fe"
      })
  void negativeListLengthReadsAsEmptyList(String hex) throws NbtException {
    // spec: core-nbt R25, vector E05, provisional: empty list whatever the element type
    assertThat(read(Hex.bytes(hex))).isEqualTo(root(ListTag.empty()));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"p02_entities_end_len_minus1", "p03_entities_byte_len_minus1"})
  void negativeListLengthInFileReadsAsEmptyList(String probe) throws NbtException {
    // spec: core-nbt R25, probes p02 and p03, provisional
    assertThat(readCompound(ProbeFiles.probe(probe))).isEqualTo(ProbeFiles.f1Tree());
  }

  @Test
  void readsListOfEmptyListsOfDifferentTypes() throws NbtException {
    // spec: core-nbt "Cas limites" (list of empty lists of different types), R7, R23
    var tag = read(Hex.bytes("09 00 00 09 00 00 00 02 01 00 00 00 00 03 00 00 00 00")).tag();
    assertThat(tag).isEqualTo(list(ListTag.empty(), ListTag.empty()));
    assertThat(((ListTag) tag).elementType()).isEqualTo(TagType.LIST);
  }

  @Test
  void duplicateKeyKeepsLastValue() throws NbtException {
    // spec: core-nbt R27, provisional
    var same = readCompound(Hex.bytes("0a 00 00 01 00 01 61 01 01 00 01 61 02 00"));
    assertThat(same).isEqualTo(compound("a", byteTag(2)));
    assertThat(same.entries()).hasSize(1);
    var changed = readCompound(Hex.bytes("0a 00 00 01 00 01 61 01 08 00 01 61 00 01 78 00"));
    assertThat(changed).isEqualTo(compound("a", stringTag("x")));
  }

  @Test
  void duplicatePaletteIdKeepsLastValue() throws NbtException {
    // spec: core-nbt R27, probe p14 (stone expected at (0,100,0)), provisional
    var palette =
        readCompound(ProbeFiles.probe("p14_duplicate_palette_id_stone")).getList("palette");
    assertThat(palette.elements()).containsExactly(compound("id", stringTag("minecraft:stone")));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"0a 00 00 00 00", "0a 00 00 00 0a 00 00 00", "0a 00 00 00 ff ff ff"})
  void ignoresBytesAfterRoot(String hex) throws NbtException {
    // spec: core-nbt R34, provisional
    assertThat(read(Hex.bytes(hex))).isEqualTo(root(CompoundTag.empty()));
  }

  @Test
  void ignoresTrailingByteOfFile() throws NbtException {
    // spec: core-nbt R34, probe p06, provisional
    assertThat(readCompound(ProbeFiles.probe("p06_trailing_byte"))).isEqualTo(ProbeFiles.f1Tree());
  }

  @Test
  void acceptsUnpairedSurrogates() throws NbtException {
    // spec: core-nbt R12, provisional: an isolated surrogate is accepted as is
    assertThat(read(Hex.bytes("08 00 00 00 03 ed a0 bd")).tag()).isEqualTo(stringTag("\uD83D"));
    assertThat(read(Hex.bytes("08 00 00 00 03 ed b8 80")).tag()).isEqualTo(stringTag("\uDE00"));
    assertThat(read(Hex.bytes("08 00 00 00 06 ed b8 80 ed a0 bd")).tag())
        .isEqualTo(stringTag("\uDE00\uD83D"));
  }

  @Test
  void acceptsModifiedUtf8InFile() throws NbtException {
    // spec: core-nbt R11 and R12, probes p11 (surrogate pair) and p12 (lone surrogate)
    assertThat(readCompound(ProbeFiles.probe("p11_extra_string_mutf8_pair")).getString("q"))
        .isEqualTo(text(0x1F600));
    assertThat(readCompound(ProbeFiles.probe("p12_extra_string_lone_surrogate")).getString("q"))
        .isEqualTo("\uD83D");
  }

  @Test
  void decodesNonAsciiKeysAndRootName() throws NbtException {
    // spec: core-nbt R5 (names are coded as strings), R11
    var named = read(Hex.bytes("0a 00 02 c3 a9 01 00 03 e2 82 ac 01 00"));
    assertThat(named.name()).isEqualTo(text(0xE9));
    assertThat(named.tag()).isEqualTo(compound(text(0x20AC), byteTag(1)));
  }

  @Test
  void readsLongestEncodableString() throws NbtException {
    // spec: core-nbt R10, "Cas limites" (String of 65535 bytes)
    byte[] data =
        Hex.concat(
            Hex.bytes("08 00 00 ff ff"), "a".repeat(65535).getBytes(StandardCharsets.US_ASCII));
    assertThat(read(data).tag()).isEqualTo(stringTag("a".repeat(65535)));
  }

  @Test
  void readsEmptyString() throws NbtException {
    // spec: core-nbt "Cas limites" (String vide 00 00)
    assertThat(read(Hex.bytes("08 00 00 00 00")).tag()).isEqualTo(stringTag(""));
  }

  @Test
  void doesNotCloseStream() throws NbtException {
    // API contract of NbtReader: the stream is not closed
    var in = new Containers.CloseTrackingInputStream(ProbeFiles.f1());
    assertThat(reader().readNamed(in)).isPresent();
    assertThat(in.closed()).isFalse();
  }
}
