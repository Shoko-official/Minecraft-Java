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
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.core.nbt.TagType;
import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Encodes one root in memory, so that a failure (R13) happens before anything reaches the caller's
 * stream. Big-endian (R3).
 */
final class TagEncoder {

  private final ByteArrayOutputStream out = new ByteArrayOutputStream();

  private TagEncoder() {}

  /** Returns type, name and payload (R5). */
  static byte[] named(NamedTag root) throws NbtException {
    var encoder = new TagEncoder();
    encoder.out.write(root.tag().type().id());
    encoder.writeString(root.name());
    encoder.writePayload(root.tag());
    return encoder.out.toByteArray();
  }

  /** Returns type and payload, without name (R20). */
  static byte[] unnamed(NbtTag root) throws NbtException {
    var encoder = new TagEncoder();
    encoder.out.write(root.type().id());
    encoder.writePayload(root);
    return encoder.out.toByteArray();
  }

  private void writePayload(NbtTag tag) throws NbtException {
    switch (tag) {
      case ByteTag t -> out.write(t.value());
      case ShortTag t -> writeShort(t.value());
      case IntTag t -> writeInt(t.value());
      case LongTag t -> writeLong(t.value());
      case FloatTag t -> writeInt(t.bits());
      case DoubleTag t -> writeLong(t.bits());
      case StringTag t -> writeString(t.value());
      case ByteArrayTag t -> {
        writeInt(t.size());
        out.writeBytes(t.toArray());
      }
      case IntArrayTag t -> {
        writeInt(t.size());
        for (int k = 0; k < t.size(); k++) {
          writeInt(t.get(k));
        }
      }
      case LongArrayTag t -> {
        writeInt(t.size());
        for (int k = 0; k < t.size(); k++) {
          writeLong(t.get(k));
        }
      }
      case ListTag t -> writeList(t);
      case CompoundTag t -> writeCompound(t);
    }
  }

  private void writeList(ListTag list) throws NbtException {
    // Empty lists carry End (R22); a heterogeneous list wraps each element under "" (R47).
    boolean wrap =
        list.elementType() == TagType.COMPOUND
            && !list.elements().stream().allMatch(CompoundTag.class::isInstance);
    out.write(list.elementType().id());
    writeInt(list.elements().size());
    for (NbtTag element : list.elements()) {
      if (wrap) {
        writeEntry("", element);
        out.write(TagType.END.id());
      } else {
        writePayload(element);
      }
    }
  }

  private void writeCompound(CompoundTag compound) throws NbtException {
    for (Map.Entry<String, NbtTag> entry : compound.entries().entrySet()) {
      writeEntry(entry.getKey(), entry.getValue());
    }
    out.write(TagType.END.id());
  }

  private void writeEntry(String key, NbtTag value) throws NbtException {
    out.write(value.type().id());
    writeString(key);
    writePayload(value);
  }

  private void writeString(String text) throws NbtException {
    byte[] encoded = ModifiedUtf8.encode(text);
    writeShort(encoded.length);
    out.writeBytes(encoded);
  }

  private void writeShort(int value) {
    out.write(value >>> 8);
    out.write(value);
  }

  private void writeInt(int value) {
    writeShort(value >>> 16);
    writeShort(value);
  }

  private void writeLong(long value) {
    writeInt((int) (value >>> 32));
    writeInt((int) value);
  }
}
