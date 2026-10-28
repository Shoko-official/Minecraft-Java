package dev.shoko.minecraft.data.pack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Read access to the files of one pack: a folder, a {@code .zip} archive (R19) or a set held in
 * memory. Paths are relative to the root, use {@code /} on every system (R24) and never start with
 * one. Implementations do not interpret file content.
 */
public interface PackFiles {

  /**
   * Opens a folder.
   *
   * @param root the pack folder
   * @return the files below {@code root}, symbolic links not followed
   */
  static PackFiles directory(Path root) {
    return new DirectoryFiles(root);
  }

  /**
   * Opens a {@code .zip} archive whose tree starts at the root of the archive (R19).
   *
   * @param archive the archive file
   * @return the files of the archive
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
   * @throws IOException when the file cannot be read or does not exist
   */
  byte[] read(String path) throws IOException;

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
