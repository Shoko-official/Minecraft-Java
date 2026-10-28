/**
 * Chunks, sections, light arrays and the NBT schema of a chunk in a region file.
 *
 * <p>Spec: {@code docs/specs/world-chunk-format.md}; rule numbers Rn in the Javadoc refer to it.
 *
 * <ul>
 *   <li>Model: {@link dev.shoko.minecraft.world.chunk.Chunk} with its {@link
 *       dev.shoko.minecraft.world.chunk.ChunkHeader}, {@link
 *       dev.shoko.minecraft.world.chunk.ChunkStatus} (R27 to R29) and {@link
 *       dev.shoko.minecraft.world.chunk.ChunkSection} (R2 to R5, R22) with its light (R14, R15):
 *       {@link dev.shoko.minecraft.world.chunk.SectionLight} and {@link
 *       dev.shoko.minecraft.world.chunk.NibbleArray}.
 *   <li>Data carried but not interpreted (opaque NBT, ADR 0005): {@link
 *       dev.shoko.minecraft.world.chunk.ChunkAttachments} with {@link
 *       dev.shoko.minecraft.world.chunk.BlockEntityData}, {@link
 *       dev.shoko.minecraft.world.chunk.ScheduledTickEntry} and {@link
 *       dev.shoko.minecraft.world.chunk.StructureData} (R21 to R26).
 *   <li>The NBT schema: {@link dev.shoko.minecraft.world.chunk.ChunkCodec} (R20 to R26), failing
 *       with {@link dev.shoko.minecraft.world.chunk.ChunkFormatException}.
 * </ul>
 *
 * <p>The engine reads only {@code DataVersion} 5023 (see the chunk codec). Calculating light and
 * heightmaps (M5) and generation (M4) are out of scope.
 */
package dev.shoko.minecraft.world.chunk;
