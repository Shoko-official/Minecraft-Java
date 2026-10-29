package dev.shoko.minecraft.loot.random;

/**
 * One decision of an evaluation that consumed random steps, in order of occurrence. The steps are
 * read from {@link dev.shoko.minecraft.core.random.RandomSource#steps()} before and after the
 * decision; a decision that draws nothing (a single candidate, a degenerate range) is also
 * reported, with equal counts, so that a test sees the order of the decisions as well as the
 * consumption.
 *
 * @param reason the kind of decision
 * @param path where in the table, such as {@code pools[0].entries[2]}; empty for a decision outside
 *     a table
 * @param stepsBefore the step count of the source before the decision
 * @param stepsAfter the step count of the source after the decision
 */
public record DrawEvent(DrawReason reason, String path, long stepsBefore, long stepsAfter) {}
