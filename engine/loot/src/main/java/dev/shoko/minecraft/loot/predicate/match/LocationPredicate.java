package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.world.chunk.Chunk;
import java.util.List;

/**
 * The position predicate of {@code location_check.predicate} and of the {@code minecraft:location}
 * key of an entity predicate (LT-R59). All requirements must hold.
 *
 * @param requirements the keys present, in file order, unmodifiable
 */
public record LocationPredicate(List<Requirement> requirements) {

  private static final int FULL_SKY_LIGHT = 15;

  /** One key of a position predicate. */
  public sealed interface Requirement
      permits Requirement.Block, Requirement.Biomes, Requirement.CanSeeSky {

    /**
     * The block at the position.
     *
     * @param block the block predicate
     */
    record Block(BlockMatch block) implements Requirement {}

    /**
     * The biome at the position.
     *
     * @param biomes the accepted biomes
     */
    record Biomes(IdOrTagSet biomes) implements Requirement {}

    /**
     * Whether the sky is visible from the position.
     *
     * @param expected the expected answer
     */
    record CanSeeSky(boolean expected) implements Requirement {}
  }

  /**
   * Tells whether a position satisfies the predicate. The block, biome and sky are read in the
   * chunk given by {@link LootContext#chunkAt}; a chunk that is not available fails every
   * requirement (provisional).
   *
   * @param context the context, for the chunks and the registries
   * @param pos the block position, offsets of {@code location_check} included
   * @return {@code true} when every requirement holds
   */
  public boolean matches(LootContext context, BlockPos pos) {
    if (requirements.isEmpty()) {
      return true;
    }
    Chunk chunk = context.chunkAt(pos).orElse(null);
    if (chunk == null || !chunk.height().contains(pos.y())) {
      return false;
    }
    for (Requirement requirement : requirements) {
      if (!holds(requirement, chunk, context, pos)) {
        return false;
      }
    }
    return true;
  }

  private static boolean holds(
      Requirement requirement, Chunk chunk, LootContext context, BlockPos pos) {
    RegistryAccess access = context.data().registries();
    return switch (requirement) {
      case Requirement.Block block -> block.block().matches(chunk.blockState(pos), access);
      case Requirement.Biomes biomes ->
          Identifier.tryParse(chunk.biome(pos))
              .map(biome -> biomes.biomes().contains(biome, access))
              .orElse(false);
      case Requirement.CanSeeSky sky -> canSeeSky(chunk, pos) == sky.expected();
    };
  }

  private static boolean canSeeSky(Chunk chunk, BlockPos pos) {
    int local = ((pos.y() & 15) << 8) | ((pos.z() & 15) << 4) | (pos.x() & 15);
    return chunk.effectiveSkyLight(Math.floorDiv(pos.y(), 16)).get(local) == FULL_SKY_LIGHT;
  }
}
