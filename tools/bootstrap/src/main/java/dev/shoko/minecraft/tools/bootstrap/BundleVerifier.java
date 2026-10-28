package dev.shoko.minecraft.tools.bootstrap;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Checks the contents of a server bundle whose own digests were already verified: its identity file
 * and the nested server archive must match {@code pins.json}.
 */
final class BundleVerifier {

  /** The server archive nested in the bundle, as listed by the bundle itself. */
  record InnerJar(ZipEntry entry, String sha256) {}

  private static final String IDENTITY = "Bundle version.json";

  private final Pins pins;

  BundleVerifier(Pins pins) {
    this.pins = pins;
  }

  /**
   * Checks the identity file of the bundle against the pinned versions.
   *
   * @return the raw identity file, to be kept as a copy
   */
  byte[] checkIdentity(ZipFile bundle) throws IOException {
    byte[] bytes;
    try (InputStream in = bundle.getInputStream(requireEntry(bundle, "version.json"))) {
      bytes = in.readAllBytes();
    }
    JsonObject identity =
        JsonFields.parseObject(new String(bytes, StandardCharsets.UTF_8), IDENTITY);
    JsonObject pack = JsonFields.object(identity, "pack_version", IDENTITY);
    String dataPack =
        JsonFields.integer(pack, "data_major", IDENTITY)
            + "."
            + JsonFields.integer(pack, "data_minor", IDENTITY);
    String resourcePack =
        JsonFields.integer(pack, "resource_major", IDENTITY)
            + "."
            + JsonFields.integer(pack, "resource_minor", IDENTITY);
    if (!JsonFields.string(identity, "id", IDENTITY).equals(pins.gameVersion())
        || JsonFields.integer(identity, "world_version", IDENTITY) != pins.worldVersion()
        || JsonFields.integer(identity, "protocol_version", IDENTITY) != pins.protocolVersion()
        || !dataPack.equals(pins.packVersion().data())
        || !resourcePack.equals(pins.packVersion().resource())) {
      throw new IOException("Bundle identity differs from pins.json: " + identity);
    }
    return bytes;
  }

  /** Verifies the inner server archive listed by the bundle against the list and the pin. */
  InnerJar checkInnerJar(ZipFile bundle) throws IOException {
    String content;
    try (InputStream in = bundle.getInputStream(requireEntry(bundle, "META-INF/versions.list"))) {
      content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    for (String line : (Iterable<String>) content.lines()::iterator) {
      String[] fields = line.split("\t", -1);
      if (fields.length == 3 && fields[1].equals(pins.gameVersion())) {
        ZipEntry entry = requireEntry(bundle, "META-INF/versions/" + fields[2]);
        String actual;
        try (InputStream in = bundle.getInputStream(entry)) {
          actual = Hashes.hex(in, "SHA-256");
        }
        if (!actual.equals(fields[0]) || !actual.equals(pins.innerServerSha256())) {
          throw new IOException(
              "Inner server archive SHA-256 " + actual + " differs from the list or the pin");
        }
        return new InnerJar(entry, actual);
      }
    }
    throw new IOException("versions.list has no entry for " + pins.gameVersion());
  }

  private static ZipEntry requireEntry(ZipFile bundle, String name) throws IOException {
    ZipEntry entry = bundle.getEntry(name);
    if (entry == null) {
      throw new IOException("Bundle has no entry " + name);
    }
    return entry;
  }
}
