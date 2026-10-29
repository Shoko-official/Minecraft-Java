package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.world.AbsentCause;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.RefusedCause;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.biome.BiomeCatalog;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.heightmap.Heightmaps;
import java.util.Optional;

/** Internal: reads the root Compound of a chunk (CF-R20 to CF-R26, CF-R29 to CF-R35). */
final class ChunkReader {

  private final BlockStateRegistry blocks;
  private final BiomeCatalog biomes;

  ChunkReader(BlockStateRegistry blocks, BiomeCatalog biomes) {
    this.blocks = blocks;
    this.biomes = biomes;
  }

  ReadResult<Chunk> read(CompoundTag root, WorldHeight height) {
    Findings findings = new Findings();
    try {
      checkVersion(root);
      ChunkStatus status = status(root);
      SectionsReader.Sections sections =
          new SectionsReader(blocks, biomes, height, findings).read(root);
      Optional<ChunkPos> pos = position(root);
      if (pos.isEmpty()) {
        findings.warn(WarningCode.POSITION_MISMATCH, "xPos or zPos is missing or is not an Int");
        return new ReadResult.Absent<>(AbsentCause.POSITION_MISMATCH, findings.warnings());
      }
      Chunk chunk = assemble(root, height, status, pos.get(), sections, findings);
      return new ReadResult.Present<>(chunk, findings.warnings());
    } catch (ReadFault fault) {
      return new ReadResult.Refused<>(fault.refusal(), fault.getMessage());
    }
  }

  private Chunk assemble(
      CompoundTag root,
      WorldHeight height,
      ChunkStatus status,
      ChunkPos pos,
      SectionsReader.Sections sections,
      Findings findings) {
    checkYPos(root, height, findings);
    long lastUpdate =
        findings.optional(root, "LastUpdate", LongTag.class, "").map(LongTag::value).orElse(0L);
    long inhabitedTime =
        findings.optional(root, "InhabitedTime", LongTag.class, "").map(LongTag::value).orElse(0L);
    Heightmaps heightmaps =
        findings
            .optional(root, "Heightmaps", CompoundTag.class, "")
            .map(compound -> Heightmaps.fromNbt(compound, height, findings))
            .orElseGet(Heightmaps::empty);
    ChunkAttachments attachments =
        new AttachmentsReader(pos, height.sectionCount(), findings)
            .read(root, !status.isFull(), sections.lightOnlySections());
    return Chunk.of(
        new ChunkHeader(pos, height, status, lastUpdate, inhabitedTime),
        sections.sections(),
        heightmaps,
        attachments);
  }

  private static void checkVersion(CompoundTag root) throws ReadFault {
    if (!(root.entries().get("DataVersion") instanceof IntTag version)) {
      throw new ReadFault(
          RefusedCause.UNSUPPORTED_VERSION, "DataVersion is missing or is not an Int");
    }
    if (version.value() != ChunkCodec.SUPPORTED_DATA_VERSION) {
      throw new ReadFault(
          RefusedCause.UNSUPPORTED_VERSION,
          "DataVersion "
              + version.value()
              + " is not supported, only "
              + ChunkCodec.SUPPORTED_DATA_VERSION);
    }
  }

  private static ChunkStatus status(CompoundTag root) throws ReadFault {
    if (!(root.entries().get("Status") instanceof StringTag name)) {
      throw new ReadFault(RefusedCause.INVALID_FIELD, "Status is missing or is not a String");
    }
    return ChunkStatus.byId(name.value())
        .orElseThrow(
            () ->
                new ReadFault(
                    RefusedCause.UNSUPPORTED_STATUS,
                    "Status '" + name.value() + "' is unknown or was removed from the game"));
  }

  private static Optional<ChunkPos> position(CompoundTag root) {
    if (root.entries().get("xPos") instanceof IntTag x
        && root.entries().get("zPos") instanceof IntTag z) {
      return Optional.of(new ChunkPos(x.value(), z.value()));
    }
    return Optional.empty();
  }

  private static void checkYPos(CompoundTag root, WorldHeight height, Findings findings) {
    if (root.entries().get("yPos") instanceof IntTag yPos) {
      if (yPos.value() != height.minSectionY()) {
        findings.warn(
            WarningCode.YPOS_MISMATCH,
            "yPos "
                + yPos.value()
                + " differs from the lowest section "
                + height.minSectionY()
                + ", ignored");
      }
    } else if (root.entries().containsKey("yPos")) {
      findings.warn(WarningCode.YPOS_WRONG_TYPE, "yPos is not an Int, ignored");
    }
  }
}
