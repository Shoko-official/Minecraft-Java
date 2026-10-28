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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DownloaderTest {

  private static final byte[] BODY = "reference bytes".getBytes(StandardCharsets.UTF_8);
  private static final String BODY_SHA1 = Hashes.hex(BODY, "SHA-1");
  private static final String BODY_SHA256 = Hashes.hex(BODY, "SHA-256");

  private final AtomicInteger requests = new AtomicInteger();
  private HttpServer server;
  private URI file;
  private URI missing;
  private URI redirect;
  private URI truncated;
  private URI chunked;

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
    server.createContext(
        "/redirect",
        exchange -> {
          exchange.getResponseHeaders().add("Location", "/file");
          exchange.sendResponseHeaders(302, -1);
          exchange.close();
        });
    server.createContext(
        "/truncated",
        exchange -> {
          // Announces 25 bytes, sends 15, then closes the connection.
          exchange.sendResponseHeaders(200, 25);
          try {
            exchange.getResponseBody().write(BODY, 0, 15);
            exchange.close();
          } catch (IOException expected) {
            // The server refuses to close a short fixed-length body cleanly; the connection drops.
          }
        });
    server.createContext(
        "/chunked",
        exchange -> {
          // A length of 0 makes the server stream the body without Content-Length.
          exchange.sendResponseHeaders(200, 0);
          exchange.getResponseBody().write(BODY);
          exchange.close();
        });
    server.start();
    String base = "http://127.0.0.1:" + server.getAddress().getPort();
    file = URI.create(base + "/file");
    missing = URI.create(base + "/missing");
    redirect = URI.create(base + "/redirect");
    truncated = URI.create(base + "/truncated");
    chunked = URI.create(base + "/chunked");
  }

  private static Downloader.Expected expected(String sha1, long size) {
    return new Downloader.Expected(size, List.of(new Downloader.Digest("SHA-1", sha1)));
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void publishesAFileThatMatches(@TempDir Path dir) throws Exception {
    Path target = dir.resolve("sub").resolve("file.bin");

    new Downloader().fetch(file, target, expected(BODY_SHA1, BODY.length));

    assertThat(target).hasBinaryContent(BODY);
    assertThat(dir.resolve("sub")).isDirectoryContaining(path -> path.equals(target));
  }

  @Test
  void rejectsAWrongDigestAndLeavesNothingBehind(@TempDir Path dir) {
    Path target = dir.resolve("file.bin");

    assertThatThrownBy(
            () -> new Downloader().fetch(file, target, expected("0".repeat(40), BODY.length)))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("SHA-1");

    assertThat(dir).isEmptyDirectory();
  }

  @Test
  void rejectsAWrongSize(@TempDir Path dir) {
    Path target = dir.resolve("file.bin");

    assertThatThrownBy(
            () -> new Downloader().fetch(file, target, expected(BODY_SHA1, BODY.length + 1)))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("size");
    assertThat(dir).isEmptyDirectory();
  }

  @Test
  void rejectsAnErrorStatus(@TempDir Path dir) {
    assertThatThrownBy(
            () ->
                new Downloader()
                    .fetch(missing, dir.resolve("x.bin"), expected(BODY_SHA1, BODY.length)))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("404");
    assertThat(dir).isEmptyDirectory();
  }

  @Test
  void doesNotDownloadAgainWhenTheTargetAlreadyMatches(@TempDir Path dir) throws Exception {
    Path target = dir.resolve("file.bin");
    Downloader downloader = new Downloader();

    downloader.fetch(file, target, Downloader.Expected.sha1(BODY_SHA1));
    downloader.fetch(file, target, Downloader.Expected.sha1(BODY_SHA1));

    assertThat(requests).hasValue(1);
  }

  @Test
  void replacesACorruptedTarget(@TempDir Path dir) throws Exception {
    Path target = dir.resolve("file.bin");
    Files.writeString(target, "corrupted");

    new Downloader().fetch(file, target, expected(BODY_SHA1, BODY.length));

    assertThat(target).hasBinaryContent(BODY);
  }

  @Test
  void rejectsAWrongSha256BeforePublishing(@TempDir Path dir) {
    Path target = dir.resolve("file.bin");
    Downloader.Expected expected =
        new Downloader.Expected(
            BODY.length,
            List.of(
                new Downloader.Digest("SHA-1", BODY_SHA1),
                new Downloader.Digest("SHA-256", "0".repeat(64))));

    assertThatThrownBy(() -> new Downloader().fetch(file, target, expected))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("SHA-256");
    assertThat(dir).isEmptyDirectory();
  }

  @Test
  void publishesWhenEveryDigestMatches(@TempDir Path dir) throws Exception {
    Path target = dir.resolve("file.bin");
    Downloader.Expected expected =
        new Downloader.Expected(
            BODY.length,
            List.of(
                new Downloader.Digest("SHA-1", BODY_SHA1),
                new Downloader.Digest("SHA-256", BODY_SHA256)));

    new Downloader().fetch(file, target, expected);

    assertThat(target).hasBinaryContent(BODY);
  }

  @Test
  void overwritesALongerLeftoverPartialFile(@TempDir Path dir) throws Exception {
    Path target = dir.resolve("file.bin");
    Path partial = dir.resolve("file.bin.part");
    Files.writeString(partial, "leftover of a killed run, longer than the body");

    new Downloader().fetch(file, target, Downloader.Expected.sha1(BODY_SHA1));

    assertThat(target).hasBinaryContent(BODY);
    assertThat(partial).doesNotExist();
  }

  @Test
  void followsARedirect(@TempDir Path dir) throws Exception {
    Path target = dir.resolve("file.bin");

    new Downloader().fetch(redirect, target, expected(BODY_SHA1, BODY.length));

    assertThat(target).hasBinaryContent(BODY);
    assertThat(requests).hasValue(1);
  }

  @Test
  void rejectsAResponseShorterThanItsContentLength(@TempDir Path dir) {
    Path target = dir.resolve("file.bin");

    assertThatThrownBy(
            () -> new Downloader().fetch(truncated, target, Downloader.Expected.sha1(BODY_SHA1)))
        .isInstanceOf(IOException.class);
    assertThat(dir).isEmptyDirectory();
  }

  @Test
  void acceptsAResponseWithoutContentLength(@TempDir Path dir) throws Exception {
    Path target = dir.resolve("file.bin");

    new Downloader().fetch(chunked, target, expected(BODY_SHA1, BODY.length));

    assertThat(target).hasBinaryContent(BODY);
  }
}
