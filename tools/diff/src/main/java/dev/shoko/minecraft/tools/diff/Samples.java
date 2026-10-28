package dev.shoko.minecraft.tools.diff;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The frozen sample set of {@code samples.json}: seeds and zones on which differential tests run.
 * Chosen before any code is compared, it never changes in the pull request of a fix.
 */
public record Samples(
    boolean frozen,
    String rule,
    int version,
    String units,
    List<Seed> seeds,
    List<Zone> zones,
    List<ReliefZone> reliefZones) {

  /** A world seed, kept as a decimal string in the file. */
  public record Seed(String name, String seed, String why) {
    public long value() {
      return Long.parseLong(seed);
    }
  }

  /** A rectangle of chunks, bounds included. */
  public record Zone(
      String name, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ, String why) {

    public boolean containsChunkX(int chunkX) {
      return minChunkX <= chunkX && chunkX <= maxChunkX;
    }

    public boolean containsChunkZ(int chunkZ) {
      return minChunkZ <= chunkZ && chunkZ <= maxChunkZ;
    }
  }

  /**
   * A zone chosen for its relief family. Its target biome, seed and zone stay null until a
   * dedicated pull request fills them from oracle observations.
   */
  public record ReliefZone(
      String family, String description, String targetBiome, String seed, Zone zone) {}

  public static Samples load() throws IOException {
    try (InputStream in = Samples.class.getResourceAsStream("/samples.json")) {
      if (in == null) {
        throw new IOException("samples.json is missing from the tools/diff resources");
      }
      try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
        Samples samples = new Gson().fromJson(reader, Samples.class);
        if (samples == null || !samples.frozen()) {
          throw new IOException("samples.json must be marked frozen");
        }
        return samples;
      }
    }
  }
}
