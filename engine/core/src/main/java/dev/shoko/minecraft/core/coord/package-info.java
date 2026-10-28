/**
 * Block, chunk, section and region coordinates, directions, and basic geometry.
 *
 * <p>Spec: {@code docs/specs/core-math.md}; rule numbers Rn in the Javadoc refer to it. Rules
 * marked provisional (level S in the spec) describe the engine's current contract, which the blind
 * tests target until an oracle probe settles the game's behavior.
 *
 * <ul>
 *   <li>Scalar conversions between block, section, quart and region units, and the horizontal
 *       limit: {@link dev.shoko.minecraft.core.coord.Coordinates} (R26, R28).
 *   <li>Positions: {@link dev.shoko.minecraft.core.coord.BlockPos} (R27 to R29), {@link
 *       dev.shoko.minecraft.core.coord.ChunkPos} (R28, R30, R33), {@link
 *       dev.shoko.minecraft.core.coord.SectionPos} (R28, R31), {@link
 *       dev.shoko.minecraft.core.coord.RegionPos} (R33).
 *   <li>Local packings inside a section: {@link dev.shoko.minecraft.core.coord.LocalIndex} (R32).
 *   <li>Vertical extent of a dimension: {@link dev.shoko.minecraft.core.coord.WorldHeight} (R25).
 *   <li>Directions: {@link dev.shoko.minecraft.core.coord.Axis} (R34), {@link
 *       dev.shoko.minecraft.core.coord.Direction} (R35, R36), {@link
 *       dev.shoko.minecraft.core.coord.Rotation} and {@link dev.shoko.minecraft.core.coord.Mirror}
 *       (R38).
 *   <li>Geometry: {@link dev.shoko.minecraft.core.coord.Vec3} (R39) and {@link
 *       dev.shoko.minecraft.core.coord.Aabb} (R40 to R42).
 * </ul>
 *
 * <p>Every conversion of an integer coordinate uses arithmetic shifts and masks, never {@code /} or
 * {@code %}, so negative coordinates round towards negative infinity (R9). The order of values of
 * block state properties ({@code facing}, {@code rotation}) is not the order of {@link
 * dev.shoko.minecraft.core.coord.Direction} and belongs to the block states spec (R37).
 *
 * <p>All types are immutable and safe to share between threads. No method accepts or returns {@code
 * null}.
 */
package dev.shoko.minecraft.core.coord;
