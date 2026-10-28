package dev.shoko.minecraft.tools.bootstrap;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Expected identity of the reference game, versioned in {@code pins.json}. Every downloaded or
 * extracted artifact is checked against these values, so a changed upstream file fails loudly.
 */
record Pins(
    String gameVersion,
    VersionJson versionJson,
    Server server,
    String innerServerSha256,
    int worldVersion,
    int protocolVersion,
    PackVersion packVersion) {

  /** Location and SHA-1 of the official version metadata. */
  record VersionJson(String url, String sha1) {}

  /** Size and digests of the official server bundle. */
  record Server(String sha1, String sha256, long size) {}

  /** Data and resource pack versions, as {@code major.minor}. */
  record PackVersion(String data, String resource) {}

  static Pins load() throws IOException {
    try (InputStream in = Pins.class.getResourceAsStream("/pins.json")) {
      if (in == null) {
        throw new IOException("pins.json is missing from the bootstrap resources");
      }
      try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
        return new Gson().fromJson(reader, Pins.class);
      }
    }
  }
}
