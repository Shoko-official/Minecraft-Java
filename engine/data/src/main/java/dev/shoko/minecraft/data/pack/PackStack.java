package dev.shoko.minecraft.data.pack;

import dev.shoko.minecraft.data.pack.meta.PackFormat;
import dev.shoko.minecraft.data.pack.meta.PackType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

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

  private final PackType type;
  private final List<Pack> packs;
  // Built on first use: listing the files can fail, and of() cannot throw an IOException.
  private Map<String, List<PackFileRef>> index;

  private PackStack(PackType type, List<Pack> packs) {
    this.type = type;
    this.packs = packs;
  }

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
    Set<String> ids = new HashSet<>();
    for (Pack pack : packsLowestFirst) {
      if (pack.type() != type) {
        throw new IllegalArgumentException("pack " + pack.id() + " is not a " + type + " pack");
      }
      if (!ids.add(pack.id())) {
        throw new IllegalArgumentException("duplicate pack identifier: " + pack.id());
      }
    }
    return new PackStack(type, List.copyOf(packsLowestFirst));
  }

  /**
   * Returns the packs.
   *
   * @return the packs from the lowest to the highest priority
   */
  public List<Pack> packs() {
    return packs;
  }

  /**
   * Lists the paths visible after overlays and filters.
   *
   * @return the distinct stacked paths sorted by Unicode code point
   * @throws IOException when a pack cannot be listed
   */
  public List<String> paths() throws IOException {
    return new ArrayList<>(index().keySet());
  }

  /**
   * Returns every visible version of a path.
   *
   * @param path a stacked path, such as {@code data/minecraft/tags/block/logs.json}
   * @return the versions from the lowest to the highest priority, empty when no pack has the path
   * @throws IOException when a pack cannot be listed
   */
  public List<PackFileRef> layers(String path) throws IOException {
    return index().getOrDefault(path, List.of());
  }

  /**
   * Returns the version that wins for a path (R41).
   *
   * @param path a stacked path
   * @return the version of the highest priority, or empty when no pack has the path
   * @throws IOException when a pack cannot be listed
   */
  public Optional<PackFileRef> winner(String path) throws IOException {
    List<PackFileRef> versions = layers(path);
    return versions.isEmpty() ? Optional.empty() : Optional.of(versions.getLast());
  }

  private synchronized Map<String, List<PackFileRef>> index() throws IOException {
    if (index == null) {
      index = build();
    }
    return index;
  }

  private Map<String, List<PackFileRef>> build() throws IOException {
    PackFormat target = type.targetFormat();
    List<Map<String, String>> views = new ArrayList<>();
    for (Pack pack : packs) {
      views.add(PackView.of(pack, target));
    }
    for (int higher = 1; higher < packs.size(); higher++) {
      if (!packs.get(higher).meta().filters().isEmpty()) {
        for (int lower = 0; lower < higher; lower++) {
          PackView.hide(views.get(lower), packs.get(higher).meta().filters());
        }
      }
    }
    Map<String, List<PackFileRef>> built = new TreeMap<>(PathOrder.BY_CODE_POINT);
    for (int i = 0; i < packs.size(); i++) {
      Pack pack = packs.get(i);
      for (Map.Entry<String, String> file : views.get(i).entrySet()) {
        PackFileRef ref = new PackFileRef(pack.id(), file.getKey(), file.getValue(), pack.files());
        built.computeIfAbsent(file.getKey(), key -> new ArrayList<>()).add(ref);
      }
    }
    built.replaceAll((path, refs) -> List.copyOf(refs));
    return built;
  }
}
