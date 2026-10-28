package dev.shoko.minecraft.core.nbt;

/**
 * An immutable NBT value of one of the 12 value types; {@link TagType#END} has no value.
 *
 * <p>Equality (R37), implemented by every type: the types must be the same ({@code Int 1} differs
 * from {@code Byte 1}); compounds compare keys and values regardless of key order (R28); lists
 * compare elements in order, and two empty lists are equal; arrays compare element by element;
 * floats and doubles compare bit patterns ({@code 0.0} differs from {@code -0.0}, a NaN equals the
 * same NaN pattern only). {@code hashCode} is consistent with this equality.
 */
public sealed interface NbtTag
    permits NumericTag, StringTag, ByteArrayTag, IntArrayTag, LongArrayTag, ListTag, CompoundTag {

  /**
   * Returns the binary type of this value (R1, R2).
   *
   * @return the type, never {@link TagType#END}
   */
  TagType type();
}
