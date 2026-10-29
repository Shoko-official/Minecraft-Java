package dev.shoko.minecraft.loot.predicate;

/** Why a condition gave its answer, for tests that check LT-R15 and LT-R29. */
public enum ConditionCause {
  /** The condition read what it needs and the check held (or, for a draw, the draw was below). */
  MATCHED,
  /** The condition read what it needs and the check did not hold. */
  NOT_MATCHED,
  /**
   * A parameter the condition reads was absent from the context (LT-R15); {@link
   * ConditionResult#passed()} tells which way the rule goes: {@code survives_explosion} passes, the
   * others fail, {@code table_bonus} reads level 0 and is not reported with this cause.
   */
  PARAMETER_ABSENT
}
