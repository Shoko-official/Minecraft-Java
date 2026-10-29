package dev.shoko.minecraft.world.palette;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A fixed number of positions, each holding a value, stored through a palette of distinct values
 * (CF-R6, CF-R12). The block states of a section use 4096 positions in the order of CF-R4; its
 * biomes use 64 in the order of CF-R5.
 *
 * <p>Values are compared with {@code equals} to merge palette entries. Mutable, confined to the
 * thread that ticks the world. The internal representation is free (CF-R12).
 *
 * @param <T> the value type
 */
public final class PalettedContainer<T> {

  private final int size;
  private final List<T> palette;
  private final Map<T, Integer> slots;
  // Null while every position holds palette slot 0: a uniform section costs no per-position array.
  private int[] indices;

  private PalettedContainer(int size, List<T> palette, Map<T, Integer> slots, int[] indices) {
    this.size = size;
    this.palette = palette;
    this.slots = slots;
    this.indices = indices;
  }

  /**
   * Creates a container where every position holds the same value, a single-entry palette.
   *
   * @param <T> the value type
   * @param size number of positions, positive
   * @param value the value of every position
   * @return the container
   */
  public static <T> PalettedContainer<T> filled(int size, T value) {
    if (size < 1) {
      throw new IllegalArgumentException("size must be positive: " + size);
    }
    Objects.requireNonNull(value, "value");
    List<T> palette = new ArrayList<>();
    palette.add(value);
    Map<T, Integer> slots = new HashMap<>();
    slots.put(value, 0);
    return new PalettedContainer<>(size, palette, slots, null);
  }

  /**
   * Creates a container from a palette and one palette index per position. Equal palette entries
   * are merged, so the palette may repeat a value (two block states corrected to the same state).
   *
   * @param palette the entries, at least one
   * @param indices one index into {@code palette} per position, each in range
   * @return the container
   */
  static <T> PalettedContainer<T> ofPalette(List<T> palette, int[] indices) {
    List<T> merged = new ArrayList<>();
    Map<T, Integer> slots = new HashMap<>();
    int[] remap = new int[palette.size()];
    for (int k = 0; k < remap.length; k++) {
      T value = Objects.requireNonNull(palette.get(k), "palette entry");
      Integer slot = slots.get(value);
      if (slot == null) {
        slot = merged.size();
        merged.add(value);
        slots.put(value, slot);
      }
      remap[k] = slot;
    }
    int[] mapped = new int[indices.length];
    for (int position = 0; position < mapped.length; position++) {
      mapped[position] = remap[indices[position]];
    }
    return new PalettedContainer<>(mapped.length, merged, slots, mapped);
  }

  /**
   * Returns the number of positions.
   *
   * @return the size
   */
  public int size() {
    return size;
  }

  /**
   * Reads one position.
   *
   * @param index position in {@code [0, size())}
   * @return the value
   * @throws IndexOutOfBoundsException if out of range
   */
  public T get(int index) {
    checkIndex(index);
    return palette.get(indices == null ? 0 : indices[index]);
  }

  /**
   * Writes one position.
   *
   * @param index position in {@code [0, size())}
   * @param value the new value
   * @return the previous value
   * @throws IndexOutOfBoundsException if out of range
   */
  public T set(int index, T value) {
    checkIndex(index);
    Objects.requireNonNull(value, "value");
    T previous = get(index);
    if (previous.equals(value)) {
      return previous;
    }
    Integer slot = slots.get(value);
    if (slot == null) {
      slot = palette.size();
      palette.add(value);
      slots.put(value, slot);
    }
    if (indices == null) {
      indices = new int[size];
    }
    indices[index] = slot;
    return previous;
  }

  /**
   * Returns the distinct values currently held, in order of first occurrence by position. The disk
   * form uses exactly these as palette, so no entry is unused (chunk writing).
   *
   * @return an unmodifiable list, never empty
   */
  public List<T> distinctValues() {
    if (indices == null) {
      return List.of(palette.getFirst());
    }
    Set<Integer> seen = new LinkedHashSet<>();
    for (int slot : indices) {
      seen.add(slot);
    }
    List<T> values = new ArrayList<>(seen.size());
    for (int slot : seen) {
      values.add(palette.get(slot));
    }
    return List.copyOf(values);
  }

  /**
   * Tells whether every position holds the same value (CF-R6, palette of one entry).
   *
   * @return {@code true} for a single distinct value
   */
  public boolean isUniform() {
    if (indices == null) {
      return true;
    }
    int first = indices[0];
    for (int slot : indices) {
      if (slot != first) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns an independent container with the same values.
   *
   * @return the copy
   */
  public PalettedContainer<T> copy() {
    if (indices == null) {
      return filled(size, palette.getFirst());
    }
    return ofPalette(palette, indices);
  }

  private void checkIndex(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("position " + index + " of " + size);
    }
  }
}
