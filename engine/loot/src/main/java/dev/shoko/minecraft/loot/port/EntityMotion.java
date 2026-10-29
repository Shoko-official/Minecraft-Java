package dev.shoko.minecraft.loot.port;

/**
 * What the {@code movement} predicate reads (LT-R16, LT-R57a, EN-R30).
 *
 * @param horizontalSpeed the horizontal speed, in blocks per tick
 * @param fallDistance the distance fallen so far, in blocks
 */
public record EntityMotion(double horizontalSpeed, double fallDistance) {}
