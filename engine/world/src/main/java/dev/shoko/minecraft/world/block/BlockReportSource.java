package dev.shoko.minecraft.world.block;

import java.util.List;

/**
 * Port: the content of the block report ({@code generated/reports/blocks.json}) joined with the
 * order of the {@code minecraft:block} registry (R1, R2, R4).
 *
 * <p>Defined here so that {@code world} does not depend on how the data module parses and exposes
 * JSON and registries; the caller adapts whatever {@code data} offers. The report is passed
 * unchecked: {@link BlockStateRegistry#load(BlockReportSource)} verifies it.
 */
public interface BlockReportSource {

  /**
   * Returns every block in the order of the {@code minecraft:block} registry: entry {@code i} has
   * protocol id {@code i} (R4).
   *
   * @return the blocks, unmodifiable
   */
  List<BlockReportEntry> blocks();
}
