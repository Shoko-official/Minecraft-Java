package dev.shoko.minecraft.world.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.support.Chunks;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Bug-hunter probes: a small compressed record whose NBT has a huge number of tiny tags. The
 * decompressed size stays under the limit of AR-R29, so only the NBT quota can refuse it.
 */
class NbtAmplificationHuntTest {

  @TempDir Path base;

  private static byte[] rootWithList(int elementType, int count, int bytesPerElement)
      throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataOutputStream data = new DataOutputStream(out);
    data.writeByte(10);
    data.writeShort(0);
    data.writeByte(9);
    data.writeShort(1);
    data.writeByte('x');
    data.writeByte(elementType);
    data.writeInt(count);
    byte[] zeros = new byte[8192];
    long remaining = (long) count * bytesPerElement;
    while (remaining > 0) {
      int chunk = (int) Math.min(zeros.length, remaining);
      data.write(zeros, 0, chunk);
      remaining -= chunk;
    }
    data.writeByte(0);
    return out.toByteArray();
  }

  private void attack(int elementType, int count, int bytesPerElement) throws Exception {
    byte[] nbt = rootWithList(elementType, count, bytesPerElement);
    ChunkPos pos = new ChunkPos(0, 0);
    try (RegionFile file = RegionFile.open(base, pos.region())) {
      file.write(pos, nbt, RegionCompression.ZLIB, 1L);
    }
    long max = Runtime.getRuntime().maxMemory();
    try (AnvilChunkStore store =
        AnvilChunkStore.open(base, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB)) {
      ReadResult<Chunk> result;
      try {
        result = store.load(pos);
      } catch (OutOfMemoryError e) {
        throw new AssertionError(
            "OutOfMemoryError with a "
                + nbt.length / 1_000_000
                + " MB NBT of "
                + count
                + " tags in a JVM of "
                + max / 1_000_000
                + " MB",
            e);
      }
      assertThat(result).isNotInstanceOf(ReadResult.Present.class);
    }
  }

  @Test
  void twentyMillionEmptyCompoundsInOneListAreRefusedNotAnOutOfMemory() throws Exception {
    attack(10, 20_000_000, 1);
  }

  @Test
  void fiftyMillionBytesInOneListAreRefusedNotAnOutOfMemory() throws Exception {
    attack(1, 50_000_000, 1);
  }

  @Test
  void tenMillionEmptyListsInOneListAreRefusedNotAnOutOfMemory() throws Exception {
    attack(9, 10_000_000, 5);
  }
}
