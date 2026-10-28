package dev.shoko.minecraft.tools.bootstrap;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;

/** Downloads a file and publishes it only once its size and digests match what was expected. */
final class Downloader {

  /** A digest the file must have, as lower-case hexadecimal. */
  record Digest(String algorithm, String hex) {}

  /**
   * What the file must be.
   *
   * @param size expected size in bytes, or a negative value when it is not known
   */
  record Expected(long size, List<Digest> digests) {

    static Expected sha1(String sha1) {
      return new Expected(-1, List.of(new Digest("SHA-1", sha1)));
    }
  }

  private final HttpClient client =
      HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .connectTimeout(Duration.ofSeconds(30))
          .build();

  /**
   * Makes {@code target} hold the expected bytes. A target that already matches is left alone, so
   * repeated runs do not download again. A download that does not match is never published.
   */
  void fetch(URI uri, Path target, Expected expected) throws IOException, InterruptedException {
    if (Files.exists(target) && mismatch(target, expected) == null) {
      return;
    }
    Files.createDirectories(target.toAbsolutePath().getParent());
    Path partial = target.resolveSibling(target.getFileName() + ".part");
    try {
      HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(10)).build();
      // A leftover partial file from a killed run must not survive under the new bytes.
      HttpResponse<Path> response =
          client.send(
              request,
              HttpResponse.BodyHandlers.ofFile(
                  partial,
                  StandardOpenOption.CREATE,
                  StandardOpenOption.WRITE,
                  StandardOpenOption.TRUNCATE_EXISTING));
      if (response.statusCode() != 200) {
        throw new IOException("HTTP " + response.statusCode() + " for " + uri);
      }
      String problem = mismatch(partial, expected);
      if (problem != null) {
        throw new IOException("Downloaded file from " + uri + " is rejected: " + problem);
      }
      Files.move(
          partial, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(partial);
    }
  }

  /**
   * Returns a description of the first difference with the expectation, or null when it matches.
   */
  static String mismatch(Path file, Expected expected) throws IOException {
    long actualSize = Files.size(file);
    if (expected.size() >= 0 && actualSize != expected.size()) {
      return "size " + actualSize + " instead of " + expected.size();
    }
    for (Digest digest : expected.digests()) {
      String actual = Hashes.hex(file, digest.algorithm());
      if (!actual.equals(digest.hex())) {
        return digest.algorithm() + " " + actual + " instead of " + digest.hex();
      }
    }
    return null;
  }
}
