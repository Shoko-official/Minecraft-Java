package dev.shoko.minecraft.data.diag;

/** How serious a diagnostic is. The policy applied to each level belongs to the caller (R55). */
public enum Severity {
  /** The element or file is missing or unusable (R54, R55). */
  ERROR,
  /**
   * The data is used but suspicious, such as a duplicate key (R51) or an incompatible pack (R6).
   */
  WARNING,
  /** Something was ignored by rule, such as an unknown folder (R15, R16). */
  INFO
}
