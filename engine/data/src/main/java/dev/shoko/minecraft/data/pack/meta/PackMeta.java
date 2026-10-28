package dev.shoko.minecraft.data.pack.meta;

import dev.shoko.minecraft.data.json.JsonNode;
import java.util.List;
import java.util.Optional;

/**
 * The content of a {@code pack.mcmeta} (R4). Unknown fields, at any depth, are ignored without
 * diagnostic (provisional, R4, R63 c). Immutable.
 *
 * <p>A pack is broken (R4, R5) when a known field has a type it should not, or when {@code
 * min_format} or {@code max_format} is absent or {@code min_format} is above {@code max_format}; a
 * broken pack reports {@link PackCompatibility#BROKEN}, and its fields that could not be read take
 * their empty value.
 */
public final class PackMeta {

  private PackMeta() {}

  /**
   * Returns the metadata of a pack that has no {@code pack.mcmeta} file, such as the vanilla pack
   * (R3): the exact target format, no flag, no filter, no overlay.
   *
   * @param packId the identifier of the pack, used as the origin of the description
   * @param type the kind of pack, which sets the format
   * @return the metadata; {@link #features()} is {@link FeatureFlags#vanilla()}, since the implicit
   *     pack requires {@code minecraft:vanilla} (R66 b)
   */
  public static PackMeta implicit(String packId, PackType type) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the description, a text component kept as written (R4). Its content is not validated,
   * and the effect of its absence on the validity of the pack is not decided (open question of the
   * spec): an absent description is simply empty here.
   *
   * @return the JSON node of {@code pack.description}, or empty when absent
   */
  public Optional<JsonNode> description() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the supported versions (R5).
   *
   * @return the range, or empty when {@code min_format} or {@code max_format} is absent or
   *     mistyped, even with {@code pack_format} present. An inverted range ({@code min > max}) is
   *     present: it is reported by {@link PackFormatRange#compatibilityWith(PackFormat)}
   */
  public Optional<PackFormatRange> formats() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Classifies the pack against the game version for its kind (R5).
   *
   * @param type the kind of pack
   * @return {@link PackCompatibility#BROKEN} when {@link #formats()} is empty or another known
   *     field was mistyped, otherwise the result of {@link
   *     PackFormatRange#compatibilityWith(PackFormat)} for {@link PackType#targetFormat()}
   */
  public PackCompatibility compatibility(PackType type) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the flags the pack requires, from {@code features.enabled} (R44, R66 b). The loader
   * discards the pack when they are not all active (R66 c).
   *
   * @return the flags, empty when the field is absent (or mistyped, which breaks the pack)
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
