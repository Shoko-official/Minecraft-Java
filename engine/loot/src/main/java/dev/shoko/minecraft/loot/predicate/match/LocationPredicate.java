package dev.shoko.minecraft.loot.predicate.match;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.loot.context.LootContext;
import java.util.List;

/**
 * The position predicate of {@code location_check.predicate} and of the {@code minecraft:location}
 * key of an entity predicate (LT-R59). All requirements must hold.
 *
 * @param requirements the keys present, in file order, unmodifiable
 */
public record LocationPredicate(List<Requirement> requirements) {

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
    return Skeleton.throwNotImplemented();
  }
}
