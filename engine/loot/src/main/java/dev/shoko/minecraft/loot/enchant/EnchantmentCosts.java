package dev.shoko.minecraft.loot.enchant;

import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;

/**
 * The cost and weight fields of a definition (EN-R2, EN-R3, EN-R5, EN-R25).
 *
 * @param weight the weight in the selection of the enchanting table, 1, 2, 5 or 10 in the data
 * @param anvilCost the anvil multiplier, 1, 2, 4 or 8 in the data
 * @param minCost the minimum cost by level, of the shape of {@code linear} without {@code type}
 * @param maxCost the maximum cost by level
 */
public record EnchantmentCosts(
    int weight, int anvilCost, LevelBasedValue.Linear minCost, LevelBasedValue.Linear maxCost) {}
