package dev.shoko.minecraft.world.storage;

import java.util.List;

/**
 * One section of a {@code poi/} chunk (AR-R23, provisional).
 *
 * @param valid {@code Valid}, a Byte read as a boolean
 * @param records {@code Records}, in file order
 */
public record PoiSection(boolean valid, List<PoiRecord> records) {}
