package dev.shoko.minecraft.tools.bootstrap;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/** Downloads a file and publishes it only once its size and SHA-1 match what was expected. */
final class Downloader {

  private final HttpClient client =
      HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .connectTimeout(Duration.ofSeconds(30))
          .build();

  /**
   * Makes {@code target} hold the expected bytes. A target that already matches is left alone, so
   * repeated runs do not download again.
   *
   * @param size expected size in bytes, or a negative value when it is not known
   */
  void fetch(URI uri, Path target, String sha1, long size)
      throws IOException, InterruptedException {
    if (Files.exists(target) && mismatch(target, sha1, size) == null) {
      return;
    }
    Files.createDirectories(target.toAbsolutePath().getParent());
    Path partial = target.resolveSibling(target.getFileName() + ".part");
    try {
      HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(10)).build();
      HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(partial));
      if (response.statusCode() != 200) {
        throw new IOException("HTTP " + response.statusCode() + " for " + uri);
      }
      String problem = mismatch(partial, sha1, size);
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
  static String mismatch(Path file, String sha1, long size) throws IOException {
    long actualSize = Files.size(file);
    if (size >= 0 && actualSize != size) {
      return "size " + actualSize + " instead of " + size;
    }
    String actualSha1 = Hashes.hex(file, "SHA-1");
    if (!actualSha1.equals(sha1)) {
      return "SHA-1 " + actualSha1 + " instead of " + sha1;
    }
    return null;
  }
}
