package dev.shoko.minecraft.data.pack.meta;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonString;
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

  // Null when absent; exposed as Optional only.
  private final JsonNode description;
  private final PackFormatRange formats;
  private final boolean broken;
  private final FeatureFlags features;
  private final List<FilterRule> filters;
  private final List<OverlayEntry> overlays;

  PackMeta(
      JsonNode description,
      PackFormatRange formats,
      boolean broken,
      FeatureFlags features,
      List<FilterRule> filters,
      List<OverlayEntry> overlays) {
    this.description = description;
    this.formats = formats;
    this.broken = broken;
    this.features = features;
    this.filters = List.copyOf(filters);
    this.overlays = List.copyOf(overlays);
  }

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
    PackFormat target = type.targetFormat();
    JsonNode description =
        new JsonString(
            new FileOrigin(packId, ""), JsonPath.root(), "dataPack." + packId + ".description");
    return new PackMeta(
        description,
        new PackFormatRange(target, target),
        false,
        FeatureFlags.vanilla(),
        List.of(),
        List.of());
  }

  /**
   * Returns the description, a text component kept as written (R4). Its content is not validated,
   * and the effect of its absence on the validity of the pack is not decided (open question of the
   * spec): an absent description is simply empty here.
   *
   * @return the JSON node of {@code pack.description}, or empty when absent
   */
  public Optional<JsonNode> description() {
    return Optional.ofNullable(description);
  }

  /**
   * Returns the supported versions (R5).
   *
   * @return the range, or empty when {@code min_format} or {@code max_format} is absent or
   *     mistyped, even with {@code pack_format} present. An inverted range ({@code min > max}) is
   *     present: it is reported by {@link PackFormatRange#compatibilityWith(PackFormat)}
   */
  public Optional<PackFormatRange> formats() {
    return Optional.ofNullable(formats);
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
    if (broken || formats == null) {
      return PackCompatibility.BROKEN;
    }
    return formats.compatibilityWith(type.targetFormat());
  }

  /**
   * Returns the flags the pack requires, from {@code features.enabled} (R44, R66 b). The loader
   * discards the pack when they are not all active (R66 c).
   *
   * @return the flags, empty when the field is absent (or mistyped, which breaks the pack)
   */
  public FeatureFlags features() {
    return features;
  }

  /**
   * Returns the rules of {@code filter.block} (R47).
   *
   * @return the rules in file order
   */
  public List<FilterRule> filters() {
    return filters;
  }

  /**
   * Returns the entries of {@code overlays.entries} (R48), active or not.
   *
   * @return the entries in file order
   */
  public List<OverlayEntry> overlays() {
    return overlays;
  }
}
