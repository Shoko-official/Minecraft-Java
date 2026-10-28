package dev.shoko.minecraft.world.block;

import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.WarningSink;
import dev.shoko.minecraft.world.palette.PaletteEntryCodec;
import dev.shoko.minecraft.world.palette.PaletteFormatException;

/**
 * Converts a block state to and from an entry of a {@code block_states} palette (R16 to R21).
 *
 * <p>Reading (provisional, R19 to R21), each case other than a valid entry also sends a warning:
 *
 * <ul>
 *   <li>a String entry is the default state of that block;
 *   <li>a Compound {@code {id, properties?}}: missing properties take the value of the default
 *       state (R20); the order of keys is irrelevant (R18);
 *   <li>unknown block identifier gives {@code air}; unknown property is ignored; a value outside
 *       the list takes the default state's value for that property (R21);
 *   <li>any other tag type, or a Compound without a String {@code id}, fails.
 * </ul>
 *
 * <p>Writing (R16): a Compound with String {@code id}, and a Compound {@code properties} of String
 * values holding every property of the block, present only if the block has properties; the default
 * state is not elided (R17).
 */
public final class BlockStateEntryCodec implements PaletteEntryCodec<BlockState> {

  private BlockStateEntryCodec() {}

  /**
   * Creates a codec over a registry.
   *
   * @param registry the blocks and states to resolve against
   * @param warnings where the findings of the lenient read go
   * @return the codec
   */
  public static BlockStateEntryCodec of(BlockStateRegistry registry, WarningSink warnings) {
    return Skeleton.throwNotImplemented();
  }

  @Override
  public BlockState decode(NbtTag entry) throws PaletteFormatException {
    return Skeleton.throwNotImplemented();
  }

  @Override
  public NbtTag encode(BlockState value) {
    return Skeleton.throwNotImplemented();
  }
}
