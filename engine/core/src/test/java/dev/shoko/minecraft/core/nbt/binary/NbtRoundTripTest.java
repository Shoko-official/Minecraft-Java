package dev.shoko.minecraft.core.nbt.binary;

import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.doubleTag;
import static dev.shoko.minecraft.core.nbt.Tags.floatTag;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.longTag;
import static dev.shoko.minecraft.core.nbt.Tags.root;
import static dev.shoko.minecraft.core.nbt.Tags.shortTag;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static dev.shoko.minecraft.core.nbt.Tags.text;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.Hex;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.binary.BinaryVectors.Vector;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Reading then writing gives the same bytes (R30), and writing then reading the same tree. */
class NbtRoundTripTest {

  static byte[] rewrite(byte[] data) throws NbtException {
    NamedTag read = NbtReader.of(NbtCompression.NONE).readNamed(data).orElseThrow();
    return NbtWriter.of(NbtCompression.NONE).writeNamed(read);
  }

  static List<Vector> namedVectors() {
    return BinaryVectors.named();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("namedVectors")
  void rewritingVectorGivesSameBytes(Vector vector) throws NbtException {
    // spec: core-nbt R30, core-nbt-vectors table V ("lecture puis reecriture : memes octets")
    assertThat(Hex.format(rewrite(vector.bytes()))).isEqualTo(Hex.format(vector.bytes()));
  }

  @Test
  void emptyListOfByteIsRewrittenWithEndType() throws NbtException {
    // spec: core-nbt R22, R23, vector V24 rewritten as V10
    assertThat(Hex.format(rewrite(Hex.bytes("09 00 00 01 00 00 00 00"))))
        .isEqualTo("09 00 00 00 00 00 00 00");
  }

  @Test
  void unnamedRootRoundTrips() throws NbtException {
    // spec: core-nbt R20, vector V20
    var read = NbtReader.of(NbtCompression.NONE).readUnnamed(Hex.bytes("0a 00")).orElseThrow();
    assertThat(Hex.format(NbtWriter.of(NbtCompression.NONE).writeUnnamed(read))).isEqualTo("0a 00");
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "p00_base",
        "p01_root_name_x",
        "p11_extra_string_mutf8_pair",
        "p12_extra_string_lone_surrogate",
        "p15_extra_deep_510",
        "p16_extra_deep_511"
      })
  void rewritingAcceptedProbeGivesSameBytes(String probe) throws NbtException {
    // spec: core-nbt R11, R12, R15, R30, R31
    byte[] data = ProbeFiles.probe(probe);
    assertThat(Hex.format(rewrite(data))).isEqualTo(Hex.format(data));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "p02_entities_end_len_minus1",
        "p03_entities_byte_len_minus1",
        "p05_entities_byte_len_0",
        "p06_trailing_byte"
      })
  void normalizedProbeIsRewrittenAsF1(String probe) throws NbtException {
    // spec: core-nbt R22 (empty list as End), R23, R25, R34 (trailing byte dropped)
    assertThat(Hex.format(rewrite(ProbeFiles.probe(probe)))).isEqualTo(Hex.format(ProbeFiles.f1()));
  }

  @Test
  void deepestAcceptedNestingRoundTrips() throws NbtException {
    // spec: core-nbt R31 (512 containers), R30
    byte[] lists = BinaryVectors.nestedListsRoot(512);
    byte[] compounds = BinaryVectors.nestedCompoundsRoot(512);
    assertThat(rewrite(lists)).isEqualTo(lists);
    assertThat(rewrite(compounds)).isEqualTo(compounds);
  }

  static CompoundTag everyType() {
    return compound(
        "byte", byteTag(-128),
        "short", shortTag(32767),
        "int", intTag(-1),
        "long", longTag(Long.MIN_VALUE),
        "float", new FloatTag(0x7fc00001),
        "double", new DoubleTag(0x8000000000000000L),
        "bytes", ByteArrayTag.of((byte) 1, (byte) -1),
        "text", stringTag("\0" + text(0xE9, 0x20AC, 0x1F600)),
        "list", list(floatTag(0.5f), floatTag(-0.0f)),
        "empty", ListTag.empty(),
        "nested", list(list(intTag(1)), list(stringTag("a"))),
        "child", compound("z", byteTag(1), "a", doubleTag(Double.POSITIVE_INFINITY)),
        "ints", IntArrayTag.of(),
        "longs", LongArrayTag.of(Long.MAX_VALUE, 0L));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(NbtCompression.class)
  void treeOfEveryTypeRoundTripsInEveryContainer(NbtCompression compression) throws NbtException {
    // spec: core-nbt R1 to R11, R30, R35, "Conteneurs"
    NamedTag tree = new NamedTag("root", everyType());
    byte[] written = NbtWriter.of(compression).writeNamed(tree);
    NamedTag read = NbtReader.of(compression).readNamed(written).orElseThrow();
    assertThat(read).isEqualTo(tree);
    var keys = ((CompoundTag) read.tag()).entries().sequencedKeySet();
    assertThat(keys).containsExactlyElementsOf(everyType().entries().sequencedKeySet());
  }

  @Test
  void heterogeneousListReadsBackAsWrappedCompounds() throws NbtException {
    // spec: core-nbt R47, R63 (provisional: no unwrapping on reading, the round trip is not
    // reversible)
    byte[] written =
        NbtWriter.of(NbtCompression.NONE).writeNamed(root(list(intTag(1), stringTag("a"))));
    var read = NbtReader.of(NbtCompression.NONE).readNamed(written).orElseThrow();
    assertThat(read.tag()).isEqualTo(list(compound("", intTag(1)), compound("", stringTag("a"))));
    assertThat(read.tag()).isNotEqualTo(list(intTag(1), stringTag("a")));
  }

  @Test
  void compoundElementOfHeterogeneousListReadsBackWrapped() throws NbtException {
    // spec: core-nbt R63 (elements already Compound are wrapped too), provisional
    byte[] written =
        NbtWriter.of(NbtCompression.NONE)
            .writeNamed(root(list(compound("a", intTag(1)), intTag(2))));
    var read = NbtReader.of(NbtCompression.NONE).readNamed(written).orElseThrow();
    assertThat(read.tag())
        .isEqualTo(list(compound("", compound("a", intTag(1))), compound("", intTag(2))));
  }

  @Test
  void wrappedListIsRewrittenAsIs() throws NbtException {
    // spec: core-nbt R63: the read-back list of wrappers is an ordinary list of compounds, so a
    // second round trip gives the same bytes (no double wrapping, no unwrapping)
    byte[] once = NbtWriter.of(NbtCompression.NONE).writeNamed(root(list(byteTag(1), intTag(2))));
    assertThat(Hex.format(rewrite(once))).isEqualTo(Hex.format(once));
  }

  @Test
  void writtenGzipReadsBackWithIndependentDecoder() throws NbtException {
    // spec: core-nbt R16: our gzip output is plain RFC 1952
    byte[] gzip = NbtWriter.of(NbtCompression.GZIP).writeNamed(root(ProbeFiles.f1Tree()));
    byte[] decompressed = Containers.gunzip(gzip);
    assertThat(NbtReader.of(NbtCompression.NONE).readNamed(decompressed))
        .contains(root(ProbeFiles.f1Tree()));
  }
}
