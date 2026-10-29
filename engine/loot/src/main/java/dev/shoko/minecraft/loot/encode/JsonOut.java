package dev.shoko.minecraft.loot.encode;

import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNull;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/**
 * A small JSON tree and writer for the encoders. A tree is made of {@code Map} (an object, in
 * insertion order), {@code List}, {@code String}, {@code Boolean} and {@link Literal} numbers.
 */
public final class JsonOut {

  /** A number written as is. */
  record Literal(String text) {}

  private JsonOut() {}

  /** Starts an object. */
  static SequencedMap<String, Object> object() {
    return new LinkedHashMap<>();
  }

  /** The literal of an integer. */
  static Literal number(long value) {
    return new Literal(Long.toString(value));
  }

  /** The literal of a 32-bit float, always in decimal form. */
  static Literal number(float value) {
    return new Literal(Float.toString(value));
  }

  /** The literal of a double: an integer form when the value is integral, else a decimal one. */
  static Literal number(double value) {
    if (value == Math.rint(value) && Math.abs(value) < 1e15) {
      return new Literal(Long.toString((long) value));
    }
    return new Literal(Double.toString(value));
  }

  /** Copies a decoded JSON node into a tree, keeping the integer and decimal forms. */
  static Object tree(JsonNode node) {
    return switch (node) {
      case JsonObject object -> {
        SequencedMap<String, Object> copy = object();
        object.members().forEach((key, value) -> copy.put(key, tree(value)));
        yield copy;
      }
      case JsonArray array -> array.elements().stream().map(JsonOut::tree).toList();
      case JsonString string -> string.value();
      case JsonNumber number -> new Literal(number.literal());
      case JsonBoolean bool -> bool.value();
      case JsonNull none -> new Literal("null");
    };
  }

  /**
   * Writes a tree as compact JSON.
   *
   * @param tree a tree
   * @return the text
   */
  public static String write(Object tree) {
    StringBuilder out = new StringBuilder();
    write(tree, out);
    return out.toString();
  }

  private static void write(Object tree, StringBuilder out) {
    switch (tree) {
      case Map<?, ?> map -> {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          if (!first) {
            out.append(',');
          }
          first = false;
          string(entry.getKey().toString(), out);
          out.append(':');
          write(entry.getValue(), out);
        }
        out.append('}');
      }
      case List<?> list -> {
        out.append('[');
        for (int i = 0; i < list.size(); i++) {
          if (i > 0) {
            out.append(',');
          }
          write(list.get(i), out);
        }
        out.append(']');
      }
      case String text -> string(text, out);
      case Literal literal -> out.append(literal.text());
      default -> out.append(tree);
    }
  }

  private static void string(String text, StringBuilder out) {
    out.append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    out.append('"');
  }
}
