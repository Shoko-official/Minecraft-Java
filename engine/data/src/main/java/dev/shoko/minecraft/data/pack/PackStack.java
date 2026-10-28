package dev.shoko.minecraft.data.pack;

import dev.shoko.minecraft.data.pack.meta.PackType;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Packs of one kind in priority order, from the lowest priority (vanilla first) to the highest
 * (R40). Immutable.
 *
 * <p>For every path, the stack applies, in this order: the active overlays of each pack, whose
 * files replace those of the pack (R48), then the block filters of each pack, which hide files of
 * packs of lower priority (R47). A file visible in several packs is replaced by the one of the
 * highest priority, in full, without merging content (R41); {@link #layers(String)} still exposes
 * every version, for tags, which merge (R34). Pack metadata files and extraction markers are not
 * interpreted here.
 */
public final class PackStack {

  private PackStack() {}

  /**
   * Builds a stack.
   *
   * @param type the kind of the packs, which sets the target format used for overlays
   * @param packsLowestFirst the packs from the lowest to the highest priority
   * @return the stack; later changes to the list do not affect it
   * @throws IllegalArgumentException when two packs have the same identifier or a pack has another
   *     kind
   */
  public static PackStack of(PackType type, List<Pack> packsLowestFirst) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the packs.
   *
   * @return the packs from the lowest to the highest priority
   */
  public List<Pack> packs() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Lists the paths visible after overlays and filters.
   *
   * @return the distinct stacked paths sorted by Unicode code point
   * @throws IOException when a pack cannot be listed
   */
  public List<String> paths() throws IOException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns every visible version of a path.
   *
   * @param path a stacked path, such as {@code data/minecraft/tags/block/logs.json}
   * @return the versions from the lowest to the highest priority, empty when no pack has the path
   * @throws IOException when a pack cannot be listed
   */
  public List<PackFileRef> layers(String path) throws IOException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the version that wins for a path (R41).
   *
   * @param path a stacked path
   * @return the version of the highest priority, or empty when no pack has the path
   * @throws IOException when a pack cannot be listed
   */
  public Optional<PackFileRef> winner(String path) throws IOException {
    return Skeleton.throwNotImplemented();
  }
}
