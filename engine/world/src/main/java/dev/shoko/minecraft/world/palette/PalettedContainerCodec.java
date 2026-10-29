package dev.shoko.minecraft.world.palette;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

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

  private static final String PALETTE = "palette";
  private static final String DATA = "data";

  private final ContainerLayout layout;
  private final PaletteEntryCodec<T> entries;

  private PalettedContainerCodec(ContainerLayout layout, PaletteEntryCodec<T> entries) {
    this.layout = layout;
    this.entries = entries;
  }

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
    return new PalettedContainerCodec<>(layout, entries);
  }

  /**
   * Reads a container.
   *
   * @param container the Compound with {@code palette} and {@code data}
   * @return a container of {@code layout.entryCount()} positions
   * @throws PaletteFormatException if the Compound does not follow the form above
   */
  public PalettedContainer<T> decode(CompoundTag container) throws PaletteFormatException {
    List<T> palette = decodePalette(container);
    if (palette.size() == 1) {
      return PalettedContainer.filled(layout.entryCount(), palette.getFirst());
    }
    if (!(container.entries().get(DATA) instanceof LongArrayTag data)) {
      throw new PaletteFormatException(
          "data is missing or not a Long_Array for a palette of " + palette.size() + " entries");
    }
    int expected = layout.longCount(palette.size());
    if (data.size() != expected) {
      throw new PaletteFormatException(
          "data has " + data.size() + " longs, " + expected + " expected");
    }
    int[] indices =
        BitPacking.unpack(data.toArray(), layout.entryCount(), layout.bitsFor(palette.size()));
    for (int position = 0; position < indices.length; position++) {
      if (indices[position] >= palette.size()) {
        throw new PaletteFormatException(
            "index "
                + indices[position]
                + " at position "
                + position
                + " is outside a palette of "
                + palette.size());
      }
    }
    return PalettedContainer.ofPalette(palette, indices);
  }

  private List<T> decodePalette(CompoundTag container) throws PaletteFormatException {
    if (!(container.entries().get(PALETTE) instanceof ListTag list)) {
      throw new PaletteFormatException("palette is missing or not a List");
    }
    int size = list.elements().size();
    if (size < 1) {
      throw new PaletteFormatException("palette is empty");
    }
    if (size > layout.maxPaletteSize()) {
      throw new PaletteFormatException(
          "palette has " + size + " entries, at most " + layout.maxPaletteSize() + " allowed");
    }
    List<T> palette = new ArrayList<>(size);
    for (int k = 0; k < size; k++) {
      try {
        palette.add(entries.decode(list.elements().get(k)));
      } catch (PaletteFormatException e) {
        throw new PaletteFormatException("palette[" + k + "]: " + e.getMessage());
      }
    }
    return palette;
  }

  /**
   * Writes a container.
   *
   * @param container a container of {@code layout.entryCount()} positions
   * @return the Compound with {@code palette} and, if needed, {@code data}
   * @throws IllegalArgumentException if the size differs from the layout
   */
  public CompoundTag encode(PalettedContainer<T> container) {
    if (container.size() != layout.entryCount()) {
      throw new IllegalArgumentException(
          "container has " + container.size() + " positions, " + layout.entryCount() + " expected");
    }
    List<T> distinct = container.distinctValues();
    List<NbtTag> palette = new ArrayList<>(distinct.size());
    Map<T, Integer> slots = new HashMap<>();
    for (T value : distinct) {
      slots.put(value, palette.size());
      palette.add(entries.encode(value));
    }
    SequencedMap<String, NbtTag> result = new LinkedHashMap<>();
    result.put(PALETTE, ListTag.of(palette));
    if (distinct.size() > 1) {
      int[] indices = new int[layout.entryCount()];
      for (int position = 0; position < indices.length; position++) {
        indices[position] = slots.get(container.get(position));
      }
      long[] data = BitPacking.pack(indices, layout.bitsFor(distinct.size()));
      result.put(DATA, LongArrayTag.of(data));
    }
    return CompoundTag.of(result);
  }
}
