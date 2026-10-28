package dev.shoko.minecraft.world.storage;

import java.util.SequencedMap;

/**
 * The root of a chunk of a {@code poi/} region file (AR-R23, provisional).
 *
 * @param dataVersion {@code DataVersion}
 * @param sections {@code Sections}: the Compound keyed by section y in decimal text ({@code "-4"},
 *     {@code "0"}), here keyed by the integer, in file order
 */
public record PoiChunk(int dataVersion, SequencedMap<Integer, PoiSection> sections) {}
