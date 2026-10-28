package dev.shoko.minecraft.data.pack.meta;

import dev.shoko.minecraft.data.json.JsonNode;
import java.util.List;
import java.util.Optional;

/**
 * The content of a {@code pack.mcmeta} (R4). Unknown fields are ignored (provisional, R4).
 * Immutable.
 */
public final class PackMeta {

  private PackMeta() {}

  /**
   * Returns the metadata of a pack that has no {@code pack.mcmeta} file, such as the vanilla pack
   * (R3): the exact target format, no flag, no filter, no overlay.
   *
   * @param packId the identifier of the pack, used as the origin of the description
   * @param type the kind of pack, which sets the format
   * @return the metadata
   */
  public static PackMeta implicit(String packId, PackType type) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the description, a text component kept as written (R4).
   *
   * @return the JSON node of {@code pack.description}
   */
  public JsonNode description() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the supported versions (R5).
   *
   * @return the range, or empty when {@code min_format} or {@code max_format} is absent or
   *     mistyped, even with {@code pack_format} present
   */
  public Optional<PackFormatRange> formats() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Classifies the pack against the game version for its kind (R5).
   *
   * @param type the kind of pack
   * @return {@link PackCompatibility#BROKEN} when {@link #formats()} is empty, otherwise the result
   *     of {@link PackFormatRange#compatibilityWith(PackFormat)} for {@link
   *     PackType#targetFormat()}
   */
  public PackCompatibility compatibility(PackType type) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the flags the pack requires, from {@code features.enabled} (R44, R45).
   *
   * @return the flags, empty when the field is absent
   */
  public FeatureFlags features() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the rules of {@code filter.block} (R47).
   *
   * @return the rules in file order
   */
  public List<FilterRule> filters() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the entries of {@code overlays.entries} (R48), active or not.
   *
   * @return the entries in file order
   */
  public List<OverlayEntry> overlays() {
    return Skeleton.throwNotImplemented();
  }
}
