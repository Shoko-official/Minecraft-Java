package dev.shoko.minecraft.world.storage;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.RegionPos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Folders and file names of a 26.3 world (AR-R1 to AR-R4). Pure path computation: nothing is
 * created or read except by {@link #isLegacyLayout(Path)}.
 *
 * <p>Each dimension has {@code dimensions/<namespace>/<path>/} holding {@code region/}, {@code
 * entities/}, {@code poi/} and {@code data/}: {@code dimensions/minecraft/overworld/}. Region files
 * are {@code r.<rx>.<rz>.mca} with {@code rx = cx >> 5} (rounding down) written in decimal with a
 * minus sign when negative: chunk (-1, -1) is in {@code r.-1.-1.mca}.
 */
public final class WorldLayout {

  private static final String DIMENSIONS = "dimensions";
  private static final List<String> LEGACY_FOLDERS =
      List.of("region", "entities", "poi", "DIM-1", "DIM1");
  private static final Pattern PART = Pattern.compile("[a-z0-9_.-]+");

  private WorldLayout() {}

  /**
   * Returns the folder of a dimension.
   *
   * @param worldRoot the world folder
   * @param dimensionId dimension identifier with namespace, such as {@code minecraft:the_nether}
   * @return {@code worldRoot/dimensions/minecraft/the_nether}
   * @throws IllegalArgumentException if the identifier has no namespace, or a part is empty or
   *     could leave the world folder (a separator or a {@code ..} part)
   */
  public static Path dimensionFolder(Path worldRoot, String dimensionId) {
    int colon = dimensionId.indexOf(':');
    if (colon < 0) {
      throw new IllegalArgumentException("dimension identifier has no namespace: " + dimensionId);
    }
    String namespace = dimensionId.substring(0, colon);
    String path = dimensionId.substring(colon + 1);
    checkPart(namespace, dimensionId);
    checkPart(path, dimensionId);
    return worldRoot.resolve(DIMENSIONS).resolve(namespace).resolve(path);
  }

  /**
   * Returns the chunk folder of a dimension.
   *
   * @param dimensionFolder a folder from {@link #dimensionFolder(Path, String)}
   * @return its {@code region} folder
   */
  public static Path regionFolder(Path dimensionFolder) {
    return dimensionFolder.resolve("region");
  }

  /**
   * Returns the entity folder of a dimension.
   *
   * @param dimensionFolder a folder from {@link #dimensionFolder(Path, String)}
   * @return its {@code entities} folder
   */
  public static Path entitiesFolder(Path dimensionFolder) {
    return dimensionFolder.resolve("entities");
  }

  /**
   * Returns the points-of-interest folder of a dimension.
   *
   * @param dimensionFolder a folder from {@link #dimensionFolder(Path, String)}
   * @return its {@code poi} folder
   */
  public static Path poiFolder(Path dimensionFolder) {
    return dimensionFolder.resolve("poi");
  }

  /**
   * Returns the name of a region file (AR-R4, AR-R6).
   *
   * @param region the region position
   * @return the name, such as {@code r.46.-19.mca}
   */
  public static String regionFileName(RegionPos region) {
    return "r." + region.x() + "." + region.z() + ".mca";
  }

  /**
   * Returns the name of the external file of a chunk too large for its region file (AR-R17).
   *
   * @param chunk absolute chunk position
   * @return the name, such as {@code c.-1.-1.mcc}
   */
  public static String externalChunkFileName(ChunkPos chunk) {
    return "c." + chunk.x() + "." + chunk.z() + ".mcc";
  }

  /**
   * Tells whether a world folder uses the layout before 26.1 Snapshot 6, which the engine refuses
   * to open (AR-R3, provisional): {@code region/}, {@code entities/} or {@code poi/} at the root,
   * or a {@code DIM-1} or {@code DIM1} folder.
   *
   * @param worldRoot the world folder
   * @return {@code true} when any of them exists
   */
  public static boolean isLegacyLayout(Path worldRoot) {
    for (String folder : LEGACY_FOLDERS) {
      if (Files.exists(worldRoot.resolve(folder))) {
        return true;
      }
    }
    return false;
  }

  // Only characters that are safe in a file name on every platform; no separator, no dot-only part.
  private static void checkPart(String part, String dimensionId) {
    if (!PART.matcher(part).matches() || part.chars().allMatch(c -> c == '.')) {
      throw new IllegalArgumentException("invalid dimension identifier: " + dimensionId);
    }
  }
}
