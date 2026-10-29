package dev.shoko.minecraft.world.chunk;

import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.heightmap.Heightmap;
import dev.shoko.minecraft.world.heightmap.HeightmapType;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.RichChunk;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** CF-R20 to CF-R26, CF-R31 to CF-R34, BS-R16, BS-R17: the tree the writer produces. */
class ChunkCodecWriteTest {

  private static final Set<String> KEYS_OF_THE_SCHEMA =
      Set.of(
          "DataVersion",
          "xPos",
          "zPos",
          "yPos",
          "Status",
          "LastUpdate",
          "InhabitedTime",
          "sections",
          "Heightmaps",
          "block_entities",
          "block_ticks",
          "fluid_ticks",
          "PostProcessing",
          "structures",
          "entities");

  private final BlockStateRegistry registry = Blocks.registry();

  private Chunk simple() {
    return Chunks.empty(registry, new ChunkPos(-1, -1), Chunks.OVERWORLD);
  }

  private static CompoundTag section(CompoundTag written, int y) {
    for (NbtTag tag : written.getList("sections").elements()) {
      if (((CompoundTag) tag).getByte("Y") == y) {
        return (CompoundTag) tag;
      }
    }
    throw new AssertionError("no section " + y);
  }

  @Test
  void scalarFieldsHaveTheTypesOfTheSchema() {
    // spec: CF-R20 (Int, Int, Int, Int, String, Long, Long)
    Chunk chunk = simple();
    chunk.setLastUpdate(77L);
    chunk.setInhabitedTime(88L);

    CompoundTag written = Chunks.codec().encode(chunk);

    assertThat(written.get("DataVersion")).contains(new IntTag(5023));
    assertThat(written.get("xPos")).contains(new IntTag(-1));
    assertThat(written.get("zPos")).contains(new IntTag(-1));
    assertThat(written.get("yPos")).contains(new IntTag(-4));
    assertThat(written.get("Status")).contains(new StringTag("minecraft:full"));
    assertThat(written.get("LastUpdate")).contains(new LongTag(77L));
    assertThat(written.get("InhabitedTime")).contains(new LongTag(88L));
  }

  @Test
  void onlyKeysOfTheSchemaAreWritten() {
    // spec: CF-R21 (the writer writes only the keys it can produce)
    CompoundTag written = Chunks.codec().encode(RichChunk.build(registry));

    assertThat(written.entries().keySet()).isSubsetOf(KEYS_OF_THE_SCHEMA);
    assertThat(written.entries().keySet())
        .contains("DataVersion", "xPos", "zPos", "yPos", "Status", "sections", "Heightmaps");
  }

  @Test
  void sectionListIsCompleteSortedBottomToTopWithByteY() {
    // spec: CF-R22 (all sections of the height, even empty ones, Y is a Byte)
    CompoundTag written = Chunks.codec().encode(simple());

    List<NbtTag> sections = written.getList("sections").elements();
    assertThat(sections).hasSize(24);
    for (int rank = 0; rank < 24; rank++) {
      CompoundTag section = (CompoundTag) sections.get(rank);
      assertThat(section.get("Y")).contains(new ByteTag((byte) (rank - 4)));
      assertThat(section.get("block_states")).isPresent();
      assertThat(section.get("biomes")).isPresent();
    }
  }

  @Test
  void uniformSectionIsAPaletteOfOneEntryWithoutData() {
    // spec: CF-R6, BS-R16 (air alone: {id: minecraft:air}, no data, no BlockLight, no SkyLight)
    CompoundTag section = section(Chunks.codec().encode(simple()), 7);

    CompoundTag states = section.getCompound("block_states");
    assertThat(states.getList("palette").elements())
        .containsExactly(compound("id", str(Blocks.AIR)));
    assertThat(states.entries()).doesNotContainKey("data");
    assertThat(section.getCompound("biomes").getList("palette").elements())
        .containsExactly(str(Chunks.PLAINS));
    assertThat(section.getCompound("biomes").entries()).doesNotContainKey("data");
    assertThat(section.entries()).doesNotContainKeys("BlockLight", "SkyLight");
  }

  @Test
  void blockStatesAreWrittenWithAllTheirPropertiesAsStrings() {
    // spec: BS-R16, BS-R17 (default state not elided: grass_block[snowy=false] keeps its
    // properties)
    Chunk chunk = simple();
    chunk.setBlockState(new BlockPos(-1, -64, -1), registry.state(26));
    chunk.setBlockState(new BlockPos(-2, -64, -1), registry.state(3));

    CompoundTag states = section(Chunks.codec().encode(chunk), -4).getCompound("block_states");

    assertThat(states.getList("palette").elements())
        .contains(
            compound(
                "id",
                str(Blocks.CHEST),
                "properties",
                compound("facing", str("north"), "type", str("left"), "waterlogged", str("true"))),
            compound("id", str(Blocks.GRASS_BLOCK), "properties", compound("snowy", str("false"))));
  }

  @Test
  void paletteHoldsOnlyTheStatesInUse() {
    // spec: CF-R7 (compacted palettes, no unused entry)
    Chunk chunk = simple();
    chunk.setBlockState(new BlockPos(-1, 100, -1), registry.state(4));
    chunk.setBlockState(new BlockPos(-1, 100, -1), registry.air());

    CompoundTag states = section(Chunks.codec().encode(chunk), 6).getCompound("block_states");

    assertThat(states.getList("palette").elements()).hasSize(1);
    assertThat(states.entries()).doesNotContainKey("data");
  }

  @ParameterizedTest
  @CsvSource({"2,256", "16,256", "17,342", "33,410", "257,586", "1025,820"})
  void dataLengthFollowsTheBitsOfThePaletteSize(int distinct, int longs) {
    // spec: CF-R7, CF-R9 (2 states: 256 longs; 17 states: 342; 257 states: 586; 1025: 820)
    Chunk chunk = simple();
    for (int index = 0; index < distinct; index++) {
      chunk.section(0).blockStates().set(index, registry.state(index));
    }

    CompoundTag states = section(Chunks.codec().encode(chunk), 0).getCompound("block_states");

    assertThat(states.getList("palette").elements()).hasSize(distinct);
    assertThat(states.getLongArray("data")).hasSize(longs);
  }

  @Test
  void biomeDataIsOneLongForTwoBiomes() {
    // spec: CF-R7, CF-R9 (biomes: 2 entries, 1 bit, 1 long)
    Chunk chunk = simple();
    chunk.setBiome(new BlockPos(-1, -64, -1), Chunks.DESERT);

    CompoundTag biomes = section(Chunks.codec().encode(chunk), -4).getCompound("biomes");

    assertThat(biomes.getList("palette").elements()).hasSize(2);
    assertThat(biomes.getLongArray("data")).hasSize(1);
  }

  @Test
  void storedLightIsWrittenAsByteArraysOfTheSectionOnly() {
    // spec: CF-R14, CF-R15, CF-R30 (BlockLight and SkyLight written when stored, never derived)
    Chunk chunk = simple();
    chunk.section(2).light().setBlockLight(NibbleArray.filled(7));

    CompoundTag written = Chunks.codec().encode(chunk);

    assertThat(section(written, 2).get("BlockLight")).contains(ByteArrayTag.of(filledBytes(0x77)));
    assertThat(section(written, 2).entries()).doesNotContainKey("SkyLight");
    assertThat(section(written, 3).entries()).doesNotContainKeys("BlockLight", "SkyLight");
  }

  private static byte[] filledBytes(int value) {
    byte[] bytes = new byte[2048];
    java.util.Arrays.fill(bytes, (byte) value);
    return bytes;
  }

  @Test
  void heightmapsKeyIsAlwaysWrittenAndHoldsTheValidTypesOnly() {
    // spec: CF-R34 (Heightmaps always written, possibly empty; unknown types are not)
    Chunk none = simple();
    Chunk some = simple();
    some.heightmaps().put(HeightmapType.OCEAN_FLOOR, Heightmap.filled(Chunks.OVERWORLD, 4));

    CompoundTag writtenNone = Chunks.codec().encode(none);
    CompoundTag writtenSome = Chunks.codec().encode(some);

    assertThat(writtenNone.get("Heightmaps")).contains(CompoundTag.empty());
    assertThat(writtenSome.getCompound("Heightmaps").entries().keySet())
        .containsExactly("OCEAN_FLOOR");
    assertThat(writtenSome.getCompound("Heightmaps").get("OCEAN_FLOOR"))
        .containsInstanceOf(LongArrayTag.class);
    assertThat(writtenSome.getCompound("Heightmaps").getLongArray("OCEAN_FLOOR"))
        .isEqualTo(Heightmap.filled(Chunks.OVERWORLD, 4).toLongs());
  }

  @Test
  void postProcessingHasOneListPerSectionEvenWhenEmpty() {
    // spec: CF-R24 (one list per section of the world height, empty ones included)
    CompoundTag written = Chunks.codec().encode(simple());

    ListTag lists = written.getList("PostProcessing");
    assertThat(lists.elements()).hasSize(24);
    assertThat(lists.elements()).allMatch(tag -> ((ListTag) tag).elements().isEmpty());
  }

  @Test
  void referencesAreWrittenUnderCapitalReferencesAsPackedLongs() {
    // spec: CF-R25 (References at writing), ST1 (long = (cz << 32) | (cx & 0xFFFFFFFF))
    CompoundTag written = Chunks.codec().encode(RichChunk.build(registry));

    CompoundTag references = written.getCompound("structures").getCompound("References");
    assertThat(references.getLongArray("minecraft:village")).containsExactly(12884901887L, 0L);
    assertThat(written.getCompound("structures").getCompound("starts"))
        .isEqualTo(compound("minecraft:village", compound("id", str("INVALID"))));
  }

  @Test
  void entitiesAreWrittenForAProtoChunkOnly() {
    // spec: CF-R21 (entities only in a proto-chunk)
    CompoundTag pig = compound("id", str("minecraft:pig"));
    Chunk chunk = simple();
    ChunkAttachments base = chunk.attachments();
    chunk.setAttachments(
        new ChunkAttachments(
            base.blockEntities(),
            base.blockTicks(),
            base.fluidTicks(),
            base.postProcessing(),
            base.structures(),
            List.of(pig),
            base.lightOnlySections()));

    assertThat(Chunks.codec().encode(chunk).entries()).doesNotContainKey("entities");
    chunk.setStatus(ChunkStatus.FEATURES);
    CompoundTag proto = Chunks.codec().encode(chunk);
    assertThat(proto.getList("entities").elements()).containsExactly(pig);
    assertThat(proto.get("Status")).contains(str("minecraft:features"));
  }

  @Test
  void blockEntitiesAndTicksAreWrittenWithTheirKeys() {
    // spec: CF-R23, CF-R26
    CompoundTag written = Chunks.codec().encode(RichChunk.build(registry));

    assertThat(written.getList("block_entities").elements()).hasSize(1);
    CompoundTag tick = (CompoundTag) written.getList("block_ticks").elements().getFirst();
    assertThat(tick.getString("i")).isEqualTo("minecraft:stone");
    assertThat(tick.getInt("p")).isEqualTo(1);
    assertThat(tick.getInt("t")).isEqualTo(-2);
    assertThat(tick.getInt("x")).isEqualTo(-48);
    assertThat(tick.getInt("y")).isEqualTo(-64);
    assertThat(tick.getInt("z")).isEqualTo(127);
    assertThat(written.getList("fluid_ticks").elements()).hasSize(1);
  }

  @Test
  void writingTheSameChunkTwiceGivesTheSameBytes() {
    // spec: determinism (same input, same output, byte for byte)
    Chunk chunk = RichChunk.build(registry);

    byte[] first = ChunkNbt.bytes(Chunks.codec().encode(chunk));
    byte[] second = ChunkNbt.bytes(Chunks.codec().encode(chunk));

    assertThat(second).isEqualTo(first);
  }

  @Test
  void writingEqualChunksGivesTheSameBytes() {
    // spec: determinism (two equal chunks built twice give the same output)
    byte[] first = ChunkNbt.bytes(Chunks.codec().encode(RichChunk.build(registry)));
    byte[] second = ChunkNbt.bytes(Chunks.codec().encode(RichChunk.build(Blocks.registry())));

    assertThat(second).isEqualTo(first);
  }
}
