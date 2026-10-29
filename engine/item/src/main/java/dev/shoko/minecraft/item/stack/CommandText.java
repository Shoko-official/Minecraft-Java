package dev.shoko.minecraft.item.stack;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.snbt.SnbtParser;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

/**
 * Splits the text form of the {@code minecraft:item_stack} argument of commands (IC-R29): {@code
 * <id>[<component>=<value>, !<component>, ...]}. The values are SNBT; the splitter only finds where
 * each one ends, then hands it to the SNBT parser of {@code core}.
 */
final class CommandText {

  private final String text;
  private int pos;

  private CommandText(String text) {
    this.text = text;
  }

  /**
   * The parts of a stack text.
   *
   * @param item the item identifier text
   * @param components the component entries in order: a key as written ({@code !} kept) to its
   *     value, an empty compound for a removal
   */
  record Parts(String item, SequencedMap<String, NbtTag> components) {}

  /**
   * Splits a text.
   *
   * @param text the argument text
   * @return the parts
   * @throws FindingException with {@code TEXT_SYNTAX} when the text is malformed
   */
  static Parts split(String text) {
    return new CommandText(text).parse();
  }

  private Parts parse() {
    String item = token();
    if (item.isEmpty()) {
      throw syntax("expected an item identifier");
    }
    SequencedMap<String, NbtTag> components = new LinkedHashMap<>();
    if (peek() == '[') {
      pos++;
      entries(components);
    }
    if (pos != text.length()) {
      throw syntax("unexpected text after the stack");
    }
    return new Parts(item, components);
  }

  private void entries(SequencedMap<String, NbtTag> components) {
    skipBlanks();
    if (peek() == ']') {
      pos++;
      return;
    }
    while (true) {
      entry(components);
      skipBlanks();
      char next = peek();
      pos++;
      if (next == ']') {
        return;
      }
      if (next != ',') {
        throw syntax("expected ',' or ']'");
      }
      skipBlanks();
      if (peek() == ']') {
        pos++;
        return;
      }
    }
  }

  private void entry(SequencedMap<String, NbtTag> components) {
    skipBlanks();
    boolean removal = peek() == '!';
    if (removal) {
      pos++;
    }
    String key = token();
    if (key.isEmpty()) {
      throw syntax("expected a component identifier");
    }
    String name = removal ? "!" + key : key;
    if (removal) {
      components.put(name, CompoundTag.empty());
      return;
    }
    skipBlanks();
    if (peek() != '=') {
      throw syntax("expected '=' after " + key);
    }
    pos++;
    components.put(name, snbt(valueText()));
  }

  private NbtTag snbt(String value) {
    try {
      return SnbtParser.parse(value);
    } catch (NbtException e) {
      throw syntax("bad SNBT value: " + e.getMessage());
    }
  }

  /** Reads up to the comma or bracket that ends the value, outside quotes and nesting. */
  private String valueText() {
    int start = pos;
    int depth = 0;
    char quote = 0;
    while (pos < text.length()) {
      char c = text.charAt(pos);
      if (quote != 0) {
        if (c == '\\') {
          pos++;
        } else if (c == quote) {
          quote = 0;
        }
      } else if (c == '"' || c == '\'') {
        quote = c;
      } else if (c == '{' || c == '[' || c == '(') {
        depth++;
      } else if (depth > 0 && (c == '}' || c == ']' || c == ')')) {
        depth--;
      } else if (depth == 0 && (c == ',' || c == ']')) {
        break;
      }
      pos++;
    }
    if (pos >= text.length()) {
      throw syntax("unterminated value");
    }
    String value = text.substring(start, pos).strip();
    if (value.isEmpty()) {
      throw syntax("expected a value");
    }
    return value;
  }

  private String token() {
    int start = pos;
    while (pos < text.length() && isTokenChar(text.charAt(pos))) {
      pos++;
    }
    return text.substring(start, pos);
  }

  private static boolean isTokenChar(char c) {
    return (c >= 'a' && c <= 'z')
        || (c >= '0' && c <= '9')
        || c == '_'
        || c == '-'
        || c == '.'
        || c == ':'
        || c == '/';
  }

  private char peek() {
    return pos < text.length() ? text.charAt(pos) : 0;
  }

  private void skipBlanks() {
    while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
      pos++;
    }
  }

  private FindingException syntax(String message) {
    return new FindingException(FindingCode.TEXT_SYNTAX, "$[" + pos + "]", message);
  }
}
