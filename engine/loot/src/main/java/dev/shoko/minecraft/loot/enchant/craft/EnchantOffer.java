package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.item.component.EnchantmentLevels;

/**
 * One offer of the enchanting table (EN-R37 to EN-R39).
 *
 * @param lineCost the cost of the line, in levels
 * @param lapisCost the lapis lazuli the line takes: 1, 2 or 3 by line (provisional)
 * @param enchantments the enchantments the offer would apply, in the order they were chosen
 */
public record EnchantOffer(int lineCost, int lapisCost, EnchantmentLevels enchantments) {}
