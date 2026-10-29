package dev.shoko.minecraft.item.component;

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
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Writes canonical NBT as compact JSON text that the JSON reader of {@code data} reads back (the
 * inverse of {@link JsonInput#any}). A {@code Byte} of 0 or 1 is a boolean, a float is written as
 * the exact decimal of its double so that no bit is lost (IC-R12). Numbers that JSON cannot hold
 * (NaN, infinities) and NBT arrays have no faithful JSON form: infinities and NaN are refused, the
 * arrays are written as lists of integers.
 */
final class TagJson {

  private TagJson() {}

  /**
   * Writes a tag.
   *
   * @param tag the tag
   * @return the JSON text
   * @throws IllegalArgumentException when a number is NaN or infinite
   */
  static String write(NbtTag tag) {
    StringBuilder out = new StringBuilder();
    append(out, tag);
    return out.toString();
  }

  /**
   * Writes a string as a JSON string literal, escaping what JSON requires and the unpaired
   * surrogates that the reader refuses in raw text.
   *
   * @param text the text
   * @return the quoted literal
   */
  static String quote(String text) {
    StringBuilder out = new StringBuilder(text.length() + 2).append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      boolean pairedHigh =
          Character.isHighSurrogate(c)
              && i + 1 < text.length()
              && Character.isLowSurrogate(text.charAt(i + 1));
      if (pairedHigh) {
        out.append(c).append(text.charAt(++i));
      } else if (c == '"' || c == '\\') {
        out.append('\\').append(c);
      } else if (c < ' ' || Character.isSurrogate(c)) {
        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
      } else {
        out.append(c);
      }
    }
    return out.append('"').toString();
  }

  private static void append(StringBuilder out, NbtTag tag) {
    switch (tag) {
      case ByteTag t -> out.append(byteText(t.value()));
      case ShortTag t -> out.append(t.value());
      case IntTag t -> out.append(t.value());
      case LongTag t -> out.append(t.value());
      case FloatTag t -> out.append(decimal(t.floatValue()));
      case DoubleTag t -> out.append(decimal(t.doubleValue()));
      case StringTag t -> out.append(quote(t.value()));
      case ByteArrayTag t -> appendArray(out, t.size(), k -> Byte.toString(t.get(k)));
      case IntArrayTag t -> appendArray(out, t.size(), k -> Integer.toString(t.get(k)));
      case LongArrayTag t -> appendArray(out, t.size(), k -> Long.toString(t.get(k)));
      case ListTag t -> appendList(out, t);
      case CompoundTag t -> appendCompound(out, t);
    }
  }

  private static String byteText(byte value) {
    return value == 0 || value == 1 ? Boolean.toString(value == 1) : Byte.toString(value);
  }

  private static String decimal(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException("JSON cannot hold " + value);
    }
    return Double.toString(value);
  }

  private static void appendArray(StringBuilder out, int size, IntFunction<String> element) {
    out.append('[');
    for (int k = 0; k < size; k++) {
      out.append(k == 0 ? "" : ",").append(element.apply(k));
    }
    out.append(']');
  }

  private static void appendList(StringBuilder out, ListTag list) {
    out.append('[');
    String separator = "";
    for (NbtTag element : list.elements()) {
      out.append(separator);
      append(out, element);
      separator = ",";
    }
    out.append(']');
  }

  private static void appendCompound(StringBuilder out, CompoundTag compound) {
    out.append('{');
    String separator = "";
    for (Map.Entry<String, NbtTag> entry : compound.entries().entrySet()) {
      out.append(separator).append(quote(entry.getKey())).append(':');
      append(out, entry.getValue());
      separator = ",";
    }
    out.append('}');
  }
}
