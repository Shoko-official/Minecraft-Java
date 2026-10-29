package dev.shoko.minecraft.item;

import dev.shoko.minecraft.data.diag.Severity;

/**
 * Stable machine-readable cause of a {@link Finding}. Callers and tests rely on the code, never on
 * the message. The set of codes is a design choice of the skeleton, not a spec rule: {@code
 * DiagnosticCode} of {@code data} is a closed enum of that module and cannot carry the causes of
 * the item rules.
 */
public enum FindingCode {
  /** A field required by the shape of a component, stack or recipe is absent (IC-R12). */
  MISSING_FIELD(Severity.ERROR),
  /** A field outside the known shape; refused until probe PI3 settles it (IC-R12). */
  UNKNOWN_FIELD(Severity.ERROR),
  /** A value of the wrong kind: a string where an object is expected, and so on. */
  TYPE_MISMATCH(Severity.ERROR),
  /** A number outside the range of its field (IC-R22, IC-R16, RC-R24). */
  VALUE_RANGE(Severity.ERROR),
  /** A text that breaks the identifier syntax. */
  INVALID_IDENTIFIER(Severity.ERROR),
  /** A component identifier absent from the registry; the whole table is refused (IC-R10). */
  UNKNOWN_COMPONENT(Severity.ERROR),
  /** A stack read from NBT whose item is unknown: read as the empty stack (IC-R26). */
  UNKNOWN_ITEM(Severity.WARNING),
  /** A stack read with a count of 0 or less: read as the empty stack (IC-R15, IC-R26). */
  NON_POSITIVE_COUNT(Severity.WARNING),
  /** A slot index of a container list outside the container, or a missing or mistyped slot. */
  INVALID_SLOT(Severity.WARNING),
  /**
   * A key given twice once normalized, such as {@code sharpness} and {@code minecraft:sharpness}.
   */
  DUPLICATE_KEY(Severity.ERROR),
  /** The command text form of a stack is malformed (IC-R29). */
  TEXT_SYNTAX(Severity.ERROR),
  /** Nested stacks deeper than the supported depth (IC-R23, probe PI4). */
  NESTING_TOO_DEEP(Severity.ERROR),
  /** The prototypes break an invariant of the data: IC-R1, IC-R3, IC-R5. */
  INCONSISTENT_PROTOTYPES(Severity.ERROR),
  /** A recipe whose {@code type} is not a known serializer (RC-R1). */
  UNKNOWN_RECIPE_TYPE(Severity.ERROR),
  /** An ingredient that is an empty list (RC-R9). */
  EMPTY_INGREDIENT(Severity.ERROR),
  /** An ingredient or result naming an item that is not in the registry. */
  UNRESOLVED_ITEM(Severity.ERROR),
  /** An item tag cited by a recipe that is not loaded: the recipe is ignored (RC-R13). */
  UNRESOLVED_TAG(Severity.ERROR),
  /** A pattern that is empty, has more than 3 rows or columns, or has rows of unequal length. */
  PATTERN_SHAPE(Severity.ERROR),
  /** A key of a shaped recipe that no pattern character uses (RC-R15). */
  PATTERN_KEY_UNUSED(Severity.ERROR),
  /** A pattern character, other than the space, that has no key (RC-R15). */
  PATTERN_KEY_MISSING(Severity.ERROR),
  /** A recipe result whose count exceeds the stack size of its item (RC-R24). */
  RESULT_EXCEEDS_STACK(Severity.ERROR);

  private final Severity severity;

  FindingCode(Severity severity) {
    this.severity = severity;
  }

  /**
   * Returns how serious the finding is: an {@code ERROR} produces no value, a {@code WARNING}
   * accompanies a value produced with a provisional fallback.
   *
   * @return the severity
   */
  public Severity severity() {
    return severity;
  }
}
