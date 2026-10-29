/**
 * Loot conditions: the 20 registered types plus references to named predicates, their evaluation
 * with typed causes, and the named predicates of the {@code predicate} registry.
 *
 * <p>Spec: {@code docs/specs/loot-tables.md} (LT-R4a, LT-R15, LT-R29 to LT-R37, LT-R61a, LT-R61b)
 * and {@code docs/specs/item-enchantments.md} (EN-R13: the {@code requirements} of an effect are
 * loot conditions). The matchers they use are in {@code predicate.match}.
 */
package dev.shoko.minecraft.loot.predicate;
