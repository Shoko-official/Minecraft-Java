package dev.shoko.minecraft.data.pack;

import dev.shoko.minecraft.data.diag.Outcome;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Finds the packs available on disk (R3, R14, R19, R40). */
public final class PackDiscovery {

  private PackDiscovery() {}

  /**
   * Finds the packs of the extracted reference data: the implicit vanilla pack (R3), then the
   * built-in packs stored under {@code data/minecraft/datapacks/} (R14).
   *
   * <p>The vanilla pack excludes that folder (R14) and ignores the {@code .mcassetsroot} marker
   * (R13). It has the identifier {@code vanilla} and the origin {@link PackOrigin#BUILT_IN}. Each
   * built-in pack is a datapack named after its folder, with {@link PackOrigin#FEATURE}, ordered by
   * name by Unicode code point.
   *
   * @param resourcesRoot the {@code resources} folder of the extracted data
   * @return the packs, vanilla first, with the diagnostics of the packs that could not be opened
   * @throws IOException when a folder cannot be listed
   */
  public static Outcome<List<Pack>> referencePacks(Path resourcesRoot) throws IOException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Finds the packs of a world {@code datapacks} folder: sub-folders and {@code .zip} archives
   * (R19).
   *
   * <p>A pack is named {@code file/<name of the folder or archive>} (R40) and has {@link
   * PackOrigin#WORLD}. An entry without a usable {@code pack.mcmeta} is not a pack and is ignored
   * with a diagnostic (R7). The order is by name by Unicode code point (provisional: the order the
   * game discovers packs is an open question).
   *
   * @param datapacksFolder the {@code datapacks} folder of a world
   * @return the packs, with the diagnostics of the entries that were ignored
   * @throws IOException when the folder cannot be listed
   */
  public static Outcome<List<Pack>> worldPacks(Path datapacksFolder) throws IOException {
    return Skeleton.throwNotImplemented();
  }
}
