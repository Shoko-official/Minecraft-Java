package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;

/**
 * A JSON number. The reader keeps the written form and the exact value (R50): an integer literal
 * (no fraction, no exponent) is an integer, exact on 64 signed bits; anything else is a decimal,
 * converted to the correctly rounded binary64. So {@code 1} and {@code 1.0} differ (R29), and
 * {@code 1.7976931348623157E308} is the largest finite double.
 *
 * @param origin the file
 * @param path the place in the file
 * @param literal the number as written in the file, such as {@code 20}, {@code 1.0} or {@code
 *     2.9802322E-8}
 */
public record JsonNumber(FileOrigin origin, JsonPath path, String literal) implements JsonNode {

  /**
   * Tells whether the literal is in integer form.
   *
   * @return {@code true} for {@code 20}, {@code false} for {@code 1.0} and {@code 1e2}
   */
  public boolean isInteger() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the exact value of an integer literal.
   *
   * @return the value
   * @throws IllegalStateException when the literal is a decimal
   */
  public long longValue() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the value as a double: the correctly rounded binary64 of a decimal, or the nearest
   * double of an integer.
   *
   * @return the value
   */
  public double doubleValue() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@link JsonType#NUMBER}.
   *
   * @return the type
   */
  @Override
  public JsonType type() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Compares form and exact value, not the spelling: {@code 1.0} equals {@code 1.00} and differs
   * from {@code 1}.
   *
   * @param other another node
   * @return whether {@code other} is a number of the same form and value
   */
  @Override
  public boolean sameValueAs(JsonNode other) {
    return Skeleton.throwNotImplemented();
  }
}
