package dev.shoko.minecraft.data.testing;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackFiles;
import dev.shoko.minecraft.data.pack.PackOrigin;
import dev.shoko.minecraft.data.pack.PackStack;
import dev.shoko.minecraft.data.pack.meta.PackType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Builders of small packs, in memory or on disk, from alternating paths and text contents. */
public final class Packs {

  /** A {@code pack.mcmeta} for the target data version, as a built-in pack writes it (R2). */
  public static final String MCMETA =
      "{\"pack\":{\"description\":\"t\",\"min_format\":121,\"max_format\":121}}";

  private Packs() {}

  /** Alternating paths and contents to an ordered map. */
  public static Map<String, String> tree(String... pathsAndContents) {
    Map<String, String> files = new LinkedHashMap<>();
    for (int i = 0; i < pathsAndContents.length; i += 2) {
      files.put(pathsAndContents[i], pathsAndContents[i + 1]);
    }
    return files;
  }

  public static Map<String, byte[]> bytes(Map<String, String> tree) {
    Map<String, byte[]> files = new LinkedHashMap<>();
    tree.forEach((path, content) -> files.put(path, Jx.utf8(content)));
    return files;
  }

  public static PackFiles inMemory(String... pathsAndContents) {
    return PackFiles.inMemory(bytes(tree(pathsAndContents)));
  }

  /** The implicit pack, which has no {@code pack.mcmeta} (R3). */
  public static Pack implicit(String id, String... pathsAndContents) {
    return Pack.implicit(id, PackOrigin.BUILT_IN, PackType.DATA, inMemory(pathsAndContents));
  }

  /** A world data pack with a compatible {@code pack.mcmeta} added to the given files. */
  public static Pack world(String id, String... pathsAndContents) {
    return withMeta(id, MCMETA, pathsAndContents);
  }

  /** A world data pack opened from the given {@code pack.mcmeta} text. */
  public static Pack withMeta(String id, String mcmeta, String... pathsAndContents) {
    Map<String, String> files = tree(pathsAndContents);
    files.put("pack.mcmeta", mcmeta);
    return open(id, PackOrigin.WORLD, files);
  }

  public static Pack open(String id, PackOrigin origin, Map<String, String> files) {
    try {
      Outcome<Pack> outcome =
          Pack.open(id, origin, PackType.DATA, PackFiles.inMemory(bytes(files)));
      return outcome.value().orElseThrow();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static PackStack stack(Pack... lowestFirst) {
    return PackStack.of(PackType.DATA, List.of(lowestFirst));
  }

  /** Writes the files below a folder, creating the sub-folders. */
  public static void writeTree(Path root, Map<String, String> files) throws IOException {
    for (Map.Entry<String, String> file : files.entrySet()) {
      Path target = root.resolve(file.getKey());
      Files.createDirectories(target.getParent());
      Files.write(target, Jx.utf8(file.getValue()));
    }
  }

  /** Writes a zip archive whose tree starts at the root of the archive (R19). */
  public static Path zip(Path archive, Map<String, String> files) throws IOException {
    try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
      for (Map.Entry<String, String> file : files.entrySet()) {
        out.putNextEntry(new ZipEntry(file.getKey()));
        out.write(Jx.utf8(file.getValue()));
        out.closeEntry();
      }
    }
    return archive;
  }
}
