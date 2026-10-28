package dev.shoko.minecraft.core.nbt.binary;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.core.nbt.TagType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Optional;

/** Decodes one root from a {@link BinaryInput}; one instance per read. */
final class TagDecoder {

  /** First allocation for a declared element count, grown as elements actually arrive. */
  private static final int INITIAL_ELEMENTS = 1024;

  private final BinaryInput input;

  TagDecoder(BinaryInput input) {
    this.input = input;
  }

  /** Reads type, name and payload; empty for the End byte of an absent root (R5, R20). */
  Optional<NamedTag> readNamed() throws NbtException {
    TagType type = readType();
    if (type == TagType.END) {
      return Optional.empty();
    }
    String name = readString();
    return Optional.of(new NamedTag(name, readPayload(type, 1)));
  }

  /** Reads type and payload, no name (R20). */
  Optional<NbtTag> readUnnamed() throws NbtException {
    TagType type = readType();
    if (type == TagType.END) {
      return Optional.empty();
    }
    return Optional.of(readPayload(type, 1));
  }

  private TagType readType() throws NbtException {
    long at = input.position();
    int id = input.readUnsignedByte();
    return TagType.byId(id)
        .orElseThrow(() -> new NbtException(Reason.UNKNOWN_TYPE, at, "unknown tag type " + id));
  }

  /** Reads a payload; {@code depth} counts lists and compounds, the root container being 1. */
  private NbtTag readPayload(TagType type, int depth) throws NbtException {
    return switch (type) {
      case BYTE -> new ByteTag(input.readByte());
      case SHORT -> new ShortTag(input.readShort());
      case INT -> new IntTag(input.readInt());
      case LONG -> new LongTag(input.readLong());
      case FLOAT -> new FloatTag(input.readInt());
      case DOUBLE -> new DoubleTag(input.readLong());
      case BYTE_ARRAY -> ByteArrayTag.of(input.readBytes(readArrayLength()));
      case STRING -> new StringTag(readString());
      case LIST -> readList(depth);
      case COMPOUND -> readCompound(depth);
      case INT_ARRAY -> readIntArray();
      case LONG_ARRAY -> readLongArray();
      case END -> throw new IllegalStateException("End has no payload");
    };
  }

  private String readString() throws NbtException {
    int length = input.readUnsignedShort();
    long at = input.position();
    return ModifiedUtf8.decode(input.readBytes(length), at);
  }

  private void checkDepth(int depth) throws NbtException {
    if (depth > NbtReader.MAX_DEPTH) {
      throw new NbtException(
          Reason.TOO_DEEP, input.position(), "more than " + NbtReader.MAX_DEPTH + " containers");
    }
  }

  private ListTag readList(int depth) throws NbtException {
    checkDepth(depth);
    long typeOffset = input.position();
    TagType elementType = readType();
    int length = input.readInt();
    if (elementType == TagType.END && length > 0) {
      throw new NbtException(
          Reason.END_LIST_NOT_EMPTY, typeOffset, "list of End with " + length + " elements");
    }
    // A negative length reads as an empty list whatever the element type (R25).
    if (length <= 0) {
      return ListTag.empty();
    }
    var elements = new ArrayList<NbtTag>(Math.min(length, INITIAL_ELEMENTS));
    for (int k = 0; k < length; k++) {
      elements.add(readPayload(elementType, depth + 1));
    }
    return ListTag.of(elements);
  }

  private CompoundTag readCompound(int depth) throws NbtException {
    checkDepth(depth);
    var entries = new LinkedHashMap<String, NbtTag>();
    TagType type = readType();
    while (type != TagType.END) {
      String key = readString();
      // A duplicate key keeps the last value, at the position of its first occurrence (R27).
      entries.put(key, readPayload(type, depth + 1));
      type = readType();
    }
    return CompoundTag.of(entries);
  }

  private int readArrayLength() throws NbtException {
    long at = input.position();
    int length = input.readInt();
    if (length < 0) {
      throw new NbtException(
          Reason.NEGATIVE_ARRAY_LENGTH, at, "array of negative length " + length);
    }
    return length;
  }

  private IntArrayTag readIntArray() throws NbtException {
    int length = readArrayLength();
    int[] values = new int[Math.min(length, INITIAL_ELEMENTS)];
    for (int k = 0; k < length; k++) {
      if (k == values.length) {
        values = Arrays.copyOf(values, (int) Math.min(length, 2L * values.length));
      }
      values[k] = input.readInt();
    }
    return IntArrayTag.of(values);
  }

  private LongArrayTag readLongArray() throws NbtException {
    int length = readArrayLength();
    long[] values = new long[Math.min(length, INITIAL_ELEMENTS)];
    for (int k = 0; k < length; k++) {
      if (k == values.length) {
        values = Arrays.copyOf(values, (int) Math.min(length, 2L * values.length));
      }
      values[k] = input.readLong();
    }
    return LongArrayTag.of(values);
  }
}
