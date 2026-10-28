package dev.shoko.minecraft.core.nbt.snbt;

import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;

/** Reads a quoted SNBT string and its escapes (R45). */
final class SnbtStrings {

  private SnbtStrings() {}

  /**
   * Reads a string quoted with {@code "} or {@code '}, the cursor being on the opening quote.
   *
   * @throws NbtException SNBT_SYNTAX when the closing quote is missing, SNBT_INVALID_ESCAPE at the
   *     backslash of an invalid escape
   */
  static String readQuoted(SnbtCursor cursor) throws NbtException {
    char quote = cursor.next();
    var out = new StringBuilder();
    while (true) {
      if (cursor.atEnd()) {
        throw cursor.syntax("unterminated string");
      }
      char c = cursor.next();
      if (c == quote) {
        return out.toString();
      }
      if (c == '\\') {
        readEscape(cursor, cursor.position() - 1, out);
      } else {
        out.append(c);
      }
    }
  }

  private static void readEscape(SnbtCursor cursor, int backslash, StringBuilder out)
      throws NbtException {
    if (cursor.atEnd()) {
      throw cursor.syntax("unterminated string");
    }
    char c = cursor.next();
    switch (c) {
      case '\\', '"', '\'' -> out.append(c);
      case 'n' -> out.append('\n');
      case 's' -> out.append(' ');
      case 't' -> out.append('\t');
      case 'b' -> out.append('\b');
      case 'f' -> out.append('\f');
      case 'r' -> out.append('\r');
      case 'x' -> out.appendCodePoint((int) hex(cursor, 2, backslash));
      case 'u' -> out.appendCodePoint((int) hex(cursor, 4, backslash));
      case 'U' -> out.appendCodePoint(codePoint(hex(cursor, 8, backslash), cursor, backslash));
      case 'N' -> out.appendCodePoint(named(cursor, backslash));
      default -> throw invalid(cursor, backslash, "unknown escape \\" + c);
    }
  }

  private static long hex(SnbtCursor cursor, int digits, int backslash) throws NbtException {
    long value = 0;
    for (int k = 0; k < digits; k++) {
      int digit = cursor.atEnd() ? -1 : Character.digit(cursor.next(), 16);
      if (digit < 0) {
        throw invalid(cursor, backslash, "escape needs " + digits + " hexadecimal digits");
      }
      value = (value << 4) | digit;
    }
    return value;
  }

  private static int codePoint(long value, SnbtCursor cursor, int backslash) throws NbtException {
    if (value > Character.MAX_CODE_POINT) {
      throw invalid(cursor, backslash, "code point above U+10FFFF");
    }
    return (int) value;
  }

  /** Reads {@code {name}} after {@code \N} and returns the code point of that Unicode name. */
  private static int named(SnbtCursor cursor, int backslash) throws NbtException {
    if (cursor.atEnd() || cursor.next() != '{') {
      throw invalid(cursor, backslash, "expected '{' after \\N");
    }
    var name = new StringBuilder();
    while (!cursor.atEnd() && cursor.peek() != '}') {
      name.append(cursor.next());
    }
    if (cursor.atEnd()) {
      throw invalid(cursor, backslash, "unterminated Unicode name");
    }
    cursor.next();
    try {
      return Character.codePointOf(name.toString());
    } catch (IllegalArgumentException e) {
      throw invalid(cursor, backslash, "unknown Unicode name " + name);
    }
  }

  private static NbtException invalid(SnbtCursor cursor, int backslash, String message) {
    return cursor.error(Reason.SNBT_INVALID_ESCAPE, backslash, message);
  }
}
