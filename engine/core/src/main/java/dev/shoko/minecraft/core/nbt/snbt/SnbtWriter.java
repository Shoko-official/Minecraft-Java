package dev.shoko.minecraft.core.nbt.snbt;

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
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Formats a value as the SNBT text shown by {@code /data get} (R52 to R56), provisional until probe
 * PW1:
 *
 * <ul>
 *   <li>Numbers: {@code 1b}, {@code 1s}, {@code 1}, {@code 1L}, {@code 1.5f}, {@code 1.5d}; never
 *       {@code true} or {@code false} (R52).
 *   <li>Float and Double digits as {@link Float#toString(float)} and {@link
 *       Double#toString(double)}, then the suffix: {@code 1000.0d}, {@code 1.0E10f}, {@code -0.0d},
 *       {@code NaNd}, {@code -Infinityf} (R53).
 *   <li>Strings always quoted with {@code "}, or with {@code '} when the first quote found in the
 *       string is {@code "}; only the backslash and the chosen quote are escaped; every other
 *       character is written as is (R54).
 *   <li>Keys unquoted when made only of {@code 0-9 A-Z a-z _ - . +}, else quoted as strings (R55).
 *   <li>Separators {@code {a: 1b, b: "x"}}, {@code [1, 2]}, {@code [I; 1, 2]}; empty forms {@code
 *       {}}, {@code []}, {@code [B;]}, {@code [I;]}, {@code [L;]}; keys in storage order;
 *       heterogeneous lists without wrapper, {@code [1, "a"]} (R56).
 * </ul>
 */
public final class SnbtWriter {

  private SnbtWriter() {}

  /**
   * Returns the SNBT text of a value (R52 to R56).
   *
   * @param tag the value
   * @return the text, on a single line
   */
  public static String write(NbtTag tag) {
    var out = new StringBuilder();
    append(out, tag);
    return out.toString();
  }

  private static void append(StringBuilder out, NbtTag tag) {
    switch (tag) {
      case ByteTag t -> out.append(t.value()).append('b');
      case ShortTag t -> out.append(t.value()).append('s');
      case IntTag t -> out.append(t.value());
      case LongTag t -> out.append(t.value()).append('L');
      case FloatTag t -> out.append(Float.toString(t.floatValue())).append('f');
      case DoubleTag t -> out.append(Double.toString(t.doubleValue())).append('d');
      case StringTag t -> appendQuoted(out, t.value());
      case ByteArrayTag t -> appendArray(out, 'B', t.size(), k -> t.get(k) + "b");
      case IntArrayTag t -> appendArray(out, 'I', t.size(), k -> Integer.toString(t.get(k)));
      case LongArrayTag t -> appendArray(out, 'L', t.size(), k -> t.get(k) + "L");
      case ListTag t -> appendList(out, t);
      case CompoundTag t -> appendCompound(out, t);
    }
  }

  private static void appendList(StringBuilder out, ListTag list) {
    out.append('[');
    String separator = "";
    for (NbtTag element : list.elements()) {
      out.append(separator);
      append(out, element);
      separator = ", ";
    }
    out.append(']');
  }

  private static void appendCompound(StringBuilder out, CompoundTag compound) {
    out.append('{');
    String separator = "";
    for (Map.Entry<String, NbtTag> entry : compound.entries().entrySet()) {
      out.append(separator);
      appendKey(out, entry.getKey());
      out.append(": ");
      append(out, entry.getValue());
      separator = ", ";
    }
    out.append('}');
  }

  private static void appendArray(
      StringBuilder out, char kind, int size, IntFunction<String> element) {
    out.append('[').append(kind).append(';');
    for (int k = 0; k < size; k++) {
      out.append(k == 0 ? " " : ", ").append(element.apply(k));
    }
    out.append(']');
  }

  private static void appendKey(StringBuilder out, String key) {
    boolean plain =
        !key.isEmpty() && key.chars().allMatch(c -> SnbtCursor.isUnquotedChar((char) c));
    if (plain) {
      out.append(key);
    } else {
      appendQuoted(out, key);
    }
  }

  private static void appendQuoted(StringBuilder out, String text) {
    char quote = '"';
    for (int k = 0; k < text.length(); k++) {
      char c = text.charAt(k);
      if (c == '"' || c == '\'') {
        quote = c == '"' ? '\'' : '"';
        break;
      }
    }
    out.append(quote);
    for (int k = 0; k < text.length(); k++) {
      char c = text.charAt(k);
      if (c == '\\' || c == quote) {
        out.append('\\');
      }
      out.append(c);
    }
    out.append(quote);
  }
}
