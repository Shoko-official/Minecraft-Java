package dev.shoko.minecraft.world.support;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.binary.NbtCompression;
import dev.shoko.minecraft.core.nbt.binary.NbtReader;
import dev.shoko.minecraft.world.block.BlockReportEntry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/**
 * The test helpers themselves are checked against the vectors of the spec, so that a failing test
 * of the code under test cannot come from a wrong fixture.
 */
class FixturesTest {

  @ParameterizedTest(name = "{0}")
  @CsvFileSource(resources = "/golden/chunk-nbt-vectors.csv")
  void chunkBuilderReproducesTheVectorsOfTheSpec(String variant, int size, String sha256) {
    // spec: world-vectors RG1, RG3, RG4 (sizes and SHA-256 of the NBT of chunk A)
    byte[] nbt =
        variant.equals("chunkB")
            ? ChunkNbt.bytes(ChunkNbt.chunk(-32, -32, false, "bedrock"))
            : ChunkNbt.chunkA(variant);

    assertThat(nbt).hasSize(size);
    assertThat(Hex.sha256(nbt)).isEqualTo(sha256);
  }

  @Test
  void xxh32MatchesTheControlValuesOfTheSpec() {
    // spec: world-vectors LZ1
    assertThat(Xxh32.hash(new byte[0], 0)).isEqualTo(0x02cc5d05);
    assertThat(Xxh32.hash("a".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0))
        .isEqualTo(0x550d7456);
    assertThat(Xxh32.hash("abc".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0))
        .isEqualTo(0x32d153ff);
    byte[] plain = "minecraft".repeat(8).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    assertThat(Xxh32.hash(plain, Lz4Frames.SEED)).isEqualTo(0x1f6103ad);
    assertThat(Lz4Frames.checksum(plain)).isEqualTo(0x0f6103ad);
  }

  @Test
  void rawFrameOfLz1HasTheHeaderOfTheSpec() {
    // spec: world-vectors LZ1 (raw block header)
    byte[] plain = "minecraft".repeat(8).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    byte[] stream = Lz4Frames.rawStream(plain, 6);

    assertThat(Hex.format(java.util.Arrays.copyOf(stream, 21)))
        .isEqualTo("4c 5a 34 42 6c 6f 63 6b 16 48 00 00 00 48 00 00 00 ad 03 61 0f");
  }

  @Test
  void referencePackingMatchesTheVectorsOfTheSpec() {
    // spec: world-vectors PK2, PK3, PK5a
    long[] pk2 = RefPacking.pack(RefPacking.cycle(4096, 16), 4);
    assertThat(pk2).hasSize(256).containsOnly(0xfedcba9876543210L);
    long[] pk3 = RefPacking.pack(RefPacking.cycle(4096, 17), 5);
    assertThat(pk3).hasSize(342);
    assertThat(pk3[0]).isEqualTo(0x05a928398a418820L);
    assertThat(pk3[1]).isEqualTo(0x031483104107b9acL);
    long[] pk5 = RefPacking.pack(RefPacking.cycle(64, 5), 3);
    assertThat(pk5)
        .containsExactly(0x08d111a223444688L, 0x111a2234446888d1L, 0x22234446888d111aL, 0x3L);
  }

  @Test
  void madeUpReportHasTheDocumentedIds() {
    List<BlockReportEntry> entries = Blocks.entries();

    assertThat(entries).hasSize(Blocks.BLOCK_COUNT);
    int total = entries.stream().mapToInt(e -> e.states().size()).sum();
    assertThat(total).isEqualTo(Blocks.STATE_COUNT);
    assertThat(Blocks.entryNamed(entries, Blocks.CHEST).states().getFirst().id()).isEqualTo(24);
    assertThat(Blocks.entryNamed(entries, Blocks.NOTE_BLOCK).states()).hasSize(1350);
    assertThat(Blocks.entryNamed(entries, Blocks.BARREL).states().getLast().id()).isEqualTo(1454);
  }

  @Test
  void miniJsonReadsObjectsArraysAndEscapes() {
    // the oracle tests read the block report with this reader
    Object parsed =
        MiniJson.parse(
            "{\"a\": [1, -2, true, null, \"x\\ny\"], \"b\": {\"c\": 1.5e1}, \"d\": {}, \"e\": []}");

    var map = MiniJson.object(parsed);
    assertThat(map.keySet()).containsExactly("a", "b", "d", "e");
    assertThat(MiniJson.array(map.get("a"))).containsExactly(1L, -2L, true, null, "x\ny");
    assertThat(MiniJson.object(map.get("b")).get("c")).isEqualTo(15.0);
    assertThat(MiniJson.object(map.get("d"))).isEmpty();
    assertThat(MiniJson.array(map.get("e"))).isEmpty();
  }

  @Test
  void deepChunkBytesHaveExactlyTheRequestedDepthForTheCoreReader() throws Exception {
    // the store tests rely on 512 levels being read and 513 being refused (core-nbt R31)
    NbtReader reader = NbtReader.of(NbtCompression.NONE);
    byte[] chunk = ChunkNbt.chunkA("bedrock");

    assertThat(reader.readNamed(NestedNbt.chunkWithDepth(chunk, 512))).isPresent();
    NbtException failure =
        Failures.expect(
            NbtException.class, () -> reader.readNamed(NestedNbt.chunkWithDepth(chunk, 513)));
    assertThat(failure.reason()).isEqualTo(NbtException.Reason.TOO_DEEP);
    NbtException giant =
        Failures.expect(
            NbtException.class,
            () -> reader.readNamed(Hex.bytes("0a 00 00 0c 00 01 61 7f ff ff ff")));
    assertThat(giant.reason()).isEqualTo(NbtException.Reason.TOO_LARGE);
  }
}
