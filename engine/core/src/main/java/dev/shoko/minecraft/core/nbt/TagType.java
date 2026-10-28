package dev.shoko.minecraft.core.nbt;

import java.util.Optional;

/**
 * The 13 binary tag types, declared in identifier order (R1, R2). {@link #END} is a marker, not a
 * value: it ends a compound (R6), types an empty list (R22) and stands for an absent root (R20).
 */
public enum TagType {
  /** Identifier 0: compound terminator and element type of empty lists (R1, R5, R6, R22). */
  END,
  /** Identifier 1: signed 8-bit integer (R1, R4). */
  BYTE,
  /** Identifier 2: signed 16-bit integer (R1, R4). */
  SHORT,
  /** Identifier 3: signed 32-bit integer (R1, R4). */
  INT,
  /** Identifier 4: signed 64-bit integer (R1, R4). */
  LONG,
  /** Identifier 5: IEEE 754 binary32 (R1, R3, R4). */
  FLOAT,
  /** Identifier 6: IEEE 754 binary64 (R1, R3, R4). */
  DOUBLE,
  /** Identifier 7: array of signed bytes (R2, R9). */
  BYTE_ARRAY,
  /** Identifier 8: modified UTF-8 string (R1, R10, R11). */
  STRING,
  /** Identifier 9: list of values of one element type (R1, R7). */
  LIST,
  /** Identifier 10: named values ended by {@link #END} (R1, R6). */
  COMPOUND,
  /** Identifier 11: array of signed 32-bit integers (R1, R8). */
  INT_ARRAY,
  /** Identifier 12: array of signed 64-bit integers (R2, R9). */
  LONG_ARRAY;

  /**
   * Returns the binary identifier of this type, from 0 for {@link #END} to 12 for {@link
   * #LONG_ARRAY} (R1, R2).
   *
   * @return the identifier written before a named tag or as a list element type
   */
  public int id() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the type with this binary identifier (R1, R2).
   *
   * @param id identifier as read, possibly out of range
   * @return the type, or empty for a negative identifier or one of 13 and above (R33)
   */
  public static Optional<TagType> byId(int id) {
    return Skeleton.throwNotImplemented();
  }
}
