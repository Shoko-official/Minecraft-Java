/**
 * Loot tables, predicates, number providers, item modifiers and enchantments (ADR 0004 and its
 * amendment 1).
 *
 * <p>Specs: {@code docs/specs/loot-tables.md} (rules cited as {@code LT-Rn}, probes {@code PLn},
 * vectors {@code V-Ln}) and {@code docs/specs/item-enchantments.md} (rules cited as {@code EN-Rn},
 * probes {@code PEn}, vectors {@code V-En}). The two specs do not define these prefixes; they are
 * used here only to tell the two rule numberings apart.
 *
 * <p>This module reads {@code world} (block states, block entities, chunks) and {@code item}
 * (stacks, components, recipes) directly. The only read port is {@link
 * dev.shoko.minecraft.loot.port.EntityView}; the only effect port is {@link
 * dev.shoko.minecraft.loot.port.ActionSink}. {@code entity} and {@code game} implement them.
 *
 * <p>Randomness: every draw goes through a {@link dev.shoko.minecraft.core.random.RandomSource}
 * received from the caller, never through a clock, an unseeded generator or a random UUID. The
 * order of consumption is hypothesis H-draw (LT-R66); {@link
 * dev.shoko.minecraft.loot.random.DrawObserver} reports each decision with the steps it consumed.
 *
 * <p>Nothing in this API accepts or returns {@code null}; absence is an {@code Optional} result, an
 * empty collection or a documented neutral value. Records are immutable and hold no {@code
 * Optional}.
 */
package dev.shoko.minecraft.loot;
