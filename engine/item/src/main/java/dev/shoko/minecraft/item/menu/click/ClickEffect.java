package dev.shoko.minecraft.item.menu.click;

/** What a click did, a closed category that tests can assert. */
public enum ClickEffect {
  /** The state changed or stacks were thrown. */
  APPLIED,
  /** Nothing changed: an unknown slot, nothing to take, a slot that accepts nothing. */
  NO_CHANGE,
  /**
   * The operation was refused because the result does not fit, with no duplication or loss
   * (IC-R47b).
   */
  REFUSED
}
