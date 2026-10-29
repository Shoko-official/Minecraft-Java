package dev.shoko.minecraft.loot.enchant;

import dev.shoko.minecraft.loot.predicate.match.IdOrTagSet;

/**
 * The items an enchantment accepts (EN-R7a, EN-R7b).
 *
 * @param supported the items that can carry it (commands, anvil, loot): an item tag
 * @param primary the items the enchanting table offers it on, when the definition has the key
 * @param hasPrimary whether the definition has {@code primary_items}; when false, {@code primary}
 *     equals {@code supported} (38 of the 43 enchantments)
 */
public record EnchantmentItems(IdOrTagSet supported, IdOrTagSet primary, boolean hasPrimary) {}
