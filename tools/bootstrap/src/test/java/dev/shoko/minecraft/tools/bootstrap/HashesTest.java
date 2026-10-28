package dev.shoko.minecraft.tools.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HashesTest {

  private static final byte[] ABC = "abc".getBytes(StandardCharsets.UTF_8);

  @Test
  void sha1MatchesTheStandardVector() {
    assertThat(Hashes.hex(ABC, "SHA-1")).isEqualTo("a9993e364706816aba3e25717850c26c9cd0d89d");
  }

  @Test
  void sha256MatchesTheStandardVector() {
    assertThat(Hashes.hex(ABC, "SHA-256"))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
  }

  @Test
  void fileAndStreamAndBytesAgree(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("abc.bin");
    Files.write(file, ABC);
    assertThat(Hashes.hex(file, "SHA-256")).isEqualTo(Hashes.hex(ABC, "SHA-256"));
    assertThat(Hashes.hex(new ByteArrayInputStream(ABC), "SHA-256"))
        .isEqualTo(Hashes.hex(ABC, "SHA-256"));
  }
}
