package dev.shoko.minecraft.tools.oracle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks, before any launch, that the server jar of the cache is the one {@code tools/bootstrap}
 * verified: its size against docs/specs/reference-binaries.md (R6) and its SHA-256 against {@code
 * serverSha256} of {@code <cache>/reference/26.3/state.json}. The state file is read as plain text
 * so that the oracle does not depend on the bootstrap module.
 */
public final class ReferenceJar {

  /** Size of the official 26.3 server bundle, docs/specs/reference-binaries.md R6. */
  static final long SERVER_SIZE = 62_294_556L;

  /** A check that accepts anything, for launches that do not run the reference jar. */
  public static final ReferenceJar NONE = new ReferenceJar(null, null);

  private static final Pattern SHA256 =
      Pattern.compile("\"serverSha256\"\\s*:\\s*\"([0-9a-f]{64})\"");
  private static final Pattern VERSION = Pattern.compile("\"gameVersion\"\\s*:\\s*\"([^\"]*)\"");

  private final Path jar;
  private final Path state;

  private ReferenceJar(Path jar, Path state) {
    this.jar = jar;
    this.state = state;
  }

  /** The check of {@code <cache>/downloads/server-26.3.jar}. */
  public static ReferenceJar inCache(Path cacheDir) {
    return new ReferenceJar(
        OracleOptions.serverJar(cacheDir),
        cacheDir.resolve("reference").resolve(OracleOptions.GAME_VERSION).resolve("state.json"));
  }

  /** Path of the checked jar, or a placeholder for {@link #NONE}. */
  public String describe() {
    return jar == null ? "no reference jar check" : jar.toString();
  }

  /**
   * Fails unless the jar has the reference size and the digest verified by bootstrap.
   *
   * @throws IOException naming what differs, and pointing to bootstrap when its state is missing
   */
  public void verify() throws IOException {
    if (jar == null) {
      return;
    }
    if (!Files.isRegularFile(state)) {
      throw new IOException(
          state + " is missing: run tools/bootstrap to download and verify " + jar);
    }
    String text = Files.readString(state, StandardCharsets.UTF_8);
    Matcher version = VERSION.matcher(text);
    if (!version.find() || !version.group(1).equals(OracleOptions.GAME_VERSION)) {
      throw new IOException(state + " does not describe version " + OracleOptions.GAME_VERSION);
    }
    Matcher sha256 = SHA256.matcher(text);
    if (!sha256.find()) {
      throw new IOException(state + " has no serverSha256: run tools/bootstrap again");
    }
    if (!Files.isRegularFile(jar)) {
      throw new IOException(jar + " is missing: run tools/bootstrap first");
    }
    long size = Files.size(jar);
    if (size != SERVER_SIZE) {
      throw new IOException(jar + " has " + size + " bytes instead of " + SERVER_SIZE);
    }
    String actual = sha256(jar);
    if (!actual.equals(sha256.group(1))) {
      throw new IOException(
          jar + " has SHA-256 " + actual + " instead of " + sha256.group(1) + " (" + state + ")");
    }
  }

  private static String sha256(Path file) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required to be provided by the JDK", e);
    }
    try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
      in.transferTo(OutputStream.nullOutputStream());
    }
    return HexFormat.of().formatHex(digest.digest());
  }
}
