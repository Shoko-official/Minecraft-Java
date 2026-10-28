package dev.shoko.minecraft.core.nbt.snbt;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import java.math.BigInteger;
import java.util.Locale;

/**
 * Parses one unquoted SNBT word that starts like a number (R43, R46), provisional until probe PS1:
 *
 * <pre>
 * number  = [+-] ( ("0x" hex+ | "0b" bin+) [sign] int-type
 *                | digits ["." digits] [("e"|"E") [+-] digits] [type] )
 * sign    = s | u          (before an integer type only: 240ub, 0x11sb)
 * type    = b | s | i | l  (integers) | f | d (decimals), any case
 * </pre>
 *
 * <p>{@code _} is allowed between two digits only. Without '.' or exponent the number is an integer
 * (Int without suffix), with them a Double unless suffixed {@code f}. A value outside its type, or
 * a decimal overflowing to infinity, fails with SNBT_NUMBER_OUT_OF_RANGE; anything else that does
 * not match fails with SNBT_SYNTAX, both at the first character of the word.
 */
final class SnbtNumbers {

  private final String word;
  private final int offset;
  private int index;

  private SnbtNumbers(String word, int offset) {
    this.word = word;
    this.offset = offset;
  }

  /** Whether an unquoted word starting with {@code c} must be a number (R43). */
  static boolean startsNumber(char c) {
    return (c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.';
  }

  /**
   * Parses {@code word}, which starts at {@code offset} in the text.
   *
   * @throws NbtException SNBT_SYNTAX or SNBT_NUMBER_OUT_OF_RANGE at {@code offset}
   */
  static NbtTag parse(String word, int offset) throws NbtException {
    return new SnbtNumbers(word, offset).parse();
  }

  private NbtTag parse() throws NbtException {
    boolean negative = false;
    if (index < word.length() && (word.charAt(index) == '-' || word.charAt(index) == '+')) {
      negative = word.charAt(index) == '-';
      index++;
    }
    int radix = prefixRadix();
    if (radix != 10) {
      String digits = digits(radix);
      if (digits.isEmpty()) {
        throw syntax();
      }
      return integer(new BigInteger(digits, radix), negative, word.substring(index));
    }
    String whole = digits(10);
    String fraction = null;
    String exponent = null;
    if (accept('.')) {
      fraction = digits(10);
    }
    if (accept('e') || accept('E')) {
      String sign = accept('-') ? "-" : "";
      if (sign.isEmpty()) {
        accept('+');
      }
      String digits = digits(10);
      if (digits.isEmpty()) {
        throw syntax();
      }
      exponent = sign + digits;
    }
    if (whole.isEmpty() && (fraction == null || fraction.isEmpty())) {
      throw syntax();
    }
    String suffix = word.substring(index);
    if (fraction == null && exponent == null && !isDecimalSuffix(suffix)) {
      return integer(new BigInteger(whole), negative, suffix);
    }
    String text =
        (negative ? "-" : "")
            + (whole.isEmpty() ? "0" : whole)
            + (fraction == null ? "" : "." + (fraction.isEmpty() ? "0" : fraction))
            + (exponent == null ? "" : "e" + exponent);
    return decimal(text, suffix);
  }

  /** Consumes a {@code 0x} or {@code 0b} prefix and returns its radix, else returns 10. */
  private int prefixRadix() {
    if (word.startsWith("0x", index) || word.startsWith("0X", index)) {
      index += 2;
      return 16;
    }
    // "0b" alone is the Byte 0: a binary prefix needs a binary digit after it.
    boolean binary = word.startsWith("0b", index) || word.startsWith("0B", index);
    if (binary && index + 2 < word.length() && Character.digit(word.charAt(index + 2), 2) >= 0) {
      index += 2;
      return 2;
    }
    return 10;
  }

  /** Reads digits of {@code radix} with single {@code _} between digits; returns them without. */
  private String digits(int radix) throws NbtException {
    var out = new StringBuilder();
    boolean afterUnderscore = false;
    while (index < word.length()) {
      char c = word.charAt(index);
      if (c == '_') {
        if (out.isEmpty() || afterUnderscore) {
          throw syntax();
        }
        afterUnderscore = true;
      } else if (Character.digit(c, radix) >= 0) {
        out.append(c);
        afterUnderscore = false;
      } else {
        break;
      }
      index++;
    }
    if (afterUnderscore) {
      throw syntax();
    }
    return out.toString();
  }

  private boolean accept(char c) {
    if (index < word.length() && word.charAt(index) == c) {
      index++;
      return true;
    }
    return false;
  }

  private static boolean isDecimalSuffix(String suffix) {
    return suffix.equalsIgnoreCase("f") || suffix.equalsIgnoreCase("d");
  }

  private NbtTag integer(BigInteger magnitude, boolean negative, String suffix)
      throws NbtException {
    String lower = suffix.toLowerCase(Locale.ROOT);
    boolean signSuffix = lower.length() == 2;
    boolean unsigned = signSuffix && lower.charAt(0) == 'u';
    if (lower.length() > 2 || (signSuffix && !unsigned && lower.charAt(0) != 's')) {
      throw syntax();
    }
    char type = lower.isEmpty() ? 'i' : lower.charAt(lower.length() - 1);
    int bits =
        switch (type) {
          case 'b' -> Byte.SIZE;
          case 's' -> Short.SIZE;
          case 'i' -> Integer.SIZE;
          case 'l' -> Long.SIZE;
          default -> throw syntax();
        };
    BigInteger value = negative ? magnitude.negate() : magnitude;
    boolean inRange =
        unsigned ? value.signum() >= 0 && value.bitLength() <= bits : value.bitLength() <= bits - 1;
    if (!inRange) {
      throw outOfRange();
    }
    // An unsigned value is stored as the same bits in the signed type (240ub is Byte -16).
    long low = value.longValue();
    return switch (type) {
      case 'b' -> new ByteTag((byte) low);
      case 's' -> new ShortTag((short) low);
      case 'i' -> new IntTag((int) low);
      default -> new LongTag(low);
    };
  }

  private NbtTag decimal(String text, String suffix) throws NbtException {
    if (suffix.equalsIgnoreCase("f")) {
      float value = Float.parseFloat(text);
      if (Float.isInfinite(value)) {
        throw outOfRange();
      }
      return FloatTag.of(value);
    }
    if (!suffix.isEmpty() && !suffix.equalsIgnoreCase("d")) {
      throw syntax();
    }
    double value = Double.parseDouble(text);
    if (Double.isInfinite(value)) {
      throw outOfRange();
    }
    return DoubleTag.of(value);
  }

  private NbtException syntax() {
    return new NbtException(Reason.SNBT_SYNTAX, offset, "malformed number '" + word + "'");
  }

  private NbtException outOfRange() {
    return new NbtException(
        Reason.SNBT_NUMBER_OUT_OF_RANGE, offset, "number out of range '" + word + "'");
  }
}
