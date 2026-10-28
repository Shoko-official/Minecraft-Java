package dev.shoko.minecraft.data.diag;

/**
 * How serious a diagnostic is. Fixed per {@link DiagnosticCode} (R61); the policy applied to each
 * level belongs to the caller (R55).
 */
public enum Severity {
  /** The element, file or pack is missing, unusable or discarded (R54, R55, R66). */
  ERROR,
  /** The data is used but suspicious, such as a duplicate key (R58) or a folder that is no pack. */
  WARNING,
  /** Something was ignored by rule, such as an unknown folder (R15, R16). */
  INFO
}
