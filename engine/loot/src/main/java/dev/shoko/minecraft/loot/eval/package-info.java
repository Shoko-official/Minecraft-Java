/**
 * The draw of a loot table: pool rolls, expansion of entries, weighted choice, ordered application
 * of modifiers, splitting of stacks, and the unpacking of a container.
 *
 * <p>Spec: {@code docs/specs/loot-tables.md} (LT-R19 to LT-R28, LT-R48, LT-R49, LT-R54 to LT-R56,
 * LT-R63 to LT-R70, probes PL1 to PL8, PL16, PL19). The order of the random draws is hypothesis
 * H-draw (LT-R66): every decision reports a {@code DrawEvent} to the observer of the context, in
 * the order of the algorithm. Intermediate steps ({@code rolls}, {@code candidates}) are public so
 * that a test can check a decision without running a whole table.
 */
package dev.shoko.minecraft.loot.eval;
