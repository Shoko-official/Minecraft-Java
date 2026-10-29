/**
 * The matchers that conditions and modifiers share: ranges, identifier or tag sets, block, item,
 * entity, location and damage source predicates.
 *
 * <p>Spec: {@code docs/specs/loot-tables.md} (LT-R34, LT-R35, LT-R57 to LT-R60). Only the keys met
 * in the data are typed (section {@code R57} to {@code R60} of the spec); a registered key that no
 * data uses is kept as read. Every predicate holds a list of requirements that must all hold
 * (LT-R57b), so that an absent key is an absent requirement and never a {@code null}.
 */
package dev.shoko.minecraft.loot.predicate.match;
