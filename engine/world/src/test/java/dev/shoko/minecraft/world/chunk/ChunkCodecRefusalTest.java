package dev.shoko.minecraft.world.chunk;

import static dev.shoko.minecraft.world.support.Nbt.b;
import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.l;
import static dev.shoko.minecraft.world.support.Nbt.list;
import static dev.shoko.minecraft.world.support.Nbt.s;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static dev.shoko.minecraft.world.support.Nbt.with;
import static dev.shoko.minecraft.world.support.Nbt.without;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.world.AbsentCause;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.RefusedCause;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Nbt;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** CF-R10, CF-R29, CF-R32, CF-R35: the faults that refuse a chunk, with their causes. */
class ChunkCodecRefusalTest {

  private static CompoundTag chunkA() {
    return ChunkNbt.chunk(-1, -1, true, "bedrock");
  }

  private static ReadResult.Refused<Chunk> refused(CompoundTag root) {
    return Chunks.refused(Chunks.decode(root));
  }

  private static ReadResult.Absent<Chunk> absent(CompoundTag root) {
    return Chunks.absent(Chunks.decode(root));
  }

  private static List<NbtTag> sectionsOf(CompoundTag root) {
    return new ArrayList<>(root.getList("sections").elements());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 5022, 5024, 4774, Integer.MAX_VALUE})
  void otherDataVersionIsRefused(int version) {
    // spec: CF-R35 and "Entrees / sorties" (only DataVersion 5023 is read)
    ReadResult.Refused<Chunk> result = refused(with(chunkA(), "DataVersion", i(version)));

    assertThat(result.cause()).isEqualTo(RefusedCause.UNSUPPORTED_VERSION);
    assertThat(result.message()).contains("DataVersion");
  }

  @Test
  void missingOrMistypedDataVersionIsRefused() {
    // spec: CF-R35 (absent or another type than Int: UNSUPPORTED_VERSION)
    assertThat(refused(without(chunkA(), "DataVersion")).cause())
        .isEqualTo(RefusedCause.UNSUPPORTED_VERSION);
    assertThat(refused(with(chunkA(), "DataVersion", l(5023))).cause())
        .isEqualTo(RefusedCause.UNSUPPORTED_VERSION);
    assertThat(refused(with(chunkA(), "DataVersion", str("5023"))).cause())
        .isEqualTo(RefusedCause.UNSUPPORTED_VERSION);
    assertThat(refused(with(chunkA(), "DataVersion", s(5023))).cause())
        .isEqualTo(RefusedCause.UNSUPPORTED_VERSION);
  }

  @Test
  void missingOrMistypedStatusIsRefusedAsAnInvalidField() {
    // spec: CF-R35 (Status absent or not a String: INVALID_FIELD)
    ReadResult.Refused<Chunk> missing = refused(without(chunkA(), "Status"));

    assertThat(missing.cause()).isEqualTo(RefusedCause.INVALID_FIELD);
    assertThat(missing.message()).contains("Status");
    assertThat(refused(with(chunkA(), "Status", i(9))).cause())
        .isEqualTo(RefusedCause.INVALID_FIELD);
    assertThat(refused(with(chunkA(), "Status", list(str("minecraft:full")))).cause())
        .isEqualTo(RefusedCause.INVALID_FIELD);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "minecraft:noise",
        "minecraft:surface",
        "minecraft:carvers",
        "minecraft:liquid_carvers",
        "minecraft:heightmaps",
        "minecraft:foo",
        ""
      })
  void removedAndUnknownStatusesAreRefused(String status) {
    // spec: CF-R29 (noise, surface and carvers come from an earlier version: refused)
    ReadResult.Refused<Chunk> result = refused(with(chunkA(), "Status", str(status)));

    assertThat(result.cause()).isEqualTo(RefusedCause.UNSUPPORTED_STATUS);
    assertThat(result.message()).contains("Status");
  }

  @Test
  void missingSectionsKeyIsRefusedAsAnInvalidField() {
    // spec: CF-R32 a, RG4 no_sections_key
    ReadResult.Refused<Chunk> result = refused(ChunkNbt.chunk(-1, -1, true, "no_sections_key"));

    assertThat(result.cause()).isEqualTo(RefusedCause.INVALID_FIELD);
    assertThat(result.message()).contains("sections");
  }

  @Test
  void sectionsThatAreNotAListOfCompoundsAreRefused() {
    // spec: CF-R32 a (a value that is not a List of Compound)
    assertThat(refused(with(chunkA(), "sections", compound("a", i(1)))).cause())
        .isEqualTo(RefusedCause.INVALID_FIELD);
    assertThat(refused(with(chunkA(), "sections", str("sections"))).cause())
        .isEqualTo(RefusedCause.INVALID_FIELD);
    assertThat(refused(with(chunkA(), "sections", list(i(1), i(2)))).cause())
        .isEqualTo(RefusedCause.INVALID_FIELD);
    List<NbtTag> mixed = sectionsOf(chunkA());
    mixed.add(i(3));
    assertThat(refused(with(chunkA(), "sections", Nbt.listOf(mixed))).cause())
        .isEqualTo(RefusedCause.INVALID_FIELD);
  }

  @Test
  void sectionWithoutYIsRefusedWithItsRank() {
    // spec: CF-R32 b, RG4 no_section_y (the entry of rank 1 has no Y)
    ReadResult.Refused<Chunk> result = refused(ChunkNbt.chunk(-1, -1, true, "no_section_y"));

    assertThat(result.cause()).isEqualTo(RefusedCause.INVALID_SECTION);
    assertThat(result.message()).contains("1");
  }

  @Test
  void sectionWhoseYIsNotAByteIsRefused() {
    // spec: CF-R32 b (Y not a Byte)
    for (NbtTag wrong : new NbtTag[] {i(0), s(0), l(0), str("0")}) {
      List<NbtTag> sections = sectionsOf(chunkA());
      sections.set(4, with((CompoundTag) sections.get(4), "Y", wrong));

      ReadResult.Refused<Chunk> result = refused(with(chunkA(), "sections", Nbt.listOf(sections)));

      assertThat(result.cause()).isEqualTo(RefusedCause.INVALID_SECTION);
    }
  }

  @Test
  void duplicateYIsRefusedWithTheY() {
    // spec: CF-R32 c, RG4 duplicate_section
    ReadResult.Refused<Chunk> result = refused(ChunkNbt.chunk(-1, -1, true, "duplicate_section"));

    assertThat(result.cause()).isEqualTo(RefusedCause.INVALID_SECTION);
    assertThat(result.message()).contains("-4");
  }

  @Test
  void duplicateYInTheMiddleOfTheListIsRefused() {
    // spec: CF-R32 c (two entries with the same Y, wherever they are in the list)
    List<NbtTag> sections = sectionsOf(chunkA());
    sections.add(10, ChunkNbt.airSection(7));

    ReadResult.Refused<Chunk> result = refused(with(chunkA(), "sections", Nbt.listOf(sections)));

    assertThat(result.cause()).isEqualTo(RefusedCause.INVALID_SECTION);
    assertThat(result.message()).contains("7");
  }

  @Test
  void invalidContainersAreRefusedWithTheContainerNamed() {
    // spec: CF-R10, RG3 bad_length, empty_palette, index_overflow
    for (String variant : new String[] {"bad_length", "empty_palette", "index_overflow"}) {
      ReadResult.Refused<Chunk> result = refused(ChunkNbt.chunk(-1, -1, true, variant));

      assertThat(result.cause()).as(variant).isEqualTo(RefusedCause.INVALID_CONTAINER);
      assertThat(result.message()).as(variant).contains("block_states");
    }
  }

  @Test
  void invalidBiomeContainerIsRefused() {
    // spec: CF-R10 (a palette of two entries without data), biomes container
    List<NbtTag> sections = sectionsOf(chunkA());
    CompoundTag broken =
        with(
            (CompoundTag) sections.get(0),
            "biomes",
            compound("palette", list(str(Chunks.PLAINS), str(Chunks.DESERT))));
    sections.set(0, broken);

    ReadResult.Refused<Chunk> result = refused(with(chunkA(), "sections", Nbt.listOf(sections)));

    assertThat(result.cause()).isEqualTo(RefusedCause.INVALID_CONTAINER);
    assertThat(result.message()).contains("biomes");
  }

  @Test
  void refusalNamesTheFaultyFieldRatherThanBeingEmpty() {
    // spec: AR-R26 (a refusal carries a message naming the field)
    assertThat(refused(without(chunkA(), "sections")).message()).isNotBlank();
    assertThat(refused(without(chunkA(), "DataVersion")).message()).isNotBlank();
  }

  @ParameterizedTest
  @ValueSource(strings = {"xPos", "zPos"})
  void missingPositionMakesTheChunkAbsentAsAPositionMismatch(String key) {
    // spec: CF-R35 (xPos or zPos absent: POSITION_MISMATCH)
    ReadResult.Absent<Chunk> result = absent(without(chunkA(), key));

    assertThat(result.cause()).isEqualTo(AbsentCause.POSITION_MISMATCH);
  }

  @ParameterizedTest
  @ValueSource(strings = {"xPos", "zPos"})
  void mistypedPositionMakesTheChunkAbsentAsAPositionMismatch(String key) {
    // spec: CF-R35 (xPos or zPos not an Int: POSITION_MISMATCH)
    assertThat(absent(with(chunkA(), key, l(-1))).cause()).isEqualTo(AbsentCause.POSITION_MISMATCH);
    assertThat(absent(with(chunkA(), key, str("-1"))).cause())
        .isEqualTo(AbsentCause.POSITION_MISMATCH);
    assertThat(absent(with(chunkA(), key, b(-1))).cause()).isEqualTo(AbsentCause.POSITION_MISMATCH);
  }

  @Test
  void aPositionThatIsPresentIsNotCheckedByTheCodec() {
    // spec: ChunkCodec.decode Javadoc (whether a present position is the one expected is for the
    // caller)
    ReadResult.Present<Chunk> result =
        Chunks.present(Chunks.decode(ChunkNbt.chunk(-2, -1, true, "bedrock")));

    assertThat(result.value().pos().x()).isEqualTo(-2);
  }

  @Test
  void emptySectionListIsNotRefused() {
    // spec: CF-R32 g (an empty List is completed, only a missing key is refused)
    ReadResult<Chunk> result = Chunks.decode(with(chunkA(), "sections", ListTag.empty()));

    assertThat(result).isInstanceOf(ReadResult.Present.class);
  }
}
