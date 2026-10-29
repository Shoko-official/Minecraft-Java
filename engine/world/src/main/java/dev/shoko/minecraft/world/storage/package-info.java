/**
 * Folders of a world, Anvil region files, their compression and the chunk store.
 *
 * <p>Spec: {@code docs/specs/world-anvil-region.md}; rule numbers Rn in the Javadoc refer to it.
 *
 * <ul>
 *   <li>Folders and file names: {@link dev.shoko.minecraft.world.storage.WorldLayout} (R1 to R4).
 *   <li>The container: {@link dev.shoko.minecraft.world.storage.RegionFile} (R5 to R12, R17 to
 *       R20), compression types {@link dev.shoko.minecraft.world.storage.RegionCompression} (R13,
 *       R14), their bounded decompression (R29) failing with {@link
 *       dev.shoko.minecraft.world.storage.CorruptStreamException} or {@link
 *       dev.shoko.minecraft.world.storage.OutputLimitExceededException}, and the LZ4 frame {@link
 *       dev.shoko.minecraft.world.storage.Lz4BlockCodec} (R15, R16).
 *   <li>Chunks as objects: {@link dev.shoko.minecraft.world.storage.AnvilChunkStore}, which joins a
 *       region file, the NBT reader and writer of the core module, and the {@link
 *       dev.shoko.minecraft.world.chunk.ChunkCodec} (R19 to R21, R24, R26 to R30). A save at a
 *       refused position fails with {@link
 *       dev.shoko.minecraft.world.storage.WriteOnRefusedPositionException} (R28).
 *   <li>Sidecar files: {@link dev.shoko.minecraft.world.storage.AuxiliaryChunkCodec} for the
 *       envelopes of {@code entities/} and {@code poi/} (R22, R23).
 * </ul>
 *
 * <p>A read has three issues, {@link dev.shoko.minecraft.world.ReadResult}: present, "absent"
 * (generated later; not an error) or "refused" (the bytes are never touched), with the causes of
 * R26. A failure of the file system is an {@link java.io.IOException} (R27). The definitions are in
 * the spec, section "Entrees / sorties". Times are explicit arguments.
 */
package dev.shoko.minecraft.world.storage;
