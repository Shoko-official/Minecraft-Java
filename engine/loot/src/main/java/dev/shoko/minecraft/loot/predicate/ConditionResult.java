package dev.shoko.minecraft.loot.predicate;

/**
 * The answer of a condition and its cause.
 *
 * @param passed whether the condition holds
 * @param cause why
 */
public record ConditionResult(boolean passed, ConditionCause cause) {}
