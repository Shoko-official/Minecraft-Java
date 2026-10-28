package dev.shoko.minecraft.tools.diff;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the frozen sample set. A failing digest means that samples.json changed: that is only
 * legitimate in a dedicated pull request, which updates {@link #FROZEN_SHA256} with a reason.
 */
class SamplesTest {

  /** SHA-256 of samples.json with line endings normalized to LF. */
  private static final String FROZEN_SHA256 =
      "efc6fef2a8cd0d03f425c4fd526d5fb58769ac559267393cb7231b310cea1187";

  private static String rawText() throws IOException {
    try (InputStream in = SamplesTest.class.getResourceAsStream("/samples.json")) {
      assertThat(in).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
  }

  @Test
  void isFrozenAndStatesItsModificationRule() throws IOException {
    JsonObject root = JsonParser.parseString(rawText()).getAsJsonObject();

    assertThat(root.keySet())
        .containsExactly("frozen", "rule", "version", "units", "seeds", "zones", "reliefZones");
    assertThat(root.get("frozen").getAsBoolean()).isTrue();
    assertThat(root.get("rule").getAsString())
        .contains("dedicated pull request")
        .contains("never made by the agent that has just met a failure");
  }

  @Test
  void contentMatchesTheFrozenDigest() throws IOException, NoSuchAlgorithmException {
    byte[] bytes = rawText().getBytes(StandardCharsets.UTF_8);
    String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));

    assertThat(digest).isEqualTo(FROZEN_SHA256);
  }

  @Test
  void seedsCoverTheRequiredEdgeCases() throws IOException {
    List<Samples.Seed> seeds = Samples.load().seeds();
    List<Long> values = seeds.stream().map(Samples.Seed::value).toList();

    assertThat(values).doesNotHaveDuplicates().contains(0L, 1L, -1L, Long.MIN_VALUE);
    assertThat(values).anyMatch(v -> v < -1_000_000_000_000L && v != Long.MIN_VALUE);
    assertThat(values).anyMatch(v -> v > 1 && v < 1_000_000_000L);
    assertThat(seeds).extracting(Samples.Seed::name).doesNotHaveDuplicates();
    assertThat(seeds).allMatch(s -> !s.why().isBlank());
  }

  @Test
  void zonesCoverNegativeCoordinatesAndRegionBorders() throws IOException {
    List<Samples.Zone> zones = Samples.load().zones();

    assertThat(zones).extracting(Samples.Zone::name).doesNotHaveDuplicates();
    assertThat(zones)
        .allMatch(z -> z.minChunkX() <= z.maxChunkX() && z.minChunkZ() <= z.maxChunkZ());
    assertThat(zones).anyMatch(z -> z.maxChunkX() < 0 && z.maxChunkZ() < 0);
    for (int[] border : new int[][] {{-1, 0}, {31, 32}, {-33, -32}}) {
      assertThat(zones)
          .as("border %d/%d", border[0], border[1])
          .anyMatch(z -> z.containsChunkX(border[0]) && z.containsChunkX(border[1]))
          .anyMatch(z -> z.containsChunkZ(border[0]) && z.containsChunkZ(border[1]));
    }
  }

  @Test
  void reliefZonesNameAFamilyAndLeaveUnknownsEmpty() throws IOException {
    List<Samples.ReliefZone> relief = Samples.load().reliefZones();

    assertThat(relief).hasSizeGreaterThanOrEqualTo(8);
    assertThat(relief).extracting(Samples.ReliefZone::family).doesNotHaveDuplicates();
    for (Samples.ReliefZone zone : relief) {
      assertThat(zone.description()).isNotBlank();
      if (zone.targetBiome() != null) {
        assertThat(zone.targetBiome()).matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
      }
      if (zone.seed() != null) {
        assertThat(zone.seed()).matches("-?[0-9]+");
      }
      if (zone.zone() != null) {
        assertThat(zone.zone().minChunkX()).isLessThanOrEqualTo(zone.zone().maxChunkX());
        assertThat(zone.zone().minChunkZ()).isLessThanOrEqualTo(zone.zone().maxChunkZ());
      }
    }
  }
}
