package dev.shoko.minecraft.core.nbt.snbt;

import dev.shoko.minecraft.core.nbt.NbtTag;

/**
 * Formats a value as the SNBT text shown by {@code /data get} (R52 to R56), provisional until probe
 * PW1:
 *
 * <ul>
 *   <li>Numbers: {@code 1b}, {@code 1s}, {@code 1}, {@code 1L}, {@code 1.5f}, {@code 1.5d}; never
 *       {@code true} or {@code false} (R52).
 *   <li>Float and Double digits as {@link Float#toString(float)} and {@link
 *       Double#toString(double)}, then the suffix: {@code 1000.0d}, {@code 1.0E10f}, {@code -0.0d},
 *       {@code NaNd}, {@code -Infinityf} (R53).
 *   <li>Strings always quoted with {@code "}, or with {@code '} when the first quote found in the
 *       string is {@code "}; only the backslash and the chosen quote are escaped; every other
 *       character is written as is (R54).
 *   <li>Keys unquoted when made only of {@code 0-9 A-Z a-z _ - . +}, else quoted as strings (R55).
 *   <li>Separators {@code {a: 1b, b: "x"}}, {@code [1, 2]}, {@code [I; 1, 2]}; empty forms {@code
 *       {}}, {@code []}, {@code [B;]}, {@code [I;]}, {@code [L;]}; keys in storage order;
 *       heterogeneous lists without wrapper, {@code [1, "a"]} (R56).
 * </ul>
 */
public final class SnbtWriter {

  private SnbtWriter() {}

  /**
   * Returns the SNBT text of a value (R52 to R56).
   *
   * @param tag the value
   * @return the text, on a single line
   */
  public static String write(NbtTag tag) {
    return Skeleton.throwNotImplemented();
  }
}
