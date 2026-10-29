/**
 * The loot context: named parameters, context types, luck, level facts and the random source of a
 * draw.
 *
 * <p>Spec: {@code docs/specs/loot-tables.md} (LT-R11 to LT-R15, LT-R18) and {@code
 * docs/specs/item-enchantments.md} (EN-R15). The parameters of each of the 31 context types are
 * provisional (LT-R13, LT-R14b): a context does not refuse a missing parameter, it reports it
 * ({@link dev.shoko.minecraft.loot.context.LootContext#missingRequiredKeys()}) and each condition
 * decides what an absent parameter means (LT-R15).
 */
package dev.shoko.minecraft.loot.context;
