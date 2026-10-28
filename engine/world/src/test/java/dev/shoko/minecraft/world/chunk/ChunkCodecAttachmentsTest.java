package dev.shoko.minecraft.world.chunk;

import static dev.shoko.minecraft.world.support.Nbt.b;
import static dev.shoko.minecraft.world.support.Nbt.compound;
import static dev.shoko.minecraft.world.support.Nbt.i;
import static dev.shoko.minecraft.world.support.Nbt.list;
import static dev.shoko.minecraft.world.support.Nbt.longs;
import static dev.shoko.minecraft.world.support.Nbt.s;
import static dev.shoko.minecraft.world.support.Nbt.str;
import static dev.shoko.minecraft.world.support.Nbt.with;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.LocalIndex;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.ReadWarning;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Nbt;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CF-R21, CF-R23 to CF-R26: what a chunk carries without interpreting it. */
class ChunkCodecAttachmentsTest {

  private static CompoundTag chunkA() {
    return ChunkNbt.chunk(-1, -1, true, "bedrock");
  }

  private static ReadResult.Present<Chunk> read(CompoundTag root) {
    return Chunks.present(Chunks.decode(root));
  }

  private static CompoundTag tick(String id, int priority, int delay, int x, int y, int z) {
    return compound("i", str(id), "p", i(priority), "t", i(delay), "x", i(x), "y", i(y), "z", i(z));
  }

  private static CompoundTag blockEntity(String id, int x, int y, int z) {
    return compound("id", str(id), "x", i(x), "y", i(y), "z", i(z), "Lock", str("key"));
  }

  @Test
  void blockEntitiesInsideTheChunkAreKeptInFileOrder() {
    // spec: CF-R26 (id, x, y, z and the rest kept as read, ADR 0005 opaque NBT)
    CompoundTag first = blockEntity("minecraft:chest", -5, 10, -3);
    CompoundTag second = blockEntity("minecraft:furnace", -16, -64, -1);
    CompoundTag root = with(chunkA(), "block_entities", list(first, second));

    ReadResult.Present<Chunk> result = read(root);

    assertThat(result.warnings()).isEmpty();
    assertThat(result.value().attachments().blockEntities())
        .containsExactly(
            new BlockEntityData("minecraft:chest", new BlockPos(-5, 10, -3), first),
            new BlockEntityData("minecraft:furnace", new BlockPos(-16, -64, -1), second));
  }

  @Test
  void provisionalBlockEntityOutsideTheChunkIsIgnoredWithAWarning() {
    // spec: CF-R26 (outside the chunk: ignored with a warning)
    CompoundTag inside = blockEntity("minecraft:chest", -1, 0, -1);
    CompoundTag outside = blockEntity("minecraft:chest", 0, 0, 0);
    CompoundTag farAway = blockEntity("minecraft:chest", -100, 0, -1);
    CompoundTag root = with(chunkA(), "block_entities", list(outside, inside, farAway));

    ReadResult.Present<Chunk> result = read(root);

    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(
            WarningCode.BLOCK_ENTITY_OUTSIDE_CHUNK, WarningCode.BLOCK_ENTITY_OUTSIDE_CHUNK);
    assertThat(result.value().attachments().blockEntities())
        .extracting(BlockEntityData::pos)
        .containsExactly(new BlockPos(-1, 0, -1));
  }

  @Test
  void scheduledTicksAreReadWithEveryField() {
    // spec: CF-R23 (i, p, t, x, y, z; t is negative when late)
    CompoundTag root =
        with(
            with(
                chunkA(),
                "block_ticks",
                list(
                    tick("minecraft:stone", 0, 5, -5, 3, -9),
                    tick("minecraft:dirt", -1, -3, -1, 100, -16))),
            "fluid_ticks",
            list(tick("minecraft:water", 2, 7, -16, -64, -16)));

    ReadResult.Present<Chunk> result = read(root);

    assertThat(result.warnings()).isEmpty();
    assertThat(result.value().attachments().blockTicks())
        .containsExactly(
            new ScheduledTickEntry("minecraft:stone", 0, 5, new BlockPos(-5, 3, -9)),
            new ScheduledTickEntry("minecraft:dirt", -1, -3, new BlockPos(-1, 100, -16)));
    assertThat(result.value().attachments().fluidTicks())
        .containsExactly(
            new ScheduledTickEntry("minecraft:water", 2, 7, new BlockPos(-16, -64, -16)));
  }

  @Test
  void postProcessingOfVectorPp1IsReadWithTheIgnoredShortReported() {
    // spec: PP1, RG4 postprocessing (lists 0 and 4 hold 2307 and 2387, the Short 4096 is ignored)
    ReadResult.Present<Chunk> result = read(ChunkNbt.chunk(-1, -1, true, "postprocessing"));

    List<List<Short>> lists = result.value().attachments().postProcessing();
    assertThat(lists).hasSize(24);
    assertThat(lists.get(0)).containsExactly((short) 2307);
    assertThat(lists.get(4)).containsExactly((short) 2387);
    assertThat(lists.get(5)).isEmpty();
    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.POST_PROCESSING_SHORT_IGNORED);
  }

  @Test
  void postProcessingShortsDesignateBlocksInOrder0Zyx() {
    // spec: CF-R24, PP1 (2307 is (3, 0, 9) of section -4: bedrock; 2387 is (3, 5, 9) of section 0:
    // air)
    Chunk chunk = read(ChunkNbt.chunk(-1, -1, true, "postprocessing")).value();

    int first = chunk.attachments().postProcessing().get(0).getFirst();
    int second = chunk.attachments().postProcessing().get(4).getFirst();

    assertThat(
            chunk
                .section(-4)
                .blockStates()
                .get(LocalIndex.sectionStateIndex(first & 15, (first >> 4) & 15, (first >> 8) & 15))
                .id())
        .isEqualTo(4);
    assertThat(
            chunk
                .section(0)
                .blockStates()
                .get(
                    LocalIndex.sectionStateIndex(
                        second & 15, (second >> 4) & 15, (second >> 8) & 15))
                .id())
        .isZero();
    assertThat(first).isEqualTo(LocalIndex.postProcessingIndex(3, 0, 9));
  }

  @Test
  void provisionalShortsWithAHighNibbleAreIgnoredEachWithAWarning() {
    // spec: CF-R24 (high 4 bits not 0: ignored with a warning; 4095 and 0 are kept; order kept)
    List<ListTag> lists = new ArrayList<>();
    for (int k = 0; k < 24; k++) {
      lists.add(ListTag.empty());
    }
    lists.set(2, list(s(4095), s(0x1000), s(0), s(-4096), s(17), s(0x2fff)));

    ReadResult.Present<Chunk> result = read(with(chunkA(), "PostProcessing", Nbt.listOf(lists)));

    assertThat(result.value().attachments().postProcessing().get(2))
        .containsExactly((short) 4095, (short) 0, (short) 17);
    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(
            WarningCode.POST_PROCESSING_SHORT_IGNORED,
            WarningCode.POST_PROCESSING_SHORT_IGNORED,
            WarningCode.POST_PROCESSING_SHORT_IGNORED);
  }

  @Test
  void provisionalFewerListsThanSectionsLeaveTheMissingOnesEmpty() {
    // spec: CF-R24 (fewer lists than sections: the missing lists are empty)
    ListTag three = list(list(s(1)), ListTag.empty(), list(s(2), s(3)));

    ReadResult.Present<Chunk> result = read(with(chunkA(), "PostProcessing", three));

    List<List<Short>> lists = result.value().attachments().postProcessing();
    assertThat(lists).hasSize(24);
    assertThat(lists.get(0)).containsExactly((short) 1);
    assertThat(lists.get(2)).containsExactly((short) 2, (short) 3);
    assertThat(lists.subList(3, 24)).allMatch(List::isEmpty);
    assertThat(result.warnings()).isEmpty();
  }

  @Test
  void provisionalMoreListsThanSectionsAreIgnoredWithAWarning() {
    // spec: CF-R24 (extra lists ignored with a warning)
    List<ListTag> lists = new ArrayList<>();
    for (int k = 0; k < 26; k++) {
      lists.add(list(s(k)));
    }

    ReadResult.Present<Chunk> result = read(with(chunkA(), "PostProcessing", Nbt.listOf(lists)));

    assertThat(result.value().attachments().postProcessing()).hasSize(24);
    assertThat(result.value().attachments().postProcessing().get(23)).containsExactly((short) 23);
    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .contains(WarningCode.POST_PROCESSING_LIST_IGNORED)
        .containsOnly(WarningCode.POST_PROCESSING_LIST_IGNORED);
  }

  @Test
  void referencesAreReadFromEitherKeySpelling() {
    // spec: CF-R25 ("References" or "references" at reading), ST1 (long = (cz << 32) | (cx &
    // 0xFFFFFFFF))
    for (String key : new String[] {"References", "references"}) {
      CompoundTag structures =
          compound(
              "starts",
              compound(),
              key,
              compound(
                  "minecraft:village",
                  longs(0L, -1L, 12884901887L, -4294967293L),
                  "minecraft:mineshaft",
                  longs(new ChunkPos(7, -9).pack())));

      StructureData data =
          read(with(chunkA(), "structures", structures)).value().attachments().structures();

      assertThat(data.references().get("minecraft:village"))
          .containsExactly(
              new ChunkPos(0, 0), new ChunkPos(-1, -1), new ChunkPos(-1, 2), new ChunkPos(3, -1));
      assertThat(data.references().get("minecraft:mineshaft")).containsExactly(new ChunkPos(7, -9));
      assertThat(List.copyOf(data.references().keySet()))
          .containsExactly("minecraft:village", "minecraft:mineshaft");
    }
  }

  @Test
  void startsAreKeptAsOpaqueNbt() {
    // spec: CF-R25 (starts is opaque; a finished structure has id INVALID)
    CompoundTag starts =
        compound(
            "minecraft:village",
            compound("id", str("INVALID")),
            "minecraft:stronghold",
            compound("id", str("minecraft:stronghold"), "ChunkX", i(4), "Children", list()));

    StructureData data =
        read(with(chunkA(), "structures", compound("starts", starts)))
            .value()
            .attachments()
            .structures();

    assertThat(data.starts()).isEqualTo(starts);
  }

  @Test
  void entitiesOfAProtoChunkAreRead() {
    // spec: CF-R21 (entities, only in a proto-chunk)
    CompoundTag pig = compound("id", str("minecraft:pig"), "Pos", list());
    CompoundTag root =
        with(with(chunkA(), "Status", str("minecraft:features")), "entities", list(pig));

    Chunk chunk = Chunks.decodeClean(root);

    assertThat(chunk.status()).isEqualTo(ChunkStatus.FEATURES);
    assertThat(chunk.attachments().entities()).containsExactly(pig);
  }

  @Test
  void entitiesOfAFullChunkAreNotRead() {
    // spec: CF-R21 (a complete chunk keeps its entities in entities/ files: the key is not read)
    CompoundTag pig = compound("id", str("minecraft:pig"));

    Chunk chunk = Chunks.decodeClean(with(chunkA(), "entities", list(pig)));

    assertThat(chunk.attachments().entities()).isEmpty();
  }

  @Test
  void blockEntityKindOfTheMadeUpDataIsOpaque() {
    // spec: ADR 0005 (the Compound is kept as read, whatever it holds)
    CompoundTag chest = blockEntity("minecraft:chest", -2, 0, -2);
    CompoundTag weird = with(chest, "Extra", compound("deep", list(b(1), b(2))));

    BlockEntityData data =
        read(with(chunkA(), "block_entities", list(weird)))
            .value()
            .attachments()
            .blockEntities()
            .getFirst();

    assertThat(data.data()).isEqualTo(weird);
    assertThat(data.id()).isEqualTo("minecraft:chest");
  }
}
