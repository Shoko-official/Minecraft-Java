package dev.shoko.minecraft.tools.bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Deletes and fingerprints directory trees of the cache without ever following a link.
 *
 * <p>A symbolic link or an NTFS junction (seen by Java as a directory that is also "other") is a
 * leaf: {@link #clear} deletes the link itself, never what it points to, and {@link #fingerprint}
 * records it without descending. Both are recognised from their own attributes, so a link back into
 * the tree is not entered either.
 */
final class SafeTree {

  /**
   * The observable shape of a tree.
   *
   * @param files number of regular files
   * @param listing SHA-256 of the sorted lines {@code relative/path TAB size} (links appear as
   *     {@code relative/path TAB link}); it changes when a file is added, deleted, renamed, or
   *     changes size
   * @param content SHA-256 of the sorted lines {@code relative/path TAB size TAB sha256}, or null
   *     when the content was not hashed; only this detects a same-size rewrite of a file
   */
  record Fingerprint(int files, String listing, String content) {}

  private SafeTree() {}

  /**
   * Deletes {@code dir} and everything under it. Refuses, without deleting anything, when the real
   * path of {@code dir} is outside the real path of {@code cacheRoot}.
   */
  static void clear(Path dir, Path cacheRoot) throws IOException {
    if (!Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    BasicFileAttributes rootAttrs =
        Files.readAttributes(dir, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    if (isLink(rootAttrs)) {
      Files.delete(dir);
      return;
    }
    Path realCache = cacheRoot.toRealPath();
    Path realDir = dir.toRealPath();
    if (!realDir.startsWith(realCache)) {
      throw new IOException(
          "Refusing to clear "
              + dir.toAbsolutePath()
              + ": its real path "
              + realDir
              + " is outside the cache "
              + realCache);
    }
    Files.walkFileTree(dir, new Deleter());
  }

  /**
   * Fingerprints the tree under {@code dir}; a missing directory has no files, and a {@code dir}
   * that is itself a link has none either (its listing only records the link).
   *
   * @param hashContent whether to also hash every file, which reads all of them
   */
  static Fingerprint fingerprint(Path dir, boolean hashContent) throws IOException {
    Lister lister = new Lister(dir, hashContent);
    if (Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) {
      Files.walkFileTree(dir, lister);
    }
    return new Fingerprint(
        lister.files, digest(lister.listing), hashContent ? digest(lister.content) : null);
  }

  private static boolean isLink(BasicFileAttributes attrs) {
    return attrs.isSymbolicLink() || attrs.isOther();
  }

  private static String relative(Path root, Path path) {
    StringBuilder name = new StringBuilder();
    for (Path part : root.relativize(path)) {
      if (!name.isEmpty()) {
        name.append('/');
      }
      name.append(part);
    }
    return name.toString();
  }

  private static String digest(List<String> lines) {
    List<String> sorted = new ArrayList<>(lines);
    sorted.sort(null);
    StringBuilder text = new StringBuilder();
    for (String line : sorted) {
      text.append(line).append('\n');
    }
    return Hashes.hex(text.toString().getBytes(StandardCharsets.UTF_8), "SHA-256");
  }

  /** Deletes depth first; links are deleted as leaves, their targets are never visited. */
  private static final class Deleter extends SimpleFileVisitor<Path> {

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException {
      if (isLink(attrs)) {
        Files.delete(dir);
        return FileVisitResult.SKIP_SUBTREE;
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      Files.delete(file);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException failure) throws IOException {
      if (failure != null) {
        throw failure;
      }
      Files.delete(dir);
      return FileVisitResult.CONTINUE;
    }
  }

  /** Collects one line per file or link; never descends into a link. */
  private static final class Lister extends SimpleFileVisitor<Path> {

    private final Path root;
    private final boolean hashContent;
    private final List<String> listing = new ArrayList<>();
    private final List<String> content = new ArrayList<>();
    private int files;

    Lister(Path root, boolean hashContent) {
      this.root = root;
      this.hashContent = hashContent;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      if (isLink(attrs)) {
        addLink(dir);
        return FileVisitResult.SKIP_SUBTREE;
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (!attrs.isRegularFile()) {
        addLink(file);
        return FileVisitResult.CONTINUE;
      }
      String name = relative(root, file);
      files++;
      listing.add(name + "\t" + attrs.size());
      if (hashContent) {
        content.add(name + "\t" + attrs.size() + "\t" + Hashes.hex(file, "SHA-256"));
      }
      return FileVisitResult.CONTINUE;
    }

    private void addLink(Path path) {
      listing.add(relative(root, path) + "\tlink");
      content.add(relative(root, path) + "\tlink");
    }
  }
}
