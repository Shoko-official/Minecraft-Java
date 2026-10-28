package dev.shoko.minecraft.tools.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SafeTreeTest {

  /** A cache with {@code resources/sub/a.txt} and a directory outside it with a file to keep. */
  private record Layout(Path cache, Path resources, Path outside, Path keep) {

    static Layout create(Path dir) throws IOException {
      Path cache = Files.createDirectories(dir.resolve("cache"));
      Path resources = Files.createDirectories(cache.resolve("resources/sub"));
      Files.writeString(resources.resolve("a.txt"), "a");
      Path outside = Files.createDirectories(dir.resolve("outside"));
      Path keep = Files.writeString(outside.resolve("keep.txt"), "keep");
      return new Layout(cache, cache.resolve("resources"), outside, keep);
    }
  }

  @Test
  void clearDeletesTheWholeTree(@TempDir Path dir) throws IOException {
    Layout layout = Layout.create(dir);

    SafeTree.clear(layout.resources(), layout.cache());

    assertThat(layout.resources()).doesNotExist();
    assertThat(layout.cache()).exists();
  }

  @Test
  void clearOfAMissingDirectoryDoesNothing(@TempDir Path dir) throws IOException {
    SafeTree.clear(dir.resolve("missing"), dir);

    assertThat(dir).isEmptyDirectory();
  }

  @Test
  void clearDeletesAJunctionButNotItsTarget(@TempDir Path dir) throws Exception {
    Layout layout = Layout.create(dir);
    TestLinks.junction(layout.resources().resolve("sub/escape"), layout.outside());

    SafeTree.clear(layout.resources(), layout.cache());

    assertThat(layout.resources()).doesNotExist();
    assertThat(layout.keep()).hasContent("keep");
  }

  @Test
  void clearDeletesASymbolicLinkButNotItsTarget(@TempDir Path dir) throws IOException {
    Layout layout = Layout.create(dir);
    TestLinks.symlink(layout.resources().resolve("sub/escape"), layout.outside());

    SafeTree.clear(layout.resources(), layout.cache());

    assertThat(layout.resources()).doesNotExist();
    assertThat(layout.keep()).hasContent("keep");
  }

  /** The real path test alone cannot see this link: its target is inside the tree. */
  @Test
  void clearDoesNotEnterALinkBackToItsOwnTree(@TempDir Path dir) throws Exception {
    Layout layout = Layout.create(dir);
    TestLinks.directoryLink(layout.resources().resolve("sub/loop"), layout.resources());

    SafeTree.clear(layout.resources(), layout.cache());

    assertThat(layout.resources()).doesNotExist();
  }

  @Test
  void fingerprintDoesNotEnterALinkBackToItsOwnTree(@TempDir Path dir) throws Exception {
    Layout layout = Layout.create(dir);
    SafeTree.Fingerprint before = SafeTree.fingerprint(layout.resources(), false);
    TestLinks.directoryLink(layout.resources().resolve("sub/loop"), layout.resources());

    assertThat(SafeTree.fingerprint(layout.resources(), false).files()).isEqualTo(before.files());
  }

  @Test
  void clearOfARootThatIsAJunctionDeletesOnlyTheJunction(@TempDir Path dir) throws Exception {
    Layout layout = Layout.create(dir);
    Path linked = layout.cache().resolve("linked");
    TestLinks.directoryLink(linked, layout.outside());

    SafeTree.clear(linked, layout.cache());

    assertThat(linked).doesNotExist();
    assertThat(layout.keep()).hasContent("keep");
  }

  @Test
  void clearRefusesADirectoryWhoseRealPathLeavesTheCache(@TempDir Path dir) throws Exception {
    Layout layout = Layout.create(dir);
    Files.createDirectories(layout.outside().resolve("26.3/resources"));
    Path kept = Files.writeString(layout.outside().resolve("26.3/resources/kept.txt"), "kept");
    Path reference = layout.cache().resolve("reference");
    TestLinks.directoryLink(reference, layout.outside());

    assertThatThrownBy(() -> SafeTree.clear(reference.resolve("26.3/resources"), layout.cache()))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("outside the cache");
    assertThat(kept).hasContent("kept");
  }

  @Test
  void fingerprintCountsFilesAndIsStable(@TempDir Path dir) throws IOException {
    Layout layout = Layout.create(dir);
    Files.writeString(layout.resources().resolve("b.txt"), "bb");

    SafeTree.Fingerprint first = SafeTree.fingerprint(layout.resources(), true);
    SafeTree.Fingerprint second = SafeTree.fingerprint(layout.resources(), true);

    assertThat(first.files()).isEqualTo(2);
    assertThat(second).isEqualTo(first);
    assertThat(SafeTree.fingerprint(layout.resources(), false).content()).isNull();
  }

  @Test
  void fingerprintListingSeesDeletionAdditionAndTruncation(@TempDir Path dir) throws IOException {
    Layout layout = Layout.create(dir);
    Path file = layout.resources().resolve("sub/a.txt");
    String original = SafeTree.fingerprint(layout.resources(), false).listing();

    Files.delete(file);
    String deleted = SafeTree.fingerprint(layout.resources(), false).listing();
    Files.writeString(file, "a");
    Files.writeString(layout.resources().resolve("added.txt"), "x");
    String added = SafeTree.fingerprint(layout.resources(), false).listing();
    Files.delete(layout.resources().resolve("added.txt"));
    Files.writeString(file, "");
    String truncated = SafeTree.fingerprint(layout.resources(), false).listing();

    assertThat(deleted).isNotEqualTo(original);
    assertThat(added).isNotEqualTo(original);
    assertThat(truncated).isNotEqualTo(original);
  }

  @Test
  void onlyTheContentDigestSeesASameSizeRewrite(@TempDir Path dir) throws IOException {
    Layout layout = Layout.create(dir);
    SafeTree.Fingerprint before = SafeTree.fingerprint(layout.resources(), true);

    Files.writeString(layout.resources().resolve("sub/a.txt"), "z");
    SafeTree.Fingerprint after = SafeTree.fingerprint(layout.resources(), true);

    assertThat(after.listing()).isEqualTo(before.listing());
    assertThat(after.content()).isNotEqualTo(before.content());
  }

  @Test
  void fingerprintRecordsAJunctionWithoutEnteringIt(@TempDir Path dir) throws Exception {
    Layout layout = Layout.create(dir);
    SafeTree.Fingerprint before = SafeTree.fingerprint(layout.resources(), true);

    TestLinks.directoryLink(layout.resources().resolve("sub/escape"), layout.outside());
    SafeTree.Fingerprint after = SafeTree.fingerprint(layout.resources(), true);

    assertThat(after.files()).isEqualTo(before.files());
    assertThat(after.listing()).isNotEqualTo(before.listing());
  }

  @Test
  void fingerprintOfARootLinkHasNoFiles(@TempDir Path dir) throws Exception {
    Layout layout = Layout.create(dir);
    Path linked = layout.cache().resolve("linked");
    TestLinks.directoryLink(linked, layout.resources());

    assertThat(SafeTree.fingerprint(linked, false).files()).isZero();
    assertThat(Files.exists(linked, LinkOption.NOFOLLOW_LINKS)).isTrue();
  }
}
