package dev.shoko.minecraft.architecture.fixture.budget.small;

/** Budget fixture: its nested class is not a top-level class. */
public final class SmallOne {
  /** Not counted by the budget. */
  public static final class Nested {}

  public int value() {
    return 1;
  }
}
