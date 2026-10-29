package dev.shoko.minecraft.world.heightmap;

/**
 * The heightmap types of the {@code Heightmaps} Compound (CF-R16). The NBT key of a type is the
 * constant name.
 */
public enum HeightmapType {
  WORLD_SURFACE,
  WORLD_SURFACE_WG,
  OCEAN_FLOOR,
  OCEAN_FLOOR_WG,
  MOTION_BLOCKING,
  MOTION_BLOCKING_NO_LEAVES
}
