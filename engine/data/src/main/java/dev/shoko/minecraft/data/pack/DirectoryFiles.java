package dev.shoko.minecraft.data.pack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The files of a folder. Only the regular files found by one walk of the folder can be read: a path
 * outside that listing is refused, so a path cannot reach out of the folder. The walk neither
 * follows nor lists links of any kind: a symbolic link, or on Windows a junction, which the JDK
 * reports as "other", is skipped, and a folder that is itself such a link is refused.
 */
final class DirectoryFiles extends BaseFiles {

  private final Path root;
  private List<String> listing;
  // Membership tests only: never iterated, so its order is not observable.
  private Set<String> listed;

  DirectoryFiles(Path root) {
    this.root = root;
  }

  @Override
  public synchronized List<String> paths() throws IOException {
    list();
    return new ArrayList<>(listing);
  }

  @Override
  public byte[] read(String path) throws IOException {
    boolean known;
    synchronized (this) {
      list();
      known = listed.contains(path);
    }
    if (!known) {
      throw new NoSuchFileException(path);
    }
    Path target = root.resolve(path);
    if (Files.size(target) > PackFiles.MAX_FILE_BYTES) {
      throw BoundedRead.tooLarge(path);
    }
    try (InputStream in = Files.newInputStream(target)) {
      return BoundedRead.readAll(in, path);
    }
  }

  private void list() throws IOException {
    if (listing != null) {
      return;
    }
    if (isLink(Files.readAttributes(root, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS))) {
      throw new PackContentException("the pack folder is a link: " + root);
    }
    List<String> found = new ArrayList<>();
    Files.walkFileTree(
        root,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return isLink(attrs) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (attrs.isRegularFile()) {
              found.add(slashed(root.relativize(file)));
            }
            return FileVisitResult.CONTINUE;
          }
        });
    found.sort(PathOrder.BY_CODE_POINT);
    listing = found;
    listed = new HashSet<>(found);
  }

  private static boolean isLink(BasicFileAttributes attrs) {
    return attrs.isSymbolicLink() || attrs.isOther();
  }

  private static String slashed(Path relative) {
    List<String> names = new ArrayList<>();
    for (Path name : relative) {
      names.add(name.toString());
    }
    return String.join("/", names);
  }
}
