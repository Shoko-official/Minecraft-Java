package dev.shoko.minecraft.world.biome;

/**
 * Port: the biomes that exist in the loaded data ({@code worldgen/biome}, 67 in the reference
 * data). Defined here so that {@code world} does not depend on how the data module exposes its
 * registries.
 */
public interface BiomeCatalog {

  /**
   * Tells whether a biome exists.
   *
   * @param biomeId identifier with namespace
   * @return {@code true} when the biome is loaded
   */
  boolean contains(String biomeId);
}
