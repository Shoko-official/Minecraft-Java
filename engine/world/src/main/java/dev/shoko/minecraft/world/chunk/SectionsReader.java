package dev.shoko.minecraft.world.chunk;

import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.RefusedCause;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.WarningSink;
import dev.shoko.minecraft.world.biome.BiomeCatalog;
import dev.shoko.minecraft.world.biome.BiomeEntryCodec;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.block.BlockStateEntryCodec;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.palette.ContainerLayout;
import dev.shoko.minecraft.world.palette.PaletteEntryCodec;
import dev.shoko.minecraft.world.palette.PaletteFormatException;
import dev.shoko.minecraft.world.palette.PalettedContainer;
import dev.shoko.minecraft.world.palette.PalettedContainerCodec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Internal: reads the {@code sections} list of a chunk root and reconciles it with the world height
 * (CF-R22, CF-R31, CF-R32).
 */
final class SectionsReader {

  /** The sections of the world height, bottom to top, and the light sections kept aside. */
  record Sections(
      List<ChunkSection> sections, SequencedMap<Integer, LightOnlySection> lightOnlySections) {}

  private record Listed(int index, CompoundTag tag) {}

  private final BlockStateRegistry blocks;
  private final BiomeCatalog biomes;
  private final WorldHeight height;
  private final Findings findings;

  SectionsReader(
      BlockStateRegistry blocks, BiomeCatalog biomes, WorldHeight height, Findings findings) {
    this.blocks = blocks;
    this.biomes = biomes;
    this.height = height;
    this.findings = findings;
  }

  Sections read(CompoundTag root) throws ReadFault {
    if (!(root.entries().get("sections") instanceof ListTag list)) {
      throw new ReadFault(
          RefusedCause.INVALID_FIELD, "sections is missing or is not a List of Compound");
    }
    List<CompoundTag> entries = new ArrayList<>(list.elements().size());
    for (int index = 0; index < list.elements().size(); index++) {
      if (!(list.elements().get(index) instanceof CompoundTag compound)) {
        throw new ReadFault(
            RefusedCause.INVALID_FIELD, "sections[" + index + "] is not a Compound");
      }
      entries.add(compound);
    }
    int lowLight = height.minSectionY() - 1;
    int highLight = height.maxSectionY() + 1;
    Map<Integer, Listed> normal = new TreeMap<>();
    Map<Integer, Listed> light = new TreeMap<>();
    Set<Integer> seen = new HashSet<>();
    for (int index = 0; index < entries.size(); index++) {
      int y = sectionY(entries.get(index), index);
      if (!seen.add(y)) {
        throw new ReadFault(RefusedCause.INVALID_SECTION, "sections[" + index + "] repeats Y " + y);
      }
      Listed listed = new Listed(index, entries.get(index));
      if (y >= height.minSectionY() && y <= height.maxSectionY()) {
        normal.put(y, listed);
      } else if (y == lowLight || y == highLight) {
        light.put(y, listed);
      } else {
        findings.warn(
            WarningCode.SECTION_IGNORED,
            "sections[" + index + "] has Y " + y + ", too far from the world height, ignored");
      }
    }
    return new Sections(worldSections(normal, entries.isEmpty()), lightSections(light));
  }

  private static int sectionY(CompoundTag section, int index) throws ReadFault {
    if (section.entries().get("Y") instanceof ByteTag y) {
      return y.value();
    }
    throw new ReadFault(
        RefusedCause.INVALID_SECTION, "sections[" + index + "] has no Y, or Y is not a Byte");
  }

  private List<ChunkSection> worldSections(Map<Integer, Listed> normal, boolean emptyList)
      throws ReadFault {
    if (emptyList) {
      findings.warn(
          WarningCode.SECTIONS_EMPTY, "sections is empty, every section is synthesized empty");
    }
    List<ChunkSection> result = new ArrayList<>(height.sectionCount());
    for (int y = height.minSectionY(); y <= height.maxSectionY(); y++) {
      Listed listed = normal.get(y);
      if (listed != null) {
        result.add(readSection(y, listed));
        continue;
      }
      if (!emptyList) {
        findings.warn(
            WarningCode.SECTION_MISSING, "section Y " + y + " is missing, synthesized empty");
      }
      result.add(ChunkSection.uniform(y, blocks.air(), BiomeEntryCodec.FALLBACK_BIOME));
    }
    return result;
  }

  private SequencedMap<Integer, LightOnlySection> lightSections(Map<Integer, Listed> light)
      throws ReadFault {
    SequencedMap<Integer, LightOnlySection> result = new LinkedHashMap<>();
    for (Map.Entry<Integer, Listed> entry : light.entrySet()) {
      CompoundTag tag = entry.getValue().tag();
      String label = label(entry.getValue().index(), entry.getKey());
      SequencedMap<String, NbtTag> others = new LinkedHashMap<>(tag.entries());
      others.remove("Y");
      others.remove("BlockLight");
      others.remove("SkyLight");
      result.put(
          entry.getKey(), new LightOnlySection(readLight(tag, label), CompoundTag.of(others)));
    }
    return result;
  }

  private ChunkSection readSection(int y, Listed listed) throws ReadFault {
    CompoundTag tag = listed.tag();
    String label = label(listed.index(), y);
    PalettedContainer<BlockState> states =
        container(
            tag,
            "block_states",
            label,
            ContainerLayout.BLOCKS,
            sink -> BlockStateEntryCodec.of(blocks, sink),
            blocks.air());
    PalettedContainer<String> cells =
        container(
            tag,
            "biomes",
            label,
            ContainerLayout.BIOMES,
            sink -> BiomeEntryCodec.of(biomes, sink),
            BiomeEntryCodec.FALLBACK_BIOME);
    return ChunkSection.of(y, states, cells, readLight(tag, label));
  }

  private static String label(int index, int y) {
    return "sections[" + index + "] (Y " + y + ") ";
  }

  private <T> PalettedContainer<T> container(
      CompoundTag section,
      String key,
      String label,
      ContainerLayout layout,
      Function<WarningSink, PaletteEntryCodec<T>> codecFor,
      T whenAbsent)
      throws ReadFault {
    NbtTag tag = section.entries().get(key);
    if (tag == null) {
      return PalettedContainer.filled(layout.entryCount(), whenAbsent);
    }
    if (!(tag instanceof CompoundTag compound)) {
      throw new ReadFault(
          RefusedCause.INVALID_CONTAINER, label + key + " is a " + tag.type() + ", not a Compound");
    }
    int[] entry = {0};
    PaletteEntryCodec<T> inner =
        codecFor.apply(
            (code, message) ->
                findings.warn(code, label + key + ".palette[" + entry[0] + "]: " + message));
    PaletteEntryCodec<T> counting =
        new PaletteEntryCodec<>() {
          @Override
          public T decode(NbtTag value) throws PaletteFormatException {
            T decoded = inner.decode(value);
            entry[0]++;
            return decoded;
          }

          @Override
          public NbtTag encode(T value) {
            return inner.encode(value);
          }
        };
    try {
      return PalettedContainerCodec.of(layout, counting).decode(compound);
    } catch (PaletteFormatException e) {
      throw new ReadFault(RefusedCause.INVALID_CONTAINER, label + key + ": " + e.getMessage());
    }
  }

  private SectionLight readLight(CompoundTag section, String label) {
    SectionLight light = SectionLight.none();
    lightArray(section, "BlockLight", label).ifPresent(light::setBlockLight);
    lightArray(section, "SkyLight", label).ifPresent(light::setSkyLight);
    return light;
  }

  private Optional<NibbleArray> lightArray(CompoundTag section, String key, String label) {
    Optional<ByteArrayTag> bytes = findings.optional(section, key, ByteArrayTag.class, label);
    if (bytes.isEmpty()) {
      return Optional.empty();
    }
    if (bytes.get().size() != NibbleArray.BYTE_LENGTH) {
      findings.warn(
          WarningCode.FIELD_WRONG_TYPE,
          label
              + key
              + " has "
              + bytes.get().size()
              + " bytes, "
              + NibbleArray.BYTE_LENGTH
              + " expected, ignored");
      return Optional.empty();
    }
    return Optional.of(NibbleArray.ofBytes(bytes.get().toArray()));
  }
}
