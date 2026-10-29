package dev.shoko.minecraft.item.component;

import java.util.List;

/**
 * The shape of the value of one component: what the decoder accepts and the canonical NBT it
 * produces (IC-R12, IC-R13). Integers become {@code Int}, floats {@code Float}, doubles {@code
 * Double}, booleans {@code Byte} 0 or 1, identifiers and text {@code String}.
 */
sealed interface Shape {

  /** Any string. */
  record Str() implements Shape {}

  /** A string with the syntax of an identifier, written in its normal form. */
  record Ident() implements Shape {}

  /**
   * An identifier string, one prefixed by {@code #} for a tag, or a list of identifier strings (a
   * holder set).
   */
  record IdSet() implements Shape {}

  /** An integer that fits an {@code Int}. */
  record IntS() implements Shape {}

  /** A number read as a double, then converted to a {@code Float} (IC-R13). */
  record FloatS() implements Shape {}

  /** A number kept as a {@code Double}. */
  record DoubleS() implements Shape {}

  /** A boolean. */
  record BoolS() implements Shape {}

  /**
   * A list whose elements all have one shape.
   *
   * @param element the shape of each element
   */
  record ListOf(Shape element) implements Shape {}

  /**
   * An object with known fields.
   *
   * @param fields the accepted fields
   */
  record Obj(List<Field> fields) implements Shape {}

  /**
   * An object whose keys are identifiers and whose values have one shape.
   *
   * @param value the shape of each value
   */
  record MapOf(Shape value) implements Shape {}

  /** Any value that NBT can hold, kept as is. */
  record AnyValue() implements Shape {}

  /** Any object. */
  record AnyCompound() implements Shape {}

  /**
   * One field of an {@link Obj}.
   *
   * @param name the key
   * @param shape the shape of the value
   * @param required whether the key must be present
   */
  record Field(String name, Shape shape, boolean required) {}
}
