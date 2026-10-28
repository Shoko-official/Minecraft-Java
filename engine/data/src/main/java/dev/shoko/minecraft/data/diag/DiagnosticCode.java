package dev.shoko.minecraft.data.diag;

/**
 * Stable machine-readable cause of a diagnostic. Callers and tests rely on the code, never on the
 * message, which is for humans and not specified. The set of codes is a design choice of the
 * skeleton, not a spec rule.
 */
public enum DiagnosticCode {
  /** Bytes that are not valid UTF-8, or a byte order mark at the start of a file (R53). */
  INVALID_ENCODING,
  /** Text outside RFC 8259: empty file, comment, trailing comma, content after root (R52, R53). */
  INVALID_JSON,
  /** Nesting deeper than {@code JsonReader.MAX_DEPTH}. */
  JSON_TOO_DEEP,
  /** A decimal that overflows to infinity or an integer beyond 64 bits (R50). */
  NUMBER_OUT_OF_RANGE,
  /** An object with the same key twice; the last value is kept (R51). */
  DUPLICATE_KEY,
  /** A {@code pack.mcmeta} that is not an object with a {@code pack} object (R7). */
  INVALID_PACK_META,
  /** A pack made for another version, or with unusable format fields (R5, R6). */
  INCOMPATIBLE_PACK,
  /** A {@code filter} or {@code overlays} entry of {@code pack.mcmeta} that is unreadable. */
  INVALID_PACK_FILTER,
  /** A file path or value that breaks the identifier syntax (R20, R24). */
  INVALID_IDENTIFIER,
  /** A file under a folder that is no registry (R15). */
  UNKNOWN_FOLDER,
  /** A file with an extension the folder does not take (R12). */
  UNSUPPORTED_EXTENSION,
  /** A tag file under a registry that takes no tags (R16). */
  TAGS_NOT_ACCEPTED,
  /** A tag file whose shape is wrong (R32). */
  INVALID_TAG_FILE,
  /** A required element of a tag that is not in the registry (R36). */
  MISSING_TAG_ENTRY,
  /** A reference to a tag that does not exist, or that failed (R36). */
  MISSING_TAG_REFERENCE,
  /** Tags referencing each other in a loop (R37). */
  TAG_CYCLE,
  /** A required field absent from an object (R56). */
  MISSING_FIELD,
  /** A field or value of the wrong JSON type, including {@code null} (R56). */
  WRONG_TYPE,
  /** A value of the right type but outside what the codec accepts. */
  INVALID_VALUE,
  /** A {@code type} field naming no known variant. */
  UNKNOWN_VARIANT,
  /** A reference to an element or tag that does not exist (K3). */
  UNRESOLVED_REFERENCE
}
