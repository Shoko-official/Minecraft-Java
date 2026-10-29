package dev.shoko.minecraft.data.diag;

/**
 * Stable machine-readable cause of a diagnostic: the closed list of the table "Codes de diagnostic"
 * of {@code docs/specs/data-datapack-loading.md} (R61). Callers and tests rely on the code and its
 * severity, never on the message, which is for humans and not specified. Each code has one fixed
 * severity.
 */
public enum DiagnosticCode {
  /**
   * Syntax outside RFC 8259 (R52): comment, trailing comma, single quote, unquoted key or string,
   * {@code NaN}, {@code Infinity}, malformed number (R59), bad escape or raw control character in a
   * string (R60), whitespace other than space, tab, line feed and carriage return.
   */
  JSON_SYNTAX(Severity.ERROR),
  /** A UTF-8 byte order mark at the start of the file (R53). */
  JSON_BOM(Severity.ERROR),
  /** An empty file, or one made only of whitespace (R53). */
  JSON_EMPTY(Severity.ERROR),
  /** Non-blank content after the root value (R53). */
  JSON_TRAILING(Severity.ERROR),
  /** Bytes that are not valid UTF-8, a surrogate encoded in bytes included (R53, R60). */
  JSON_UTF8(Severity.ERROR),
  /** More than {@code JsonReader.MAX_DEPTH} nested arrays and objects (R57). */
  JSON_TOO_DEEP(Severity.ERROR),
  /** An integer beyond 64 bits, or a decimal that overflows to infinity (R59). */
  JSON_NUMBER_RANGE(Severity.ERROR),
  /** An object with the same key twice; one per extra occurrence (R58). */
  JSON_DUPLICATE_KEY(Severity.WARNING),
  /** A JSON value of another type than expected, {@code null} included (R56, R62, R64). */
  CODEC_TYPE_MISMATCH(Severity.ERROR),
  /** A required field absent from an object (R56). */
  CODEC_MISSING_FIELD(Severity.ERROR),
  /** A field unknown to an object read in strict mode (R63). */
  CODEC_UNKNOWN_FIELD(Severity.ERROR),
  /** A number outside the bounds of the expected type (R62). */
  CODEC_NUMBER_RANGE(Severity.ERROR),
  /**
   * An identifier or {@code #tag} reference that breaks the syntax, or is misplaced (R20, R32,
   * R64).
   */
  CODEC_INVALID_IDENTIFIER(Severity.ERROR),
  /** An element or tag that is not in the universe it is looked up in (R36, R64, K3). */
  CODEC_UNRESOLVED_REFERENCE(Severity.ERROR),
  /** A pack requiring feature flags that are not all active; the pack is discarded (R66). */
  PACK_FLAGS_INACTIVE(Severity.ERROR),
  /** A folder or archive that is not a pack (R7). */
  PACK_INVALID(Severity.WARNING),
  /** A file ignored by rule (R15, R16, R24). */
  FILE_IGNORED(Severity.INFO);

  private final Severity severity;

  DiagnosticCode(Severity severity) {
    this.severity = severity;
  }

  /**
   * Returns the severity fixed for this code by the spec table (R61).
   *
   * @return the severity
   */
  public Severity severity() {
    return severity;
  }
}
