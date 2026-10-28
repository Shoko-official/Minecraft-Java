package dev.shoko.minecraft.tools.oracle;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class ReferenceJarTest {

  private static Path jar(Path cache) {
    return OracleOptions.serverJar(cache);
  }

  private static void writeState(Path cache, String version, String sha256) throws IOException {
    Path state = cache.resolve("reference").resolve("26.3").resolve("state.json");
    Files.createDirectories(state.getParent());
    Files.writeString(
        state,
        "{\n  \"gameVersion\": \""
            + version
            + "\",\n  \"serverSha256\": \""
            + sha256
            + "\",\n  \"resourceFiles\": 1\n}\n");
  }

  /** A jar of the reference size, all zeros, and its SHA-256. */
  private static String writeJarOfTheReferenceSize(Path cache) throws Exception {
    Files.createDirectories(jar(cache).getParent());
    try (RandomAccessFile file = new RandomAccessFile(jar(cache).toFile(), "rw")) {
      file.setLength(ReferenceJar.SERVER_SIZE);
    }
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(jar(cache))));
  }

  @Test
  void refusesWithoutTheBootstrapStateAndPointsToBootstrap(@TempDir Path cache) {
    assertThatThrownBy(() -> ReferenceJar.inCache(cache).verify())
        .isInstanceOf(IOException.class)
        .hasMessageContaining("state.json is missing: run tools/bootstrap")
        .hasMessageContaining("server-26.3.jar");
  }

  @Test
  void refusesAStateOfAnotherVersion(@TempDir Path cache) throws IOException {
    writeState(cache, "26.2", "a".repeat(64));

    assertThatThrownBy(() -> ReferenceJar.inCache(cache).verify())
        .hasMessageContaining("does not describe version 26.3");
  }

  @Test
  void refusesAJarOfTheWrongSize(@TempDir Path cache) throws IOException {
    writeState(cache, "26.3", "a".repeat(64));
    Files.createDirectories(jar(cache).getParent());
    Files.write(jar(cache), new byte[10]);

    assertThatThrownBy(() -> ReferenceJar.inCache(cache).verify())
        .hasMessageContaining("has 10 bytes instead of 62294556");
  }

  @Test
  @Timeout(30)
  void comparesTheJarWithTheVerifiedDigest(@TempDir Path cache) throws Exception {
    String digest = writeJarOfTheReferenceSize(cache);
    writeState(cache, "26.3", "0".repeat(64));

    assertThatThrownBy(() -> ReferenceJar.inCache(cache).verify())
        .hasMessageContaining("has SHA-256 " + digest + " instead of " + "0".repeat(64));

    writeState(cache, "26.3", digest);
    assertThatCode(() -> ReferenceJar.inCache(cache).verify()).doesNotThrowAnyException();
  }

  @Test
  void noneChecksNothing() {
    assertThatCode(ReferenceJar.NONE::verify).doesNotThrowAnyException();
  }
}
