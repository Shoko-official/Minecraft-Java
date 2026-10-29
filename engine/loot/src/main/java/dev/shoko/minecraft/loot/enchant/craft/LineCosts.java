package dev.shoko.minecraft.loot.enchant.craft;

/**
 * The costs of the three lines of the enchanting table (EN-R37).
 *
 * @param top the cost of the top line, {@code max(1, floor(base / 3))}
 * @param middle the cost of the middle line, {@code floor(2 * base / 3) + 1}
 * @param bottom the cost of the bottom line, {@code max(base, 2 * shelves)}
 */
public record LineCosts(int top, int middle, int bottom) {}
