package dev.shoko.minecraft.data.pack;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Read access to the files of one pack: a folder, a {@code .zip} archive (R19) or a set held in
 * memory. Paths are relative to the root, use {@code /} on every system (R24) and never start with
 * one. Implementations do not interpret file content.
 *
 * <p>No file larger than {@link #MAX_FILE_BYTES} is ever read: {@link #read(String)} refuses it
 * with a {@link PackContentException}, whatever the pack is stored in.
 */
public interface PackFiles {

  /**
   * Size ceiling of one file, in bytes. The files of the target data are under 60 KB (R49); the
   * ceiling is a safety margin against decompression bombs and hostile packs, not a rule of the
   * game (architecture decision, not observable on the reference).
   */
  int MAX_FILE_BYTES = 8 * 1024 * 1024;

  /**
   * Opens a folder.
   *
   * @param root the pack folder
   * @return the files below {@code root}. Neither symbolic links nor other links such as Windows
   *     junctions are followed or listed, and a root that is itself a link is refused when the
   *     files are first listed or read, with an {@link IOException}
   */
  static PackFiles directory(Path root) {
    return new DirectoryFiles(root);
  }

  /**
   * Opens a {@code .zip} archive whose tree starts at the root of the archive (R19).
   *
   * @param archive the archive file
   * @return the files of the archive. Entries whose name is not a plain relative path (absolute,
   *     with {@code ..}, {@code .}, empty or backslash steps, or a {@code :}) are neither listed
   *     nor readable. When several entries have the same name, the name is listed once and the last
   *     entry is the one read
   */
  static PackFiles archive(Path archive) {
    return new ArchiveFiles(archive);
  }

  /**
   * Holds files in memory, for packs built by code and for tests.
   *
   * @param files the content of each path, in any order
   * @return the files; later changes to {@code files} do not affect it
   */
  static PackFiles inMemory(Map<String, byte[]> files) {
    return new MemoryFiles(files);
  }

  /**
   * Lists every file, directories excluded.
   *
   * @return the paths sorted by Unicode code point
   * @throws IOException when the pack cannot be listed
   */
  List<String> paths() throws IOException;

  /**
   * Reads one file.
   *
   * @param path a path returned by {@link #paths()}
   * @return the bytes
   * @throws PackContentException when the file is larger than {@link #MAX_FILE_BYTES} or its entry
   *     in an archive is damaged
   * @throws IOException when the file cannot be read or does not exist
   */
  byte[] read(String path) throws IOException;

  /**
   * Keeps the underlying resource, such as the open archive, until the returned handle is closed,
   * so that many reads cost one opening. Reads work without it, each one then opening and closing
   * the resource. Handles may be nested.
   *
   * @return the handle to close
   * @throws IOException when the resource cannot be opened
   */
  default Closeable keepOpen() throws IOException {
    return () -> {};
  }

  /**
   * Returns a view of one directory as a root of its own, as used for the built-in packs stored in
   * {@code data/minecraft/datapacks/<name>/} (R14) and for overlay directories (R48).
   *
   * @param directory the directory path, without trailing slash
   * @return the files below it, with paths relative to it
   */
  PackFiles subtree(String directory);

  /**
   * Returns a view without one directory, as the vanilla pack excludes {@code
   * data/minecraft/datapacks/} (R14).
   *
   * @param directory the directory path, without trailing slash
   * @return every file except those below it
   */
  PackFiles without(String directory);
}
