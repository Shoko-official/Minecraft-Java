package dev.shoko.minecraft.loot;

import dev.shoko.minecraft.data.diag.Severity;

/**
 * Stable machine-readable cause of a {@link LootFinding}. Callers and tests rely on the code and
 * its severity, never on the message. The set of codes is a design choice of the skeleton, not a
 * spec rule: {@code DiagnosticCode} of {@code data} is a closed enum of that module and cannot
 * carry the causes of the loot rules.
 */
public enum LootFindingCode {
  /** A field required by the shape of a table, entry, condition, modifier or enchantment. */
  MISSING_FIELD(Severity.ERROR),
  /** A field outside the known shape; refused until probe PL17 settles it (provisional). */
  UNKNOWN_FIELD(Severity.ERROR),
  /** A value of the wrong kind: a string where an object is expected, and so on. */
  TYPE_MISMATCH(Severity.ERROR),
  /** A number outside the range of its field (EN-R10, LT-R6). */
  VALUE_RANGE(Severity.ERROR),
  /** A text that breaks the identifier syntax. */
  INVALID_IDENTIFIER(Severity.ERROR),
  /** A {@code type} that is not in its registry (LT-R3, LT-R7, LT-R50). */
  UNKNOWN_TYPE(Severity.ERROR),
  /** A named predicate, provider or table that does not exist; the table is refused (LT-R61b). */
  UNRESOLVED_REFERENCE(Severity.ERROR),
  /** A tag cited by a table or an enchantment that is not loaded. */
  UNRESOLVED_TAG(Severity.ERROR),
  /** An element that lies on a cycle of references between named elements (LT-R76). */
  REFERENCE_CYCLE(Severity.ERROR),
  /** A table that references itself, directly (LT-R23). */
  SELF_REFERENCE(Severity.ERROR),
  /** An enchantment identifier that is not in the enchantment registry. */
  UNKNOWN_ENCHANTMENT(Severity.ERROR),
  /** A condition reads a parameter that the context type does not provide (LT-R15). */
  UNAVAILABLE_PARAMETER(Severity.WARNING),
  /** A modifier of the 21 types without behavior, read as the identity (LT-R47). */
  NOT_IMPLEMENTED_MODIFIER(Severity.WARNING),
  /** The constant weight of an entry is above the cap of 2^20 and counts as 2^20 (LT-R74c). */
  WEIGHT_CAPPED(Severity.WARNING),
  /** A table referenced at evaluation time that is not loaded: it yields nothing (runtime). */
  TABLE_NOT_FOUND(Severity.WARNING),
  /**
   * A {@code dynamic} entry whose source the context cannot provide: it yields nothing (LT-R24).
   */
  DYNAMIC_SOURCE_MISSING(Severity.INFO);

  private final Severity severity;

  LootFindingCode(Severity severity) {
    this.severity = severity;
  }

  /**
   * Returns how serious the finding is: an {@code ERROR} produces no value, a {@code WARNING}
   * accompanies a value produced with a provisional fallback, an {@code INFO} is an observation.
   *
   * @return the severity
   */
  public Severity severity() {
    return severity;
  }
}
