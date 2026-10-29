package dev.shoko.minecraft.loot.predicate.match;

/** An optional boolean requirement: not asked, or asked to be true or false. */
public enum Tristate {
  /** The key is absent: any value matches. */
  UNSET,
  /** The key is present and {@code true}. */
  TRUE,
  /** The key is present and {@code false}. */
  FALSE
}
