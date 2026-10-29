/**
 * Contextual number providers: the 23 integer and 28 float types, the named providers loaded from
 * {@code context_int_provider} and {@code context_float_provider}, and their evaluation for the
 * machines of {@code item}.
 *
 * <p>Spec: {@code docs/specs/loot-tables.md} (LT-R50 to LT-R53) and {@code
 * docs/specs/item-recipes.md} (DC1, through ADR 0004 amendment 1). The shapes read in the data are
 * typed; the types whose fields no data shows (score, storage, environment attribute, length) are
 * kept as {@code Unspecified} and evaluated provisionally.
 */
package dev.shoko.minecraft.loot.number;
