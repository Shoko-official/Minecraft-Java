package dev.shoko.minecraft.world.palette;

import dev.shoko.minecraft.core.nbt.NbtTag;

/**
 * Converts one palette entry between its value and its NBT tag (CF-R11).
 *
 * @param <T> the value type of the container: a block state, a biome identifier
 */
public interface PaletteEntryCodec<T> {

  /**
   * Reads one entry of a palette list.
   *
   * @param entry the tag
   * @return the value; lenient cases (unknown identifier, partial properties) are resolved by the
   *     implementation and reported to its warning sink
   * @throws PaletteFormatException if the tag cannot be an entry at all; an implementation whose
   *     spec never rejects an entry (block states, BS-R22) does not declare it
   */
  T decode(NbtTag entry) throws PaletteFormatException;

  /**
   * Writes one entry.
   *
   * @param value the value
   * @return the tag, of the form the writer produces
   */
  NbtTag encode(T value);
}
