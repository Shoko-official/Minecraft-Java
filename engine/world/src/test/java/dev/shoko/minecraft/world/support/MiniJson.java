package dev.shoko.minecraft.world.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON reader for the oracle tests, which read the report of the reference data. The
 * module has no JSON library: objects become ordered maps, arrays lists, integers longs.
 */
public final class MiniJson {

  private final String text;
  private int at;

  private MiniJson(String text) {
    this.text = text;
  }

  public static Object parse(String text) {
    MiniJson reader = new MiniJson(text);
    Object value = reader.value();
    reader.skipSpace();
    if (reader.at != text.length()) {
      throw new IllegalArgumentException("text after the value at " + reader.at);
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> object(Object value) {
    return (Map<String, Object>) value;
  }

  @SuppressWarnings("unchecked")
  public static List<Object> array(Object value) {
    return (List<Object>) value;
  }

  private void skipSpace() {
    while (at < text.length() && Character.isWhitespace(text.charAt(at))) {
      at++;
    }
  }

  private Object value() {
    skipSpace();
    char c = text.charAt(at);
    return switch (c) {
      case '{' -> readObject();
      case '[' -> readArray();
      case '"' -> readString();
      case 't' -> literal("true", true);
      case 'f' -> literal("false", false);
      case 'n' -> literal("null", null);
      default -> readNumber();
    };
  }

  private Object literal(String word, Object value) {
    if (!text.startsWith(word, at)) {
      throw new IllegalArgumentException("bad literal at " + at);
    }
    at += word.length();
    return value;
  }

  private Map<String, Object> readObject() {
    Map<String, Object> map = new LinkedHashMap<>();
    at++;
    skipSpace();
    if (text.charAt(at) == '}') {
      at++;
      return map;
    }
    while (true) {
      skipSpace();
      String key = readString();
      skipSpace();
      expect(':');
      map.put(key, value());
      skipSpace();
      if (text.charAt(at) == ',') {
        at++;
      } else {
        expect('}');
        return map;
      }
    }
  }

  private List<Object> readArray() {
    List<Object> list = new ArrayList<>();
    at++;
    skipSpace();
    if (text.charAt(at) == ']') {
      at++;
      return list;
    }
    while (true) {
      list.add(value());
      skipSpace();
      if (text.charAt(at) == ',') {
        at++;
      } else {
        expect(']');
        return list;
      }
    }
  }

  private void expect(char c) {
    if (text.charAt(at) != c) {
      throw new IllegalArgumentException("expected " + c + " at " + at);
    }
    at++;
  }

  private String readString() {
    expect('"');
    StringBuilder out = new StringBuilder();
    while (text.charAt(at) != '"') {
      char c = text.charAt(at++);
      if (c != '\\') {
        out.append(c);
        continue;
      }
      char escape = text.charAt(at++);
      switch (escape) {
        case 'n' -> out.append('\n');
        case 't' -> out.append('\t');
        case 'r' -> out.append('\r');
        case 'b' -> out.append('\b');
        case 'f' -> out.append('\f');
        case 'u' -> {
          out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
          at += 4;
        }
        default -> out.append(escape);
      }
    }
    at++;
    return out.toString();
  }

  private Object readNumber() {
    int start = at;
    while (at < text.length() && "+-0123456789.eE".indexOf(text.charAt(at)) >= 0) {
      at++;
    }
    String number = text.substring(start, at);
    if (number.isEmpty()) {
      throw new IllegalArgumentException("bad value at " + start);
    }
    return number.contains(".") || number.contains("e") || number.contains("E")
        ? (Object) Double.parseDouble(number)
        : (Object) Long.parseLong(number);
  }
}
