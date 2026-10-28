package dev.shoko.minecraft.data.json;

import java.util.HashMap;
import java.util.Map;

/**
 * Takes the long numeric literals out of a text before the library reads it. The streaming reader
 * of the library mishandles them in strict mode: it accumulates the digits of an integer in a
 * {@code long} and refuses a literal such as {@code 1} followed by 70 zeros as a leading-zero
 * number once the accumulator wraps to 0, and it gives up on any literal longer than its buffer.
 * R59 f asks for no limit on the number of digits, so a literal longer than {@link #SHORT_LIMIT}
 * characters is replaced by {@code 0} in the text given to the library and read back here by its
 * rank among the numbers of the text.
 */
final class LongNumbers {

  /** Longest literal the library reads correctly: 18 digits always fit in a {@code long}. */
  private static final int SHORT_LIMIT = 18;

  private static final String PLACEHOLDER = "0";

  private final String libraryText;
  // Rank among the numbers of the text -> literal; looked up by key only.
  private final Map<Integer, String> literals;

  private LongNumbers(String libraryText, Map<Integer, String> literals) {
    this.libraryText = libraryText;
    this.literals = literals;
  }

  /**
   * Scans a text. Strings are skipped; outside them a run of characters that are neither blank nor
   * structural is a number when it starts with {@code -} or a digit.
   */
  static LongNumbers scan(String text) {
    Map<Integer, String> literals = new HashMap<>();
    StringBuilder library = null;
    int copied = 0;
    int rank = 0;
    int i = 0;
    while (i < text.length()) {
      char c = text.charAt(i);
      if (c == '"') {
        i = endOfString(text, i);
      } else if (isStructural(c)) {
        i++;
      } else {
        int end = endOfRun(text, i);
        if (c == '-' || (c >= '0' && c <= '9')) {
          if (end - i > SHORT_LIMIT) {
            literals.put(rank, text.substring(i, end));
            if (library == null) {
              library = new StringBuilder(text.length());
            }
            library.append(text, copied, i).append(PLACEHOLDER);
            copied = end;
          }
          rank++;
        }
        i = end;
      }
    }
    if (library == null) {
      return new LongNumbers(text, literals);
    }
    return new LongNumbers(library.append(text, copied, text.length()).toString(), literals);
  }

  /** The text to give to the library. */
  String libraryText() {
    return libraryText;
  }

  /** The long literal that is the number of this rank, or null when the library reads it. */
  String literalAt(int rank) {
    return literals.get(rank);
  }

  /** Tells whether a literal follows the JSON number grammar. */
  static boolean isNumber(String literal) {
    int i = 0;
    int n = literal.length();
    if (i < n && literal.charAt(i) == '-') {
      i++;
    }
    if (i >= n || !isDigit(literal.charAt(i))) {
      return false;
    }
    if (literal.charAt(i) == '0') {
      i++;
    } else {
      i = skipDigits(literal, i);
    }
    if (i < n && literal.charAt(i) == '.') {
      int fraction = skipDigits(literal, i + 1);
      if (fraction == i + 1) {
        return false;
      }
      i = fraction;
    }
    if (i < n && (literal.charAt(i) == 'e' || literal.charAt(i) == 'E')) {
      int start = i + 1;
      if (start < n && (literal.charAt(start) == '+' || literal.charAt(start) == '-')) {
        start++;
      }
      int exponent = skipDigits(literal, start);
      if (exponent == start) {
        return false;
      }
      i = exponent;
    }
    return i == n;
  }

  private static int skipDigits(String text, int from) {
    int i = from;
    while (i < text.length() && isDigit(text.charAt(i))) {
      i++;
    }
    return i;
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isStructural(char c) {
    return c == ' ' || c == '\t' || c == '\n' || c == '\r' || "{}[],:".indexOf(c) >= 0;
  }

  private static int endOfRun(String text, int from) {
    int i = from;
    while (i < text.length() && !isStructural(text.charAt(i)) && text.charAt(i) != '"') {
      i++;
    }
    return i;
  }

  private static int endOfString(String text, int quote) {
    int i = quote + 1;
    while (i < text.length()) {
      char c = text.charAt(i);
      if (c == '\\') {
        i += 2;
      } else if (c == '"') {
        return i + 1;
      } else {
        i++;
      }
    }
    return text.length();
  }
}
