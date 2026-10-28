package dev.shoko.minecraft.tools.oracle.anvil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.tools.oracle.nbt.NbtBytes;
import dev.shoko.minecraft.tools.oracle.nbt.NbtReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RegionFileTest {

  /** Synthetic region bytes: header slots and sectors are written directly by each test. */
  private static final class RegionBytes {
    private final ByteBuffer buffer;

    RegionBytes(int sectors) {
      buffer = ByteBuffer.allocate(sectors * RegionFile.SECTOR_BYTES);
    }

    RegionBytes location(int index, int sectorOffset, int sectorCount) {
      buffer.putInt(index * 4, (sectorOffset << 8) | sectorCount);
      return this;
    }

    RegionBytes timestamp(int index, int seconds) {
      buffer.putInt(4096 + index * 4, seconds);
      return this;
    }

    /** Chunk header (declared length, compression) and payload at the start of a sector. */
    RegionBytes chunk(int sectorOffset, int declaredLength, int compression, byte[] payload) {
      int start = sectorOffset * RegionFile.SECTOR_BYTES;
      buffer.putInt(start, declaredLength);
      buffer.put(start + 4, (byte) compression);
      buffer.put(start + 5, payload);
      return this;
    }

    RegionBytes chunk(int sectorOffset, int compression, byte[] payload) {
      return chunk(sectorOffset, payload.length + 1, compression, payload);
    }

    RegionFile open(int regionX, int regionZ) throws IOException {
      return RegionFile.of(regionX, regionZ, buffer.array(), new NbtReader());
    }
  }

  private static byte[] marker(int value) {
    return NbtBytes.rootWithInt("marker", value);
  }

  @Test
  void decodesGzipZlibAndUncompressedChunks() throws IOException {
    RegionFile region =
        new RegionBytes(5)
            .location(0, 2, 1)
            .location(1, 3, 1)
            .location(32, 4, 1)
            .chunk(2, 1, NbtBytes.gzip(marker(1)))
            .chunk(3, 2, NbtBytes.zlib(marker(2)))
            .chunk(4, 3, marker(3))
            .open(0, 0);

    assertThat(region.chunk(0, 0).orElseThrow().value()).containsEntry("marker", 1);
    assertThat(region.chunk(1, 0).orElseThrow().value()).containsEntry("marker", 2);
    assertThat(region.chunk(0, 1).orElseThrow().value()).containsEntry("marker", 3);
  }

  @Test
  void reportsAbsentChunksAsEmpty() throws IOException {
    RegionFile region = new RegionBytes(2).open(0, 0);

    assertThat(region.contains(5, 7)).isFalse();
    assertThat(region.chunk(5, 7)).isEmpty();
    assertThat(region.timestamp(5, 7)).isZero();
  }

  @Test
  void indexesChunksByMaskingWithNegativeCoordinates() {
    assertThat(RegionFile.index(0, 0)).isZero();
    assertThat(RegionFile.index(31, 0)).isEqualTo(31);
    assertThat(RegionFile.index(0, 1)).isEqualTo(32);
    assertThat(RegionFile.index(-1, -1)).isEqualTo(1023);
    assertThat(RegionFile.index(-32, -32)).isZero();
    assertThat(RegionFile.index(-33, 0)).isEqualTo(31);
    assertThat(RegionFile.index(32, 33)).isEqualTo(32);
    assertThat(RegionFile.regionOf(31)).isZero();
    assertThat(RegionFile.regionOf(32)).isEqualTo(1);
    assertThat(RegionFile.regionOf(-1)).isEqualTo(-1);
    assertThat(RegionFile.regionOf(-32)).isEqualTo(-1);
    assertThat(RegionFile.regionOf(-33)).isEqualTo(-2);
    assertThat(RegionFile.fileName(-1, 0)).isEqualTo("r.-1.0.mca");
  }

  @Test
  void readsChunksOfANegativeRegionFromTheRightSlots() throws IOException {
    RegionFile region =
        new RegionBytes(4)
            // Literal slots: chunk (-1,-1) is the last of region (-1,-1), (-32,-32) the first.
            .location(1023, 2, 1)
            .location(0, 3, 1)
            .timestamp(1023, 1_700_000_000)
            .chunk(2, 3, marker(-1))
            .chunk(3, 3, marker(-32))
            .open(-1, -1);

    assertThat(region.chunk(-1, -1).orElseThrow().value()).containsEntry("marker", -1);
    assertThat(region.chunk(-32, -32).orElseThrow().value()).containsEntry("marker", -32);
    assertThat(region.chunk(-2, -1)).isEmpty();
    assertThat(region.timestamp(-1, -1)).isEqualTo(1_700_000_000);
  }

  @Test
  void refusesAChunkOfAnotherRegion() throws IOException {
    RegionFile region = new RegionBytes(2).open(-1, 0);

    assertThatThrownBy(() -> region.chunk(31, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not in region (-1,0)");
    assertThatThrownBy(() -> region.chunk(-33, 0)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void refusesAnOffsetOutsideTheFile() throws IOException {
    RegionFile region = new RegionBytes(3).location(2 + 3 * 32, 40, 1).open(0, 0);

    assertThatThrownBy(() -> region.chunk(2, 3))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("chunk (2,3)")
        .hasMessageContaining("outside the file");
  }

  @Test
  void refusesAnOffsetInsideTheHeader() throws IOException {
    RegionFile region = new RegionBytes(3).location(0, 1, 1).open(0, 0);

    assertThatThrownBy(() -> region.chunk(0, 0)).hasMessageContaining("invalid location");
  }

  @Test
  void refusesALengthLargerThanTheAllocatedSectors() throws IOException {
    RegionFile region =
        new RegionBytes(4).location(0, 2, 1).chunk(2, 5000, 3, marker(1)).open(0, 0);

    assertThatThrownBy(() -> region.chunk(0, 0)).hasMessageContaining("does not fit in its 1");
  }

  @Test
  void refusesALengthThatRunsPastTheEndOfTheFile() throws IOException {
    RegionFile region =
        new RegionBytes(3).location(0, 2, 2).chunk(2, 5000, 3, marker(1)).open(0, 0);

    assertThatThrownBy(() -> region.chunk(0, 0)).hasMessageContaining("past the end of the file");
  }

  @Test
  void refusesAZeroLength() throws IOException {
    RegionFile region = new RegionBytes(3).location(0, 2, 1).chunk(2, 0, 3, new byte[0]).open(0, 0);

    assertThatThrownBy(() -> region.chunk(0, 0)).hasMessageContaining("invalid chunk length 0");
  }

  @Test
  void refusesLz4ExternalAndUnknownCompressionExplicitly() throws IOException {
    RegionFile region =
        new RegionBytes(5)
            .location(0, 2, 1)
            .location(1, 3, 1)
            .location(2, 4, 1)
            .chunk(2, 4, marker(1))
            .chunk(3, 0x82, marker(1))
            .chunk(4, 9, marker(1))
            .open(0, 0);

    assertThatThrownBy(() -> region.chunk(0, 0)).hasMessageContaining("LZ4");
    assertThatThrownBy(() -> region.chunk(1, 0)).hasMessageContaining("external");
    assertThatThrownBy(() -> region.chunk(2, 0)).hasMessageContaining("unknown compression type 9");
  }

  @Test
  void namesTheChunkWhenItsPayloadIsCorrupt() throws IOException {
    byte[] whole = NbtBytes.zlib(marker(1));
    byte[] corrupt = Arrays.copyOf(whole, whole.length / 2);
    RegionFile region = new RegionBytes(3).location(0, 2, 1).chunk(2, 2, corrupt).open(0, 0);

    assertThatThrownBy(() -> region.chunk(0, 0))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("chunk (0,0)");
  }

  @Test
  void readsCoordinatesFromTheFileName(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("r.-2.3.mca");
    Files.write(file, new byte[2 * RegionFile.SECTOR_BYTES]);

    RegionFile region = RegionFile.read(file);

    assertThat(region.regionX()).isEqualTo(-2);
    assertThat(region.regionZ()).isEqualTo(3);
  }

  @Test
  void refusesAWrongFileNameAndATruncatedHeader(@TempDir Path dir) throws IOException {
    Path badName = dir.resolve("region.mca");
    Files.write(badName, new byte[2 * RegionFile.SECTOR_BYTES]);
    Path shortFile = dir.resolve("r.0.0.mca");
    Files.write(shortFile, new byte[100]);

    assertThatThrownBy(() -> RegionFile.read(badName)).hasMessageContaining("not a region file");
    assertThatThrownBy(() -> RegionFile.read(shortFile)).hasMessageContaining("shorter than");
  }

  private static byte[] positioned(int chunkX, int chunkZ) {
    return new NbtBytes()
        .named(10, "")
        .named(3, "xPos")
        .i32(chunkX)
        .named(3, "zPos")
        .i32(chunkZ)
        .end()
        .toByteArray();
  }

  @Test
  void refusesSectorsSharedByTwoChunks() throws IOException {
    RegionFile region =
        new RegionBytes(5)
            .location(0, 2, 2)
            .location(33, 3, 1)
            .chunk(2, 3, marker(1))
            .chunk(3, 3, marker(2))
            .open(0, 0);

    assertThatThrownBy(() -> region.chunk(0, 0))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("chunk (0,0)")
        .hasMessageContaining("sectors 2..3 overlap those of chunk (1,1)");
    assertThatThrownBy(() -> region.chunk(1, 1))
        .hasMessageContaining("overlap those of chunk (0,0)");
  }

  @Test
  void acceptsAdjacentChunksThatDoNotOverlap() throws IOException {
    RegionFile region =
        new RegionBytes(5)
            .location(0, 2, 2)
            .location(33, 4, 1)
            .chunk(2, 3, marker(1))
            .chunk(4, 3, marker(2))
            .open(0, 0);

    assertThat(region.chunk(0, 0)).isPresent();
    assertThat(region.chunk(1, 1)).isPresent();
  }

  @Test
  void readsAChunkSpanningSeveralSectors() throws IOException {
    byte[] large =
        new NbtBytes()
            .named(10, "")
            .named(7, "data")
            .i32(9000)
            .raw(new int[9000])
            .named(3, "marker")
            .i32(77)
            .end()
            .toByteArray();
    RegionFile region = new RegionBytes(5).location(0, 2, 3).chunk(2, 3, large).open(0, 0);

    assertThat(large.length).isGreaterThan(2 * RegionFile.SECTOR_BYTES);
    assertThat(region.chunk(0, 0).orElseThrow().value()).containsEntry("marker", 77);
  }

  @Test
  void exposesTimestampsAsUnsignedSeconds() throws IOException {
    RegionFile region = new RegionBytes(2).timestamp(0, -1).open(0, 0);

    assertThat(region.timestamp(0, 0)).isEqualTo(4_294_967_295L);
  }

  @Test
  void checksTheChunkPositionWrittenInItsNbt() throws IOException {
    RegionFile region =
        new RegionBytes(4)
            .location(1023, 2, 1)
            .location(0, 3, 1)
            .chunk(2, 3, positioned(-1, -1))
            .chunk(3, 3, positioned(-1, -1))
            .open(-1, -1);

    assertThat(region.chunkCheckingPosition(-1, -1).orElseThrow().value())
        .containsEntry("xPos", -1);
    assertThatThrownBy(() -> region.chunkCheckingPosition(-32, -32))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("chunk (-32,-32): its NBT says xPos=-1 zPos=-1");
    assertThat(region.chunkCheckingPosition(-2, -1)).isEmpty();
  }
}
