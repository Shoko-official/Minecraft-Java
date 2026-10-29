/**
 * Block types, block states and their global identifiers.
 *
 * <p>Spec: {@code docs/specs/world-block-states.md}; rule numbers Rn in the Javadoc refer to it.
 *
 * <ul>
 *   <li>Input port: {@link dev.shoko.minecraft.world.block.BlockReportSource} with {@link
 *       dev.shoko.minecraft.world.block.BlockReportEntry} and {@link
 *       dev.shoko.minecraft.world.block.ReportedState} (R1 to R4).
 *   <li>Model: {@link dev.shoko.minecraft.world.block.BlockProperty}, {@link
 *       dev.shoko.minecraft.world.block.BlockType}, {@link
 *       dev.shoko.minecraft.world.block.BlockState} (R5 to R15).
 *   <li>Registry built and checked from the report: {@link
 *       dev.shoko.minecraft.world.block.BlockStateRegistry}, which fails with {@link
 *       dev.shoko.minecraft.world.block.BlockReportException} on any inconsistency (R3, R5 to R7).
 *   <li>NBT form of a state in a palette: {@link
 *       dev.shoko.minecraft.world.block.BlockStateEntryCodec} (R16 to R22).
 * </ul>
 *
 * <p>State identifiers depend on the loaded block set: they are never written to disk. The
 * in-memory representation of a state (flat table or interned objects) is left open by the
 * architecture document; {@link dev.shoko.minecraft.world.block.BlockState} is a value of a block
 * and an identifier so that either can back it.
 */
package dev.shoko.minecraft.world.block;
