package dev.shoko.minecraft.world.support;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.RegionPos;
import dev.shoko.minecraft.world.storage.RegionFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Region files of vector RG1 laid out in a folder, and small file helpers. */
public final class RegionFixtures {

  public static final RegionPos REGION = new RegionPos(-1, -1);
  public static final ChunkPos CHUNK_A = new ChunkPos(-1, -1);
  public static final ChunkPos CHUNK_B = new ChunkPos(-32, -32);
  public static final int INDEX_A = 1023;
  public static final int INDEX_B = 0;

  private RegionFixtures() {}

  public static Path regionFile(Path folder) {
    return folder.resolve("r.-1.-1.mca");
  }

  public static byte[] nbtOfA() {
    return ChunkNbt.chunkA("bedrock");
  }

  public static byte[] nbtOfB() {
    return ChunkNbt.bytes(ChunkNbt.chunk(-32, -32, false, "bedrock"));
  }

  /** Writes the vector RG1 file with chunk A compressed by this type. */
  public static byte[] writeRg1(Path folder, int typeOfA) {
    byte[] file = Regions.rg1(typeOfA, nbtOfA(), nbtOfB());
    write(regionFile(folder), file);
    return file;
  }

  /**
   * Writes the file of a variant of RG1 to RG4 into the folder, the way the reference tool builds
   * it: chunk A stored with zlib unless the variant says otherwise, chunk B raw. Returns the file.
   */
  public static byte[] writeVariant(Path folder, String variant) {
    byte[] nbt = ChunkNbt.chunkA(variant);
    int type = 2;
    byte[] payload = Regions.zlib(nbt);
    switch (variant) {
      case "corrupt_zlib" -> payload[payload.length / 2] ^= (byte) 0xFF;
      case "compression_127" -> {
        type = 127;
        payload = Hex.concat(Hex.bytes("00 08 78 3a 63 75 73 74 6f 6d"), payload);
      }
      case "external", "missing_mcc" -> {
        if (variant.equals("external")) {
          write(folder.resolve("c.-1.-1.mcc"), payload);
        }
        type = 2 + 128;
        payload = new byte[0];
      }
      default -> {}
    }
    byte[] file =
        Regions.file(
            java.util.List.of(
                new Regions.Blob(INDEX_A, type, payload, Regions.TIMESTAMP_A),
                new Regions.Blob(INDEX_B, 3, nbtOfB(), Regions.TIMESTAMP_B)));
    if (variant.equals("bad_offset")) {
      Regions.putInt(file, 4 * INDEX_A, (1 << 8) | 1);
    } else if (variant.equals("offset_beyond")) {
      Regions.putInt(file, 4 * INDEX_A, (200 << 8) | 1);
    }
    write(regionFile(folder), file);
    return file;
  }

  public static void write(Path path, byte[] data) {
    try {
      Files.createDirectories(path.getParent());
      Files.write(path, data);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static byte[] read(Path path) {
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static RegionFile open(Path folder) throws IOException {
    return RegionFile.open(folder, REGION);
  }

  /** Names of the files in a folder, sorted. */
  public static java.util.List<String> names(Path folder) {
    try (Stream<Path> files = Files.list(folder)) {
      return files.map(p -> p.getFileName().toString()).sorted().toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The chunk of region (-1, -1) that has this header index. */
  public static ChunkPos chunkAtIndex(int index) {
    return new ChunkPos(-32 + index % 32, -32 + index / 32);
  }
}
