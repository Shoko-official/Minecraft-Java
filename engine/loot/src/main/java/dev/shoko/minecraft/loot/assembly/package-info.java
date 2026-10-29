/**
 * The loaded data of the module and its loader, the bootstrap that {@code game} calls to decode the
 * registries owned by {@code loot} (ADR 0005, decision 1).
 *
 * <p>Spec: {@code docs/specs/loot-tables.md} (LT-R1, LT-R50, LT-R61a, plan of tests L1: the 1447
 * tables, 3 predicates and 30 providers decode without error) and {@code
 * docs/specs/item-enchantments.md} (EN-R1, EN-R34). The registries are decoded in dependency order:
 * number providers and predicates first, then enchantments, then tables, whose references are
 * checked last.
 */
package dev.shoko.minecraft.loot.assembly;
