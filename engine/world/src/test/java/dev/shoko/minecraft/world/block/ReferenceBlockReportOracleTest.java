package dev.shoko.minecraft.world.block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.binary.NbtCompression;
import dev.shoko.minecraft.core.nbt.binary.NbtReader;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.MiniJson;
import dev.shoko.minecraft.world.support.Warnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Criterion of {@code world-block-states.md} on the extracted data of the reference game (R3, R5 to
 * R7, R10, R11, R16, R17; vectors BS1 to BS3): the block report is read from the local cache
 * ({@code MINECRAFT_CACHE_DIR}, else {@code .cache} at the repository root) and never copied; every
 * test is skipped when the cache is absent.
 */
@Tag("oracle")
class ReferenceBlockReportOracleTest {

  private static Path cache() {
    String configured = System.getenv("MINECRAFT_CACHE_DIR");
    if (configured != null && !configured.isBlank()) {
      return Path.of(configured).resolve("reference/26.3");
    }
    Path dir = Path.of("").toAbsolutePath();
    while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
      dir = dir.getParent();
    }
    return (dir == null ? Path.of("") : dir).resolve(".cache/reference/26.3");
  }

  private static Path require(String relative) {
    Path file = cache().resolve(relative);
    assumeTrue(Files.exists(file), () -> "reference cache absent: " + file);
    return file;
  }

  private static Object json(String relative) throws IOException {
    return MiniJson.parse(Files.readString(require(relative), StandardCharsets.UTF_8));
  }

  /** The report joined with the registry order, as the port of the module wants it. */
  private static BlockReportSource source() throws IOException {
    Map<String, Object> blocks = MiniJson.object(json("generated/reports/blocks.json"));
    Map<String, Object> registry =
        MiniJson.object(
            MiniJson.object(
                    MiniJson.object(json("generated/reports/registries.json"))
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
    return Blocks.sourceOf(List.of(ordered));
  }

  private static BlockStateRegistry load() throws IOException, BlockReportException {
    return BlockStateRegistry.load(source());
  }

  @Test
  void reportHas1286BlocksAndTheStatesOfVectorBs1() throws IOException, BlockReportException {
    // spec: BS-R3 (1286 blocks, 35 723 states, ids 0 to 35 722), BS1
    BlockStateRegistry registry = load();

    assertThat(registry.blockCount()).isEqualTo(1286);
    assertThat(registry.stateCount()).isEqualTo(35_723);
    assertThat(registry.block("minecraft:note_block").orElseThrow().stateCount()).isEqualTo(1350);
  }

  @Test
  void everyReportedIdentifierIsRecomputedFromThePropertyLists()
      throws IOException, BlockReportException {
    // spec: BS-R5 to BS-R7 (1286 blocks of 1286 and 35 723 states of 35 723 recomputed)
    BlockReportSource source = source();
    BlockStateRegistry registry = BlockStateRegistry.load(source);

    for (BlockReportEntry entry : source.blocks()) {
      BlockType block = registry.block(entry.id()).orElseThrow();
      for (ReportedState state : entry.states()) {
        assertThat(registry.state(block, state.properties()).id())
            .as(entry.id())
            .isEqualTo(state.id());
        assertThat(block.stateId(state.properties())).isEqualTo(state.id());
        if (state.isDefault()) {
          assertThat(block.defaultStateId()).as(entry.id()).isEqualTo(state.id());
        }
      }
    }
  }

  @Test
  void identifiersToKnowAndTheTwelveBlocksOfRuleR8() throws IOException, BlockReportException {
    // spec: BS-R8, BS-R10, BS-R11, BS2 (air 0, stone 1, bedrock 88, water 89, chest 5545, grass
    // default 9)
    BlockStateRegistry registry = load();

    assertThat(registry.block("minecraft:air").orElseThrow().firstStateId()).isZero();
    assertThat(registry.block("minecraft:stone").orElseThrow().firstStateId()).isEqualTo(1);
    assertThat(registry.block("minecraft:bedrock").orElseThrow().firstStateId()).isEqualTo(88);
    assertThat(registry.block("minecraft:water").orElseThrow().firstStateId()).isEqualTo(89);
    assertThat(registry.block("minecraft:lava").orElseThrow().firstStateId()).isEqualTo(105);
    assertThat(registry.block("minecraft:void_air").orElseThrow().firstStateId()).isEqualTo(18_649);
    assertThat(registry.block("minecraft:cave_air").orElseThrow().firstStateId()).isEqualTo(18_650);
    assertThat(registry.block("minecraft:grass_block").orElseThrow().defaultStateId()).isEqualTo(9);
    assertThat(registry.block("minecraft:oak_log").orElseThrow().defaultStateId()).isEqualTo(140);
    assertThat(registry.block("minecraft:redstone_wire").orElseThrow().defaultStateId())
        .isEqualTo(6727);
    assertThat(registry.block("minecraft:chest").orElseThrow().defaultStateId()).isEqualTo(5544);
    BlockType chest = registry.block("minecraft:chest").orElseThrow();
    assertThat(chest.stateId(Map.of("facing", "north", "type", "left", "waterlogged", "true")))
        .isEqualTo(5545);
    assertThat(registry.state(35_722).block().id()).isEqualTo("minecraft:firefly_bush");
  }

  @Test
  void everyPaletteEntryOfTheStructureTemplatesDecodesWithoutWarningAndWritesBack()
      throws IOException, BlockReportException, NbtException {
    // spec: BS-R16, BS-R17, BS3 (1511 files, 1651 palettes, 6472 + 17 770 entries)
    BlockStateRegistry registry = load();
    Path structures = require("resources/data/minecraft/structure");
    List<Path> files;
    try (Stream<Path> walk = Files.walk(structures)) {
      files = walk.filter(p -> p.toString().endsWith(".nbt")).sorted().toList();
    }
    int palettes = 0;
    int plain = 0;
    int withProperties = 0;
    for (Path file : files) {
      NamedTag root =
          NbtReader.of(NbtCompression.GZIP).readNamed(Files.readAllBytes(file)).orElseThrow();
      CompoundTag compound = (CompoundTag) root.tag();
      List<ListTag> lists = new ArrayList<>();
      if (compound.get("palette").isPresent()) {
        lists.add(compound.getList("palette"));
      }
      compound.getList("palettes").elements().forEach(tag -> lists.add((ListTag) tag));
      for (ListTag palette : lists) {
        palettes++;
        for (NbtTag entry : palette.elements()) {
          CompoundTag wanted = (CompoundTag) entry;
          Warnings warnings = new Warnings();
          BlockStateEntryCodec codec = BlockStateEntryCodec.of(registry, warnings);

          BlockState state = codec.decode(wanted);

          assertThat(warnings.isEmpty()).as(file.toString()).isTrue();
          assertThat(codec.encode(state)).as(file.toString()).isEqualTo(wanted);
          if (wanted.get("properties").isPresent()) {
            withProperties++;
          } else {
            plain++;
          }
        }
      }
    }
    assertThat(files).hasSize(1511);
    assertThat(palettes).isEqualTo(1651);
    assertThat(plain).isEqualTo(6472);
    assertThat(withProperties).isEqualTo(17_770);
  }
}
