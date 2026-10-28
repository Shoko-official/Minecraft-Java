package dev.shoko.minecraft.tools.bootstrap;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Creates real links for tests, skipping the test where the platform or account cannot. */
final class TestLinks {

  static final boolean WINDOWS =
      System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");

  private TestLinks() {}

  /** Creates an NTFS junction with {@code mklink /J}. */
  static void junction(Path link, Path target) throws IOException, InterruptedException {
    assumeTrue(WINDOWS, "NTFS junctions exist only on Windows");
    Process process =
        new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), target.toString())
            .redirectErrorStream(true)
            .start();
    process.getInputStream().readAllBytes();
    assumeTrue(process.waitFor() == 0, "mklink /J failed");
  }

  static void symlink(Path link, Path target) {
    try {
      Files.createSymbolicLink(link, target);
    } catch (IOException | UnsupportedOperationException e) {
      assumeTrue(false, "symbolic links unavailable: " + e);
    }
  }

  /** A junction on Windows, where symbolic links need a privilege, a symbolic link elsewhere. */
  static void directoryLink(Path link, Path target) throws IOException, InterruptedException {
    if (WINDOWS) {
      junction(link, target);
    } else {
      symlink(link, target);
    }
  }
}
