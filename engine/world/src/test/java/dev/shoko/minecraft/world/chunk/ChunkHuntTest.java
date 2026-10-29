package dev.shoko.minecraft.world.chunk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.core.nbt.binary.NbtCompression;
import dev.shoko.minecraft.core.nbt.binary.NbtReader;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.block.BlockProperty;
import dev.shoko.minecraft.world.block.BlockReportEntry;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.block.BlockStateEntryCodec;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.block.ReportedState;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.MiniJson;
import dev.shoko.minecraft.world.support.RefPacking;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Bug-hunter probes on chunk sections, palettes and coordinates, against the real data. */
class ChunkHuntTest {

  private static final ChunkPos POS = new ChunkPos(-3, 5);

  private static Path cache() {
    String configured = System.getenv("MINECRAFT_CACHE_DIR");
    Path base =
        configured != null && !configured.isBlank()
            ? Path.of(configured)
            : Path.of("").toAbsolutePath().resolve(".cache");
    return base.resolve("reference/26.3");
  }

  private static BlockStateRegistry realRegistry() throws Exception {
    Path file = cache().resolve("generated/reports/blocks.json");
    assumeTrue(Files.exists(file), "reference cache absent");
    Path registryFile = cache().resolve("generated/reports/registries.json");
    Map<String, Object> blocks =
        MiniJson.object(MiniJson.parse(Files.readString(file, StandardCharsets.UTF_8)));
    Map<String, Object> registry =
        MiniJson.object(
            MiniJson.object(
                    MiniJson.object(
                            MiniJson.parse(Files.readString(registryFile, StandardCharsets.UTF_8)))
                        .get("minecraft:block"))
                .get("entries"));
    BlockReportEntry[] ordered = new BlockReportEntry[blocks.size()];
    for (Map.Entry<String, Object> block : blocks.entrySet()) {
      Map<String, Object> body = MiniJson.object(block.getValue());
      int protocolId =
          ((Long) MiniJson.object(registry.get(block.getKey())).get("protocol_id")).intValue();
      List<BlockProperty> properties = new ArrayList<>();
      if (body.containsKey("properties")) {
        MiniJson.object(body.get("properties"))
            .forEach(
                (name, values) ->
                    properties.add(
                        new BlockProperty(
                            name,
                            MiniJson.array(values).stream().map(String.class::cast).toList())));
      }
      List<ReportedState> states = new ArrayList<>();
      for (Object state : MiniJson.array(body.get("states"))) {
        Map<String, Object> fields = MiniJson.object(state);
        Map<String, String> values = new LinkedHashMap<>();
        if (fields.containsKey("properties")) {
          MiniJson.object(fields.get("properties")).forEach((k, v) -> values.put(k, (String) v));
        }
        states.add(
            new ReportedState(
                ((Long) fields.get("id")).intValue(),
                values,
                Boolean.TRUE.equals(fields.get("default"))));
      }
      ordered[protocolId] = new BlockReportEntry(block.getKey(), properties, states);
    }
    return BlockStateRegistry.load(Blocks.sourceOf(List.of(ordered)));
  }

  private static ChunkCodec codec(BlockStateRegistry registry) {
    return ChunkCodec.of(registry, id -> id.startsWith("minecraft:"));
  }

  private static CompoundTag section(CompoundTag root, int y) {
    for (NbtTag tag : root.getList("sections").elements()) {
      if (tag instanceof CompoundTag compound && compound.getByte("Y") == y) {
        return compound;
      }
    }
    throw new AssertionError("no section " + y);
  }

  /** The state at a block read straight from the NBT tree with the packing of CF-R8. */
  private static String rawBlockId(CompoundTag root, BlockPos at) {
    CompoundTag container = section(root, at.y() >> 4).getCompound("block_states");
    List<NbtTag> palette = container.getList("palette").elements();
    int index = ((at.y() & 15) * 16 + (at.z() & 15)) * 16 + (at.x() & 15);
    int slot = 0;
    if (palette.size() > 1) {
      long[] data = container.getLongArray("data");
      slot = RefPacking.unpack(data, RefPacking.blockBits(palette.size()), 4096)[index];
    }
    return ((CompoundTag) palette.get(slot)).getString("id");
  }

  @Tag("oracle")
  @Test
  void blockPaletteSizesAtEveryThresholdUseTheSpecBitsAndLongs() throws Exception {
    BlockStateRegistry registry = realRegistry();
    ChunkCodec codec = codec(registry);
    int[] sizes = {
      2, 3, 15, 16, 17, 31, 32, 33, 63, 64, 65, 255, 256, 257, 511, 512, 513, 1000, 2047, 2048,
      2049, 4095, 4096
    };
    for (int n : sizes) {
      Chunk chunk = Chunks.empty(registry, POS, Chunks.OVERWORLD);
      List<BlockState> distinct = new ArrayList<>();
      for (int id = 1; distinct.size() < n; id += 7) {
        distinct.add(registry.state(id % registry.stateCount()));
      }
      assertThat(distinct.stream().distinct().count()).isEqualTo(n);
      for (int i = 0; i < 4096; i++) {
        // the last positions carry the highest palette indices
        int slot = i < 4096 - n ? i % n : 4096 - 1 - i;
        BlockPos at =
            new BlockPos(
                POS.minBlockX() + (i & 15), -64 + (i >> 8), POS.minBlockZ() + ((i >> 4) & 15));
        chunk.setBlockState(at, distinct.get(slot));
      }
      CompoundTag root = codec.encode(chunk);
      CompoundTag states = section(root, -4).getCompound("block_states");
      List<NbtTag> palette = states.getList("palette").elements();
      int distinctWritten = chunk.section(-4).blockStates().distinctValues().size();
      assertThat(palette).hasSize(distinctWritten);
      if (palette.size() > 1) {
        int bits = RefPacking.blockBits(palette.size());
        assertThat(states.getLongArray("data"))
            .as("palette of %d", palette.size())
            .hasSize(RefPacking.longCount(4096, bits));
      }
      for (int i = 0; i < 4096; i++) {
        BlockPos at =
            new BlockPos(
                POS.minBlockX() + (i & 15), -64 + (i >> 8), POS.minBlockZ() + ((i >> 4) & 15));
        assertThat(rawBlockId(root, at)).isEqualTo(chunk.blockState(at).block().id());
      }
      ReadResult<Chunk> back = codec.decode(root, Chunks.OVERWORLD);
      Chunk read = Chunks.present(back).value();
      Chunks.assertSameSection(chunk.section(-4), read.section(-4));
    }
  }

  @Test
  void biomePaletteSizesAtEveryThresholdUseTheSpecBitsAndLongs() throws Exception {
    BlockStateRegistry registry = Blocks.registry();
    ChunkCodec codec = ChunkCodec.of(registry, id -> id.startsWith("minecraft:b"));
    for (int n : new int[] {2, 3, 4, 5, 8, 9, 16, 17, 32, 33, 63, 64}) {
      Chunk chunk = Chunks.empty(registry, POS, Chunks.OVERWORLD);
      for (int cell = 0; cell < 64; cell++) {
        int qx = cell & 3;
        int qz = (cell >> 2) & 3;
        int qy = cell >> 4;
        BlockPos at =
            new BlockPos(POS.minBlockX() + qx * 4 + 3, 100 + qy * 4 + 1, POS.minBlockZ() + qz * 4);
        chunk.setBiome(at, "minecraft:b" + (cell < 64 - n ? cell % n : 63 - cell));
      }
      CompoundTag root = codec.encode(chunk);
      CompoundTag biomes = section(root, 6).getCompound("biomes");
      int size = biomes.getList("palette").elements().size();
      assertThat(size).isEqualTo(chunk.section(6).biomes().distinctValues().size());
      if (size > 1) {
        int bits = RefPacking.biomeBits(size);
        assertThat(biomes.getLongArray("data")).hasSize(RefPacking.longCount(64, bits));
        int[] slots = RefPacking.unpack(biomes.getLongArray("data"), bits, 64);
        for (int cell = 0; cell < 64; cell++) {
          StringTag name = (StringTag) biomes.getList("palette").elements().get(slots[cell]);
          assertThat(name.value()).isEqualTo(chunk.section(6).biomes().get(cell));
        }
      }
      Chunk read = Chunks.present(codec.decode(root, Chunks.OVERWORLD)).value();
      Chunks.assertSameSection(chunk.section(6), read.section(6));
    }
  }

  @Test
  void blocksAtTheNegativeAndVerticalEdgesLandAtTheSpecIndex() throws Exception {
    BlockStateRegistry registry = Blocks.registry();
    ChunkCodec codec = Chunks.codec();
    for (ChunkPos pos :
        List.of(
            new ChunkPos(-1, -1),
            new ChunkPos(-2, 0),
            new ChunkPos(0, -2),
            new ChunkPos(-134217728, 134217727))) {
      Chunk chunk = Chunks.empty(registry, pos, Chunks.OVERWORLD);
      Map<BlockPos, String> expected = new HashMap<>();
      String[] ids = {Blocks.STONE, Blocks.BEDROCK, Blocks.OAK_LOG, Blocks.GRASS_BLOCK};
      int k = 0;
      for (int y : new int[] {-64, -49, -48, -1, 0, 15, 16, 319}) {
        for (int dx : new int[] {0, 1, 15}) {
          for (int dz : new int[] {0, 15}) {
            BlockPos at = new BlockPos(pos.minBlockX() + dx, y, pos.minBlockZ() + dz);
            String id = ids[k++ % ids.length];
            chunk.setBlockState(at, registry.defaultState(registry.block(id).orElseThrow()));
            expected.put(at, id);
          }
        }
      }
      CompoundTag root = codec.encode(chunk);
      expected.forEach(
          (at, id) -> assertThat(rawBlockId(root, at)).as(at.toString()).isEqualTo(id));
      Chunk read = Chunks.present(codec.decode(root, Chunks.OVERWORLD)).value();
      expected.forEach((at, id) -> assertThat(read.blockState(at).block().id()).isEqualTo(id));
    }
  }

  @Tag("oracle")
  @Test
  void everyStructureTemplateRoundTripsThroughAChunkAndItsNbt() throws Exception {
    BlockStateRegistry registry = realRegistry();
    ChunkCodec codec = codec(registry);
    Path templates = cache().resolve("resources/data/minecraft/structure");
    assumeTrue(Files.isDirectory(templates), "templates absent");
    List<Path> files;
    try (Stream<Path> walk = Files.walk(templates)) {
      files = walk.filter(p -> p.toString().endsWith(".nbt")).sorted().toList();
    }
    BlockStateEntryCodec entries = BlockStateEntryCodec.of(registry, (code, message) -> {});
    int placed = 0;
    for (Path file : files) {
      CompoundTag root =
          (CompoundTag)
              NbtReader.of(NbtCompression.GZIP)
                  .readNamed(Files.readAllBytes(file))
                  .map(NamedTag::tag)
                  .orElseThrow();
      List<NbtTag> palette =
          root.entries().containsKey("palette")
              ? root.getList("palette").elements()
              : ((ListTag) root.getList("palettes").elements().getFirst()).elements();
      Chunk chunk = Chunks.empty(registry, POS, Chunks.OVERWORLD);
      Map<BlockPos, BlockState> expected = new HashMap<>();
      for (NbtTag tag : root.getList("blocks").elements()) {
        CompoundTag block = (CompoundTag) tag;
        List<NbtTag> coordinates = block.getList("pos").elements();
        int x = ((IntTag) coordinates.get(0)).value();
        int y = ((IntTag) coordinates.get(1)).value();
        int z = ((IntTag) coordinates.get(2)).value();
        if (x > 15 || z > 15 || y > 380) {
          continue;
        }
        BlockState state = entries.decode(palette.get(block.getInt("state")));
        BlockPos at = new BlockPos(POS.minBlockX() + x, -64 + y, POS.minBlockZ() + z);
        chunk.setBlockState(at, state);
        expected.put(at, state);
        placed++;
      }
      CompoundTag written = codec.encode(chunk);
      Chunk read = Chunks.present(codec.decode(written, Chunks.OVERWORLD)).value();
      for (Map.Entry<BlockPos, BlockState> entry : expected.entrySet()) {
        assertThat(read.blockState(entry.getKey()))
            .as(file.getFileName() + " " + entry.getKey())
            .isEqualTo(entry.getValue());
        assertThat(rawBlockId(written, entry.getKey())).isEqualTo(entry.getValue().block().id());
      }
      CompoundTag again = codec.encode(read);
      assertThat(again).as("write-read-write of " + file.getFileName()).isEqualTo(written);
    }
    assertThat(placed).isGreaterThan(100_000);
    assertThat(files).hasSize(1511);
  }
}
