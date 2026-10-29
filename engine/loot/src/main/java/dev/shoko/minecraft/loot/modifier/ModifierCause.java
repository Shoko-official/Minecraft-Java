package dev.shoko.minecraft.loot.modifier;

/** What happened when one modifier met one stack. */
public enum ModifierCause {
  /** The conditions held and the action ran. */
  APPLIED,
  /** A condition failed: the stack passed unchanged (LT-R38). */
  CONDITION_FAILED,
  /** The modifier is one of the 21 types without behavior: identity (LT-R47). */
  DEFERRED_IDENTITY,
  /** The stack was already empty: nothing was done (LT-R49). */
  EMPTY_INPUT,
  /** A parameter the action needs is absent from the context; the stack passed unchanged. */
  PARAMETER_ABSENT
}
