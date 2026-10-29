package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.loot.enchant.EnchantmentDefinition;

/**
 * An enchantment at a level, a candidate or a choice of the selection (EN-R38, EN-R39).
 *
 * @param enchantment the definition
 * @param level the level, 1 to {@code maxLevel}
 */
public record EnchantmentInstance(EnchantmentDefinition enchantment, int level) {}
