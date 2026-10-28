package dev.shoko.minecraft.world.palette;

import dev.shoko.minecraft.core.nbt.CompoundTag;

/**
 * Converts a {@link PalettedContainer} to and from the Compound stored in a section (CF-R6 to
 * CF-R10).
 *
 * <p>Reading: the Compound has a {@code palette} List (at least one entry) and, when the palette
 * has more than one entry, a {@code data} Long_Array of exactly {@link
 * ContainerLayout#longCount(int)} longs (R6, R7, R8). A single-entry palette fills the container
 * and any {@code data} is ignored (provisional). The following fail with a {@link
 * PaletteFormatException} (R10, provisional): empty palette, palette larger than the layout allows,
 * {@code data} missing or of another length, an index greater than or equal to the palette size, a
 * wrong tag type.
 *
 * <p>Writing: the palette is {@link PalettedContainer#distinctValues()}, compacted; {@code data} is
 * present only for more than one entry, packed with {@link ContainerLayout#bitsFor(int)} bits (R7,
 * R8). The order of palette entries is not imposed.
 *
 * @param <T> the value type
 */
public final class PalettedContainerCodec<T> {

  private PalettedContainerCodec() {}

  /**
   * Creates a codec for one kind of container.
   *
   * @param <T> the value type
   * @param layout sizes of the container
   * @param entries conversion of one palette entry
   * @return the codec
   */
  public static <T> PalettedContainerCodec<T> of(
      ContainerLayout layout, PaletteEntryCodec<T> entries) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a container.
   *
   * @param container the Compound with {@code palette} and {@code data}
   * @return a container of {@code layout.entryCount()} positions
   * @throws PaletteFormatException if the Compound does not follow the form above
   */
  public PalettedContainer<T> decode(CompoundTag container) throws PaletteFormatException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes a container.
   *
   * @param container a container of {@code layout.entryCount()} positions
   * @return the Compound with {@code palette} and, if needed, {@code data}
   * @throws IllegalArgumentException if the size differs from the layout
   */
  public CompoundTag encode(PalettedContainer<T> container) {
    return Skeleton.throwNotImplemented();
  }
}
