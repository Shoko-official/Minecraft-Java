package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.ShortTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.world.WarningCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * Internal: reads the opaque data of a chunk root, which M3 carries without interpreting it: block
 * entities, scheduled ticks, post-processing, structures and the entities of a proto-chunk (CF-R21,
 * CF-R23 to CF-R26). Whatever is malformed is dropped with a warning, so that what is written back
 * reads without any.
 */
final class AttachmentsReader {

  private static final int POST_PROCESSING_HIGH_NIBBLE = 0xF000;

  private final ChunkPos pos;
  private final int sectionCount;
  private final Findings findings;

  AttachmentsReader(ChunkPos pos, int sectionCount, Findings findings) {
    this.pos = pos;
    this.sectionCount = sectionCount;
    this.findings = findings;
  }

  ChunkAttachments read(
      CompoundTag root, boolean protoChunk, SequencedMap<Integer, LightOnlySection> lightOnly) {
    return new ChunkAttachments(
        List.copyOf(blockEntities(root)),
        List.copyOf(ticks(root, "block_ticks")),
        List.copyOf(ticks(root, "fluid_ticks")),
        postProcessing(root),
        structures(root),
        protoChunk ? entities(root) : List.of(),
        lightOnly);
  }

  private List<CompoundTag> compounds(CompoundTag root, String key) {
    List<CompoundTag> result = new ArrayList<>();
    Optional<ListTag> list = findings.optional(root, key, ListTag.class, "");
    if (list.isEmpty()) {
      return result;
    }
    for (int index = 0; index < list.get().elements().size(); index++) {
      if (list.get().elements().get(index) instanceof CompoundTag compound) {
        result.add(compound);
      } else {
        findings.warn(
            WarningCode.FIELD_WRONG_TYPE, key + "[" + index + "] is not a Compound, ignored");
      }
    }
    return result;
  }

  private List<BlockEntityData> blockEntities(CompoundTag root) {
    List<BlockEntityData> result = new ArrayList<>();
    for (CompoundTag entry : compounds(root, "block_entities")) {
      Optional<BlockPos> at = position(entry);
      if (!(entry.entries().get("id") instanceof StringTag id) || at.isEmpty()) {
        findings.warn(
            WarningCode.FIELD_WRONG_TYPE,
            "block_entities entry without a String id and Int x, y, z, ignored");
      } else if (!at.get().chunk().equals(pos)) {
        findings.warn(
            WarningCode.BLOCK_ENTITY_OUTSIDE_CHUNK,
            "block entity " + id.value() + " at " + at.get() + " is outside chunk " + pos);
      } else {
        result.add(new BlockEntityData(id.value(), at.get(), entry));
      }
    }
    return result;
  }

  private static Optional<BlockPos> position(CompoundTag entry) {
    if (entry.entries().get("x") instanceof IntTag x
        && entry.entries().get("y") instanceof IntTag y
        && entry.entries().get("z") instanceof IntTag z) {
      return Optional.of(new BlockPos(x.value(), y.value(), z.value()));
    }
    return Optional.empty();
  }

  private List<ScheduledTickEntry> ticks(CompoundTag root, String key) {
    List<ScheduledTickEntry> result = new ArrayList<>();
    for (CompoundTag entry : compounds(root, key)) {
      Optional<BlockPos> at = position(entry);
      if (entry.entries().get("i") instanceof StringTag id
          && entry.entries().get("p") instanceof IntTag priority
          && entry.entries().get("t") instanceof IntTag delay
          && at.isPresent()) {
        result.add(new ScheduledTickEntry(id.value(), priority.value(), delay.value(), at.get()));
      } else {
        findings.warn(
            WarningCode.FIELD_WRONG_TYPE,
            key + " entry without String i and Int p, t, x, y, z, ignored");
      }
    }
    return result;
  }

  private List<List<Short>> postProcessing(CompoundTag root) {
    List<List<Short>> result = new ArrayList<>();
    Optional<ListTag> lists = findings.optional(root, "PostProcessing", ListTag.class, "");
    List<NbtTag> given = lists.map(ListTag::elements).orElse(List.of());
    for (int index = 0; index < given.size(); index++) {
      if (index >= sectionCount) {
        findings.warn(
            WarningCode.POST_PROCESSING_LIST_IGNORED,
            "PostProcessing[" + index + "] is beyond the " + sectionCount + " sections, ignored");
      } else {
        result.add(shorts(given.get(index), index));
      }
    }
    while (result.size() < sectionCount) {
      result.add(List.of());
    }
    return List.copyOf(result);
  }

  private List<Short> shorts(NbtTag tag, int index) {
    if (!(tag instanceof ListTag list)) {
      findings.warn(
          WarningCode.FIELD_WRONG_TYPE,
          "PostProcessing[" + index + "] is a " + tag.type() + ", not a List, ignored");
      return List.of();
    }
    List<Short> result = new ArrayList<>();
    for (NbtTag element : list.elements()) {
      if (element instanceof ShortTag value && (value.value() & POST_PROCESSING_HIGH_NIBBLE) == 0) {
        result.add(value.value());
      } else {
        findings.warn(
            WarningCode.POST_PROCESSING_SHORT_IGNORED,
            "PostProcessing[" + index + "] holds a value that is not a position, ignored");
      }
    }
    return List.copyOf(result);
  }

  private StructureData structures(CompoundTag root) {
    SequencedMap<String, List<ChunkPos>> references = new LinkedHashMap<>();
    Optional<CompoundTag> structures = findings.optional(root, "structures", CompoundTag.class, "");
    if (structures.isEmpty()) {
      return new StructureData(CompoundTag.empty(), references);
    }
    CompoundTag starts =
        findings
            .optional(structures.get(), "starts", CompoundTag.class, "structures.")
            .orElse(CompoundTag.empty());
    String key = structures.get().entries().get("References") != null ? "References" : "references";
    findings
        .optional(structures.get(), key, CompoundTag.class, "structures.")
        .ifPresent(compound -> readReferences(compound, references));
    return new StructureData(starts, references);
  }

  private void readReferences(
      CompoundTag compound, SequencedMap<String, List<ChunkPos>> references) {
    compound
        .entries()
        .forEach(
            (structure, tag) -> {
              if (tag instanceof LongArrayTag longs) {
                List<ChunkPos> positions = new ArrayList<>(longs.size());
                for (long packed : longs.toArray()) {
                  positions.add(ChunkPos.unpack(packed));
                }
                references.put(structure, List.copyOf(positions));
              } else {
                findings.warn(
                    WarningCode.FIELD_WRONG_TYPE,
                    "structures references of " + structure + " are not a Long_Array, ignored");
              }
            });
  }

  private List<CompoundTag> entities(CompoundTag root) {
    return List.copyOf(compounds(root, "entities"));
  }
}
