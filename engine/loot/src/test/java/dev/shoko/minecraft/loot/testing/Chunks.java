package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.chunk.ChunkAttachments;
import dev.shoko.minecraft.world.chunk.ChunkHeader;
import dev.shoko.minecraft.world.chunk.ChunkSection;
import dev.shoko.minecraft.world.chunk.ChunkStatus;
import dev.shoko.minecraft.world.heightmap.Heightmaps;
import java.util.ArrayList;
import java.util.List;

/** Made-up chunks of the Overworld height, built from the records of {@code world}. */
public final class Chunks {

  private static final WorldHeight OVERWORLD = new WorldHeight(-64, 384);
  private static final String PLAINS = "minecraft:plains";

  private Chunks() {}

  /** A chunk of air whose only non-air section is a section of stone at this section y. */
  public static Chunk stoneSection(ChunkPos pos, int stoneSectionY) {
    List<ChunkSection> sections = new ArrayList<>();
    for (int y = OVERWORLD.minSectionY(); y <= OVERWORLD.maxSectionY(); y++) {
      sections.add(
          ChunkSection.uniform(
              y, y == stoneSectionY ? Blocks.stone() : Blocks.plain("air", 0, 0), PLAINS));
    }
    return Chunk.of(
        new ChunkHeader(pos, OVERWORLD, ChunkStatus.FULL, 0L, 0L),
        sections,
        Heightmaps.empty(),
        ChunkAttachments.empty(OVERWORLD.sectionCount()));
  }
}
