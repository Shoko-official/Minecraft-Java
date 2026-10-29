/**
 * Blocks, chunks, lighting, fluids, ticks and Anvil storage.
 *
 * <p>Specs: {@code docs/specs/world-block-states.md} (rules BS-Rn), {@code
 * docs/specs/world-chunk-format.md} (CF-Rn) and {@code docs/specs/world-anvil-region.md} (AR-Rn);
 * vectors in {@code docs/specs/world-vectors.md}. Rule numbers in the Javadoc refer to them. Rules
 * marked provisional (level S) describe the engine's current contract until an oracle probe settles
 * the game's behavior.
 *
 * <ul>
 *   <li>{@link dev.shoko.minecraft.world.block}: block types, global state identifiers, palette
 *       entries of block states.
 *   <li>{@link dev.shoko.minecraft.world.palette}: paletted containers and their packed storage.
 *   <li>{@link dev.shoko.minecraft.world.biome}: palette entries of biomes.
 *   <li>{@link dev.shoko.minecraft.world.heightmap}: heightmaps and their packed storage.
 *   <li>{@link dev.shoko.minecraft.world.chunk}: chunks, sections, light arrays and the chunk NBT
 *       schema.
 *   <li>{@link dev.shoko.minecraft.world.storage}: world folders, region files, compression and the
 *       chunk store.
 * </ul>
 *
 * <p>Everything this module needs from loaded data (the block report, the known biomes) enters
 * through a small port interface defined here: {@link
 * dev.shoko.minecraft.world.block.BlockReportSource} and {@link
 * dev.shoko.minecraft.world.biome.BiomeCatalog}. Nothing reads a clock: times are explicit
 * arguments.
 *
 * <p>Reading a chunk has three exclusive issues, {@link dev.shoko.minecraft.world.ReadResult}
 * present, absent or refused (AR-R26). An absent chunk carries an {@link
 * dev.shoko.minecraft.world.AbsentCause}, a refused one a {@link
 * dev.shoko.minecraft.world.RefusedCause}; findings that do not change the issue are {@link
 * dev.shoko.minecraft.world.ReadWarning}s, each with a {@link
 * dev.shoko.minecraft.world.WarningCode}, in the list of the result. The lower-level lenient
 * decoders report to a {@link dev.shoko.minecraft.world.WarningSink}. A failure of the file system
 * is an {@link java.io.IOException}, none of the three issues (AR-R27).
 *
 * <p>Chunks, sections, containers and light arrays are mutable and confined to the thread that
 * ticks the world; codecs, registries and block types are immutable and safe to share.
 */
package dev.shoko.minecraft.world;
