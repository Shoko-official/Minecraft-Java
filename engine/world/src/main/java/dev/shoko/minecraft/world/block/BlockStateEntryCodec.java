package dev.shoko.minecraft.world.block;

import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.WarningSink;
import dev.shoko.minecraft.world.palette.PaletteEntryCodec;

/**
 * Converts a block state to and from an entry of a {@code block_states} palette (R16 to R22).
 *
 * <p>Reading (provisional, R19 to R22) never fails and never refuses a chunk: no invalid entry does
 * (R22 b). Each case other than a valid entry sends one warning, which names the block and the
 * property concerned (R22 c):
 *
 * <ul>
 *   <li>a String entry is the default state of that block;
 *   <li>a Compound {@code {id, properties?}}: each missing property takes the value of the default
 *       state, {@code DEFAULTED_PROPERTY} (R20); the order of keys is irrelevant (R18);
 *   <li>unknown block identifier gives {@code air}, {@code UNKNOWN_BLOCK}; unknown property is
 *       ignored, {@code UNKNOWN_PROPERTY}; a value outside the list takes the default state's value
 *       for that property, {@code INVALID_PROPERTY_VALUE} (R21);
 *   <li>any other tag type, or a Compound without a String {@code id}, is treated like an unknown
 *       block: {@code air} and {@code MALFORMED_PALETTE_ENTRY} (R22 a).
 * </ul>
 *
 * <p>Two entries corrected to the same state stay two entries of the palette, so indices stay valid
 * (R22 d).
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
   * @param warnings where the findings of the lenient read go, with the codes above
   * @return the codec
   */
  public static BlockStateEntryCodec of(BlockStateRegistry registry, WarningSink warnings) {
    return Skeleton.throwNotImplemented();
  }

  @Override
  public BlockState decode(NbtTag entry) {
    return Skeleton.throwNotImplemented();
  }

  @Override
  public NbtTag encode(BlockState value) {
    return Skeleton.throwNotImplemented();
  }
}
