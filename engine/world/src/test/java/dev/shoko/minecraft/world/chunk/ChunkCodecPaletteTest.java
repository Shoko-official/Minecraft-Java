package dev.shoko.minecraft.world.chunk;

import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.list;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static dev.shoko.minecraft.world.support.Nbt.with;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.ReadWarning;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Nbt;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** BS-R19 to BS-R22 and CF-R11 through the chunk: the variants of vector RG3. */
class ChunkCodecPaletteTest {

  private static final int OAK_LOG_Y = 22;

  private static ReadResult.Present<Chunk> read(String variant) {
    return Chunks.present(Chunks.decode(ChunkNbt.chunk(-1, -1, true, variant)));
  }

  private static void assertBottomLayerIs(Chunk chunk, int stateId) {
    for (int index = 0; index < 256; index++) {
      assertThat(chunk.section(-4).blockStates().get(index).id())
          .as("index " + index)
          .isEqualTo(stateId);
    }
    for (int index = 256; index < 4096; index++) {
      assertThat(chunk.section(-4).blockStates().get(index).id()).isZero();
    }
  }

  private static CompoundTag withBottomPalette(NbtTag secondEntry) {
    CompoundTag root = ChunkNbt.chunk(-1, -1, true, "bedrock");
    List<NbtTag> sections = new ArrayList<>(root.getList("sections").elements());
    CompoundTag bottom = (CompoundTag) sections.getFirst();
    CompoundTag states = bottom.getCompound("block_states");
    states = with(states, "palette", list(ChunkNbt.air(), secondEntry));
    sections.set(0, with(bottom, "block_states", states));
    return with(root, "sections", Nbt.listOf(sections));
  }

  @Test
  void stringPaletteIsReadAsDefaultStates() {
    // spec: BS-R19, RG3 string_palette (List of String: bedrock at y = -64)
    ReadResult.Present<Chunk> result = read("string_palette");

    assertThat(result.warnings()).isEmpty();
    assertBottomLayerIs(result.value(), 4);
  }

  @Test
  void provisionalPartialPropertiesTakeTheDefaultStateValues() {
    // spec: BS-R20, RG3 partial_props (oak_log without properties is oak_log[axis=y], id 22 here)
    ReadResult.Present<Chunk> result = read("partial_props");

    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.DEFAULTED_PROPERTY);
    assertBottomLayerIs(result.value(), OAK_LOG_Y);
    assertThat(result.value().section(-4).blockStates().get(0).value("axis")).isEqualTo("y");
  }

  @Test
  void provisionalUnknownBlockBecomesAirAndTheChunkStaysPresent() {
    // spec: BS-R21, BS-R22 b, RG3 unknown_block
    ReadResult.Present<Chunk> result = read("unknown_block");

    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.UNKNOWN_BLOCK);
    assertBottomLayerIs(result.value(), 0);
  }

  @Test
  void provisionalWarningNamesTheBlockAndTheSection() {
    // spec: BS-R22 c (the warning names the section, the palette entry, the block)
    ReadWarning warning = read("unknown_block").warnings().getFirst();

    assertThat(warning.message()).contains("probe:unknown");
    assertThat(warning.message().contains("-4") || warning.message().contains("sections[0]"))
        .as(warning.message())
        .isTrue();
  }

  @Test
  void provisionalInvalidValueTakesTheDefaultStatesValue() {
    // spec: BS-R21, RG3 bad_value (oak_log[axis=w] is oak_log[axis=y])
    ReadResult.Present<Chunk> result = read("bad_value");

    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.INVALID_PROPERTY_VALUE);
    assertBottomLayerIs(result.value(), OAK_LOG_Y);
  }

  @Test
  void provisionalUnknownPropertyIsIgnored() {
    // spec: BS-R21 (unknown property ignored with a warning)
    ReadResult.Present<Chunk> result =
        Chunks.present(
            Chunks.decode(
                withBottomPalette(
                    compound(
                        "id",
                        str(Blocks.OAK_LOG),
                        "properties",
                        compound("axis", str("x"), "color", str("red"))))));

    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.UNKNOWN_PROPERTY);
    assertBottomLayerIs(result.value(), 21);
  }

  @Test
  void provisionalEntryOfAnotherTagTypeNeverRefusesTheChunk() {
    // spec: BS-R22 a, b (an Int entry is an unknown block: air and a warning)
    ReadResult.Present<Chunk> result = Chunks.present(Chunks.decode(withBottomPalette(i(3))));

    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.MALFORMED_PALETTE_ENTRY);
    assertBottomLayerIs(result.value(), 0);
  }

  @Test
  void provisionalCompoundWithoutIdNeverRefusesTheChunk() {
    // spec: BS-R22 a
    ReadResult.Present<Chunk> result =
        Chunks.present(
            Chunks.decode(withBottomPalette(compound("properties", compound("axis", str("x"))))));

    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.MALFORMED_PALETTE_ENTRY);
    assertBottomLayerIs(result.value(), 0);
  }

  @Test
  void correctedEntriesStayDistinctForReadingAndAreCompactedWhenWriting() {
    // spec: BS-R22 d (two entries corrected to the same state keep indices valid; writing compacts)
    Chunk chunk = read("unknown_block").value();

    CompoundTag written = Chunks.codec().encode(chunk);

    CompoundTag bottom = (CompoundTag) written.getList("sections").elements().getFirst();
    CompoundTag states = bottom.getCompound("block_states");
    assertThat(states.getList("palette").elements()).hasSize(1);
    assertThat(states.entries()).doesNotContainKey("data");
  }

  @Test
  void correctedValueIsWrittenInPlaceOfTheOriginal() {
    // spec: BS-R21 (once saved, the corrected value is written and the original is not kept)
    Chunk chunk = read("bad_value").value();

    CompoundTag written = Chunks.codec().encode(chunk);

    CompoundTag bottom = (CompoundTag) written.getList("sections").elements().getFirst();
    List<NbtTag> palette = bottom.getCompound("block_states").getList("palette").elements();
    assertThat(palette)
        .contains(compound("id", str(Blocks.OAK_LOG), "properties", compound("axis", str("y"))));
    assertThat(palette)
        .doesNotContain(
            compound("id", str(Blocks.OAK_LOG), "properties", compound("axis", str("w"))));
  }

  @Test
  void unknownBiomeBecomesPlainsWithAWarning() {
    // spec: CF-R11, RG3 unknown_biome
    ReadResult.Present<Chunk> result = read("unknown_biome");

    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.UNKNOWN_BIOME);
    for (int cell = 0; cell < 64; cell++) {
      assertThat(result.value().section(-4).biomes().get(cell)).isEqualTo(Chunks.PLAINS);
    }
    assertBottomLayerIs(result.value(), 4);
  }

  @Test
  void biomeEntryAsACompoundWithANameIsAccepted() {
    // spec: CF-R11 (the reader also accepts a Compound with Name)
    CompoundTag root = ChunkNbt.chunk(-1, -1, true, "bedrock");
    List<NbtTag> sections = new ArrayList<>(root.getList("sections").elements());
    CompoundTag biomes = compound("palette", list(compound("Name", str(Chunks.DESERT))));
    sections.set(1, with((CompoundTag) sections.get(1), "biomes", biomes));

    Chunk chunk = Chunks.decodeClean(with(root, "sections", Nbt.listOf(sections)));

    assertThat(chunk.section(-3).biomes().get(10)).isEqualTo(Chunks.DESERT);
  }

  @Test
  void sectionWithoutBlockStatesIsAllAir() {
    // spec: CF-R22, RG3 missing_block_states (provisional: air everywhere)
    ReadResult.Present<Chunk> result = read("missing_block_states");

    assertThat(result.warnings()).isEmpty();
    assertBottomLayerIs(result.value(), 0);
  }

  @Test
  void sectionWithoutBiomesIsAllPlains() {
    // spec: CF-R22, RG3 missing_biomes (provisional: minecraft:plains)
    ReadResult.Present<Chunk> result = read("missing_biomes");

    assertThat(result.warnings()).isEmpty();
    for (int cell = 0; cell < 64; cell++) {
      assertThat(result.value().section(-4).biomes().get(cell)).isEqualTo(Chunks.PLAINS);
    }
    assertBottomLayerIs(result.value(), 4);
  }

  @Test
  void largePaletteOfStatesIsReadWithItsEntries() {
    // spec: CF-R7 to CF-R9 through the chunk: 17 distinct states need 5 bits and 342 longs
    CompoundTag root = ChunkNbt.chunk(-1, -1, true, "bedrock");
    List<NbtTag> palette = new ArrayList<>();
    int[] indices = new int[4096];
    for (int k = 0; k < 17; k++) {
      palette.add(
          compound("id", str(Blocks.WATER), "properties", compound("level", str("" + k % 16))));
    }
    palette.set(16, compound("id", str(Blocks.STONE)));
    for (int index = 0; index < 4096; index++) {
      indices[index] = index % 17;
    }
    List<NbtTag> sections = new ArrayList<>(root.getList("sections").elements());
    CompoundTag states =
        compound(
            "palette",
            Nbt.listOf(palette),
            "data",
            Nbt.longs(dev.shoko.minecraft.world.support.RefPacking.pack(indices, 5)));
    sections.set(0, with((CompoundTag) sections.get(0), "block_states", states));

    Chunk chunk = Chunks.decodeClean(with(root, "sections", Nbt.listOf(sections)));

    assertThat(chunk.section(-4).blockStates().get(0).value("level")).isEqualTo("0");
    assertThat(chunk.section(-4).blockStates().get(16).block().id()).isEqualTo(Blocks.STONE);
    assertThat(chunk.section(-4).blockStates().get(33).block().id()).isEqualTo(Blocks.STONE);
    assertThat(chunk.section(-4).blockStates().get(5).value("level")).isEqualTo("5");
  }
}
