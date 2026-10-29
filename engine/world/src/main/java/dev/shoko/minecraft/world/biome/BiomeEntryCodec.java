package dev.shoko.minecraft.world.biome;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.WarningSink;
import dev.shoko.minecraft.world.palette.PaletteEntryCodec;
import dev.shoko.minecraft.world.palette.PaletteFormatException;

/**
 * Converts a biome identifier to and from a palette entry (CF-R11, provisional).
 *
 * <p>Reading accepts a String, or a Compound with a String {@code Name}. An identifier unknown to
 * the {@link BiomeCatalog} becomes {@link #FALLBACK_BIOME} with an {@code UNKNOWN_BIOME} warning.
 * Any other tag fails with a {@link PaletteFormatException}, which refuses the chunk as an
 * inconsistent container; the spec does not classify that case (CF-R11, CF-R10). Writing produces a
 * String.
 */
public final class BiomeEntryCodec implements PaletteEntryCodec<String> {

  /**
   * Biome used for an unknown biome and for a section without {@code biomes} (CF-R11, CF-R22,
   * provisional).
   */
  public static final String FALLBACK_BIOME = "minecraft:plains";

  private final BiomeCatalog catalog;
  private final WarningSink warnings;

  private BiomeEntryCodec(BiomeCatalog catalog, WarningSink warnings) {
    this.catalog = catalog;
    this.warnings = warnings;
  }

  /**
   * Creates a codec.
   *
   * @param catalog the known biomes
   * @param warnings where the findings of the lenient read go ({@code UNKNOWN_BIOME})
   * @return the codec
   */
  public static BiomeEntryCodec of(BiomeCatalog catalog, WarningSink warnings) {
    return new BiomeEntryCodec(catalog, warnings);
  }

  @Override
  public String decode(NbtTag entry) throws PaletteFormatException {
    String id;
    if (entry instanceof StringTag text) {
      id = text.value();
    } else if (entry instanceof CompoundTag compound
        && compound.get("Name").orElse(null) instanceof StringTag name) {
      id = name.value();
    } else {
      throw new PaletteFormatException(
          "biome palette entry is not a String or a Compound with Name: " + entry.type());
    }
    if (catalog.contains(id)) {
      return id;
    }
    warnings.warn(
        WarningCode.UNKNOWN_BIOME, "unknown biome " + id + ", replaced by " + FALLBACK_BIOME);
    return FALLBACK_BIOME;
  }

  @Override
  public NbtTag encode(String value) {
    return new StringTag(value);
  }
}
