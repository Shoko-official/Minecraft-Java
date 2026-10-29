package dev.shoko.minecraft.data.hunt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import dev.shoko.minecraft.data.pack.PackFiles;
import dev.shoko.minecraft.data.testing.Packs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// spec: PackFiles.directory Javadoc (symbolic links are not followed) and the guarantee of
// DirectoryFiles: a path cannot reach out of the pack folder. A Windows junction is not a symbolic
// link for the JDK, so it needs its own test.
class PackLinksHuntTest {

  private static final String OK = "data/sonde/loot_table/ok.json";
  private static final String STOLEN = "data/sonde/loot_table/stolen.json";

  @TempDir Path tmp;
  private Path pack;
  private Path outside;

  @BeforeEach
  void createPackAndOutsideFolder() throws IOException {
    pack = Files.createDirectory(tmp.resolve("pack"));
    Packs.writeTree(pack, Packs.tree("pack.mcmeta", Packs.MCMETA, OK, "{}"));
    outside = Files.createDirectory(tmp.resolve("outside"));
    Packs.writeTree(outside, Packs.tree(STOLEN, "{\"stolen\":true}", "outside.json", "{}"));
  }

  private void assertNothingOutsideIsReachable(String linked) throws IOException {
    PackFiles files = PackFiles.directory(pack);
    assertThat(files.paths()).containsExactly(OK, "pack.mcmeta");
    assertThatThrownBy(() -> files.read(linked + "/" + STOLEN)).isInstanceOf(IOException.class);
    assertThatThrownBy(() -> files.read(linked + "/outside.json")).isInstanceOf(IOException.class);
  }

  @Test
  void symbolicLinksToFilesAndFoldersAreNotFollowed() throws IOException {
    try {
      Files.createSymbolicLink(
          pack.resolve("data/sonde/loot_table/linkfile.json"), outside.resolve("outside.json"));
      Files.createSymbolicLink(pack.resolve("linkdir"), outside);
    } catch (UnsupportedOperationException | IOException | SecurityException e) {
      assumeThat(false).as("symbolic links are not available: " + e).isTrue();
    }
    assertNothingOutsideIsReachable("linkdir");
    assertThatThrownBy(() -> PackFiles.directory(pack).read("data/sonde/loot_table/linkfile.json"))
        .isInstanceOf(IOException.class);
  }

  @Test
  void aWindowsJunctionToAFolderOutsideThePackIsNotWalked()
      throws IOException, InterruptedException {
    assumeThat(System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT))
        .contains("windows");
    Path junction = pack.resolve("junction");
    Process process =
        new ProcessBuilder("cmd", "/c", "mklink", "/J", junction.toString(), outside.toString())
            .redirectErrorStream(true)
            .start();
    process.getInputStream().readAllBytes();
    assumeThat(process.waitFor()).as("cmd /c mklink /J exit status").isZero();
    try {
      assertNothingOutsideIsReachable("junction");
      List<String> paths = PackFiles.directory(pack).paths();
      assertThat(paths).noneMatch(path -> path.startsWith("junction"));
    } finally {
      // Deleting the junction itself leaves the target alone.
      Files.delete(junction);
    }
  }
}
