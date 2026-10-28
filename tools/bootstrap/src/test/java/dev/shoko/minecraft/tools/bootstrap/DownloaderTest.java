package dev.shoko.minecraft.tools.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DownloaderTest {

  private static final byte[] BODY = "reference bytes".getBytes(StandardCharsets.UTF_8);
  private static final String BODY_SHA1 = Hashes.hex(BODY, "SHA-1");

  private final AtomicInteger requests = new AtomicInteger();
  private HttpServer server;
  private URI file;
  private URI missing;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/file",
        exchange -> {
          requests.incrementAndGet();
          exchange.sendResponseHeaders(200, BODY.length);
          exchange.getResponseBody().write(BODY);
          exchange.close();
        });
    server.createContext(
        "/missing",
        exchange -> {
          exchange.sendResponseHeaders(404, -1);
          exchange.close();
        });
    server.start();
    String base = "http://127.0.0.1:" + server.getAddress().getPort();
    file = URI.create(base + "/file");
    missing = URI.create(base + "/missing");
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void publishesAFileThatMatches(@TempDir Path dir) throws Exception {
    Path target = dir.resolve("sub").resolve("file.bin");

    new Downloader().fetch(file, target, BODY_SHA1, BODY.length);

    assertThat(target).hasBinaryContent(BODY);
    assertThat(dir.resolve("sub")).isDirectoryContaining(path -> path.equals(target));
  }

  @Test
  void rejectsAWrongDigestAndLeavesNothingBehind(@TempDir Path dir) {
    Path target = dir.resolve("file.bin");

    assertThatThrownBy(() -> new Downloader().fetch(file, target, "0".repeat(40), BODY.length))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("SHA-1");

    assertThat(dir).isEmptyDirectory();
  }

  @Test
  void rejectsAWrongSize(@TempDir Path dir) {
    Path target = dir.resolve("file.bin");

    assertThatThrownBy(() -> new Downloader().fetch(file, target, BODY_SHA1, BODY.length + 1))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("size");
    assertThat(dir).isEmptyDirectory();
  }

  @Test
  void rejectsAnErrorStatus(@TempDir Path dir) {
    assertThatThrownBy(
            () -> new Downloader().fetch(missing, dir.resolve("x.bin"), BODY_SHA1, BODY.length))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("404");
    assertThat(dir).isEmptyDirectory();
  }

  @Test
  void doesNotDownloadAgainWhenTheTargetAlreadyMatches(@TempDir Path dir) throws Exception {
    Path target = dir.resolve("file.bin");
    Downloader downloader = new Downloader();

    downloader.fetch(file, target, BODY_SHA1, -1);
    downloader.fetch(file, target, BODY_SHA1, -1);

    assertThat(requests).hasValue(1);
  }

  @Test
  void replacesACorruptedTarget(@TempDir Path dir) throws Exception {
    Path target = dir.resolve("file.bin");
    Files.writeString(target, "corrupted");

    new Downloader().fetch(file, target, BODY_SHA1, BODY.length);

    assertThat(target).hasBinaryContent(BODY);
  }
}
