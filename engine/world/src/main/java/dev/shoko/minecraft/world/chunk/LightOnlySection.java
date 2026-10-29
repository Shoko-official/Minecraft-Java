package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.nbt.CompoundTag;

/**
 * A section just below or just above the world height, which exists only to store light (CF-R31).
 * It holds no block and no biome. It is read when present and written back as it was read; the
 * engine never creates one.
 *
 * @param light the stored {@code BlockLight} and {@code SkyLight}, either of which may be absent
 * @param otherFields the other entries of the section Compound, kept verbatim and opaque (for
 *     instance {@code block_states} and {@code biomes}, which are ignored on reading and written
 *     back unchanged); never holds {@code Y}, {@code BlockLight} or {@code SkyLight}
 */
public record LightOnlySection(SectionLight light, CompoundTag otherFields) {}
