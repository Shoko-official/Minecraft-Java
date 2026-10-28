/**
 * Paletted containers of block states and biomes, and their storage in packed longs.
 *
 * <p>Spec: {@code docs/specs/world-chunk-format.md}; rule numbers Rn in the Javadoc refer to it.
 *
 * <ul>
 *   <li>Packing into longs: {@link dev.shoko.minecraft.world.palette.BitPacking} (R8), sizes of a
 *       kind of container: {@link dev.shoko.minecraft.world.palette.ContainerLayout} (R7, R9).
 *   <li>The in-memory container: {@link dev.shoko.minecraft.world.palette.PalettedContainer}.
 *   <li>The disk form, a Compound {@code palette} and {@code data}: {@link
 *       dev.shoko.minecraft.world.palette.PalettedContainerCodec} (R6 to R10), with the NBT form of
 *       one entry given by a {@link dev.shoko.minecraft.world.palette.PaletteEntryCodec}.
 * </ul>
 *
 * <p>The in-memory representation is free as long as the disk form follows R6 to R8 (R12). This
 * package knows neither blocks nor biomes.
 */
package dev.shoko.minecraft.world.palette;
