package dev.shoko.minecraft.core.nbt.snbt;

import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;

/** Position in an SNBT text, with the character classes of R41 and R43. */
final class SnbtCursor {

  private final String text;
  private int position;

  SnbtCursor(String text) {
    this.text = text;
  }

  /** Characters allowed in unquoted strings and keys: {@code 0-9 A-Z a-z _ - . +} (R43, R55). */
  static boolean isUnquotedChar(char c) {
    return (c >= '0' && c <= '9')
        || (c >= 'A' && c <= 'Z')
        || (c >= 'a' && c <= 'z')
        || c == '_'
        || c == '-'
        || c == '.'
        || c == '+';
  }

  /** Blanks allowed between tokens: space, tab, carriage return, line feed (R41, provisional). */
  static boolean isBlank(char c) {
    return c == ' ' || c == '\t' || c == '\r' || c == '\n';
  }

  int position() {
    return position;
  }

  boolean atEnd() {
    return position >= text.length();
  }

  /** Returns the current character; only valid when not {@link #atEnd()}. */
  char peek() {
    return text.charAt(position);
  }

  /** Whether the text at the current position starts with {@code prefix}. */
  boolean startsWith(String prefix) {
    return text.startsWith(prefix, position);
  }

  char next() {
    return text.charAt(position++);
  }

  void skipBlanks() {
    while (!atEnd() && isBlank(peek())) {
      position++;
    }
  }

  /** Reads the longest run of unquoted characters, possibly empty. */
  String readUnquoted() {
    int start = position;
    while (!atEnd() && isUnquotedChar(peek())) {
      position++;
    }
    return text.substring(start, position);
  }

  /** Consumes {@code expected} or fails with SNBT_SYNTAX at the current position. */
  void expect(char expected) throws NbtException {
    if (atEnd() || peek() != expected) {
      throw syntax("expected '" + expected + "'");
    }
    position++;
  }

  NbtException syntax(String message) {
    return error(Reason.SNBT_SYNTAX, position, message);
  }

  NbtException error(Reason reason, int offset, String message) {
    String found = offset < text.length() ? "'" + text.charAt(offset) + "'" : "end of text";
    return new NbtException(reason, offset, message + " at " + offset + ", found " + found);
  }
}
