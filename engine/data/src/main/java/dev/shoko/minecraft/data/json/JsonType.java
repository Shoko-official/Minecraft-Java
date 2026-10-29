package dev.shoko.minecraft.data.json;

/** The kind of a JSON value, for type checks and error messages. */
public enum JsonType {
  /** An object. */
  OBJECT,
  /** An array. */
  ARRAY,
  /** A string. */
  STRING,
  /** A number, in integer or decimal form (R50). */
  NUMBER,
  /** {@code true} or {@code false}. */
  BOOLEAN,
  /** {@code null}: read without complaint, refused by the codecs (R56). */
  NULL
}
