package dev.shoko.minecraft.world.block;

import java.util.Map;

/**
 * One state of the report: the identifier the reference game gives it, its property values, and
 * whether it is the default state (R2).
 *
 * @param id state identifier as written in the report
 * @param properties value of each property of the block by name; empty for a block without
 *     properties
 * @param isDefault {@code true} for the single default state of the block
 */
public record ReportedState(int id, Map<String, String> properties, boolean isDefault) {}
