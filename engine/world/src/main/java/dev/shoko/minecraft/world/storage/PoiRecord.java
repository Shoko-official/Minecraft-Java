package dev.shoko.minecraft.world.storage;

import dev.shoko.minecraft.core.coord.BlockPos;

/**
 * One point of interest of a {@code poi/} section (AR-R23, provisional).
 *
 * @param pos {@code pos}, the Int_Array {@code [x, y, z]}
 * @param type {@code type}, the point of interest type identifier
 * @param freeTickets {@code free_tickets}
 */
public record PoiRecord(BlockPos pos, String type, int freeTickets) {}
