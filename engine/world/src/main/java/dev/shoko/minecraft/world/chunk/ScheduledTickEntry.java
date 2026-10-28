package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.BlockPos;

/**
 * One entry of {@code block_ticks} or {@code fluid_ticks} as stored (CF-R23, provisional). Running
 * the ticks belongs to milestone M5.
 *
 * @param id identifier of the block or fluid, key {@code i}
 * @param priority priority, key {@code p}; the smallest goes first
 * @param delay ticks to wait, key {@code t}; negative when late
 * @param pos absolute position, keys {@code x}, {@code y}, {@code z}
 */
public record ScheduledTickEntry(String id, int priority, int delay, BlockPos pos) {}
