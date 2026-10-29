package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;

/**
 * A JSON number. The reader keeps the written form and the exact value (R50, R59). A literal of the
 * form {@code -?(0|[1-9][0-9]*)}, with no point and no exponent, is an integer, exact on 64 signed
 * bits; every other valid literal ({@code 1.0}, {@code 1e5}, {@code -0.0}) is a decimal, converted
 * to the correctly rounded binary64. So {@code 1} and {@code 1.0} differ (R29), and {@code
 * 1.7976931348623157E308} is the largest finite double.
 *
 * <p>The integer {@code -0} has the value 0: a 64-bit integer has no negative zero, so {@code -0}
 * and {@code 0} are the same value. The decimal {@code -0.0} keeps its sign and differs from {@code
 * 0.0} (R59). A decimal literal that underflows ({@code 1e-999}) is the zero of its sign, and
 * denormals are kept.
 *
 * <p>Being a record, {@code equals} compares the written literal, so {@code -0} differs from {@code
 * 0} there; use {@link #sameValueAs(JsonNode)} to compare values.
 *
 * @param origin the file
 * @param path the place in the file
 * @param literal the number as written in the file, such as {@code 20}, {@code 1.0}, {@code -0} or
 *     {@code 2.9802322E-8}
 */
public record JsonNumber(FileOrigin origin, JsonPath path, String literal) implements JsonNode {

  /**
   * Tells whether the literal is in integer form (R59 a).
   *
   * @return {@code true} for {@code 20} and {@code -0}, {@code false} for {@code 1.0}, {@code 1e2}
   *     and {@code -0.0}
   */
  public boolean isInteger() {
    return literal.indexOf('.') < 0 && literal.indexOf('e') < 0 && literal.indexOf('E') < 0;
  }

  /**
   * Returns the exact value of an integer literal.
   *
   * @return the value, 0 for {@code -0}
   * @throws IllegalStateException when the literal is a decimal
   */
  public long longValue() {
    if (!isInteger()) {
      throw new IllegalStateException("not an integer: " + literal);
    }
    return Long.parseLong(literal);
  }

  /**
   * Returns the value as a double: the correctly rounded binary64 of a decimal (round half to
   * even), or the nearest double of an integer. The sign of a decimal zero is kept, so the result
   * for {@code -0.0} has the bits {@code 0x8000000000000000}; the integer {@code -0} gives {@code
   * 0.0}.
   *
   * @return the value
   */
  public double doubleValue() {
    if (isInteger()) {
      // The integer -0 is 0: only a decimal zero keeps its sign.
      double value = Double.parseDouble(literal);
      return value == 0 ? 0.0 : value;
    }
    return Double.parseDouble(literal);
  }

  /**
   * Returns {@link JsonType#NUMBER}.
   *
   * @return the type
   */
  @Override
  public JsonType type() {
    return JsonType.NUMBER;
  }

  /**
   * Compares form and exact value, not the spelling (R59 e): {@code 1.0} equals {@code 1.00} and
   * differs from {@code 1}; integers are equal when their values are ({@code -0} equals {@code 0});
   * decimals are equal when their binary64 bit patterns are ({@code -0.0} differs from {@code
   * 0.0}).
   *
   * @param other another node
   * @return whether {@code other} is a number of the same form and value
   */
  @Override
  public boolean sameValueAs(JsonNode other) {
    if (!(other instanceof JsonNumber that) || isInteger() != that.isInteger()) {
      return false;
    }
    if (isInteger()) {
      return longValue() == that.longValue();
    }
    return Double.doubleToRawLongBits(doubleValue())
        == Double.doubleToRawLongBits(that.doubleValue());
  }
}
