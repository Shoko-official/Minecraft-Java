package dev.shoko.minecraft.world.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.heightmap.Heightmaps;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CF-R2 to CF-R5, CF-R20, CF-R22: the chunk model and its coordinates. */
class ChunkTest {

  private static final ChunkPos NEGATIVE = new ChunkPos(-1, -1);
  private final BlockStateRegistry registry = Blocks.registry();
  private final BlockState bedrock = registry.state(4);

  private Chunk overworld(ChunkPos pos) {
    return Chunks.empty(registry, pos, Chunks.OVERWORLD);
  }

  private List<ChunkSection> sections(int fromY, int toYInclusive) {
    List<ChunkSection> sections = new ArrayList<>();
    for (int y = fromY; y <= toYInclusive; y++) {
      sections.add(Chunks.airSection(registry, y));
    }
    return sections;
  }

  private Chunk of(WorldHeight height, List<ChunkSection> sections) {
    ChunkHeader header = new ChunkHeader(NEGATIVE, height, ChunkStatus.FULL, 0L, 0L);
    return Chunk.of(
        header, sections, Heightmaps.empty(), ChunkAttachments.empty(height.sectionCount()));
  }

  @Test
  void chunkExposesItsHeader() {
    // spec: CF-R20
    ChunkHeader header =
        new ChunkHeader(new ChunkPos(3, -7), Chunks.OVERWORLD, ChunkStatus.FEATURES, 99L, 1234L);

    Chunk chunk =
        Chunk.of(header, sections(-4, 19), Heightmaps.empty(), ChunkAttachments.empty(24));

    assertThat(chunk.pos()).isEqualTo(new ChunkPos(3, -7));
    assertThat(chunk.height()).isEqualTo(Chunks.OVERWORLD);
    assertThat(chunk.status()).isEqualTo(ChunkStatus.FEATURES);
    assertThat(chunk.lastUpdate()).isEqualTo(99L);
    assertThat(chunk.inhabitedTime()).isEqualTo(1234L);
  }

  @Test
  void headerFieldsCanBeChanged() {
    // spec: CF-R20 (the caller supplies the ticks, nothing reads a clock)
    Chunk chunk = overworld(NEGATIVE);

    chunk.setStatus(ChunkStatus.LIGHT);
    chunk.setLastUpdate(Long.MAX_VALUE);
    chunk.setInhabitedTime(-5L);

    assertThat(chunk.status()).isEqualTo(ChunkStatus.LIGHT);
    assertThat(chunk.lastUpdate()).isEqualTo(Long.MAX_VALUE);
    assertThat(chunk.inhabitedTime()).isEqualTo(-5L);
  }

  @Test
  void sectionsCoverTheHeightFromBottomToTop() {
    // spec: CF-R2, CF-R3 (24 sections, y -4 to 19, rank = sectionY - (minY >> 4))
    Chunk chunk = overworld(NEGATIVE);

    assertThat(chunk.sections()).hasSize(24);
    for (int rank = 0; rank < 24; rank++) {
      assertThat(chunk.sections().get(rank).y()).isEqualTo(rank - 4);
      assertThat(chunk.section(rank - 4)).isSameAs(chunk.sections().get(rank));
    }
  }

  @Test
  void netherAndEndHaveSixteenSections() {
    // spec: CF-R1, CF-R2 (min_y 0, height 256: section y 0 to 15)
    Chunk chunk = Chunks.empty(registry, new ChunkPos(0, 0), Chunks.NETHER);

    assertThat(chunk.sections()).hasSize(16);
    assertThat(chunk.sections().getFirst().y()).isZero();
    assertThat(chunk.sections().getLast().y()).isEqualTo(15);
  }

  @Test
  void sectionOutsideTheHeightIsRejected() {
    // spec: Chunk.section contract (IllegalArgumentException)
    Chunk chunk = overworld(NEGATIVE);

    Failures.expect(IllegalArgumentException.class, () -> chunk.section(-5));
    Failures.expect(IllegalArgumentException.class, () -> chunk.section(20));
    Failures.expect(IllegalArgumentException.class, () -> chunk.section(Integer.MIN_VALUE));
  }

  @Test
  void sectionListCannotBeModified() {
    // spec: Chunk.sections contract (unmodifiable)
    List<ChunkSection> sections = overworld(NEGATIVE).sections();

    Failures.expect(UnsupportedOperationException.class, () -> sections.removeLast());
  }

  @Test
  void sectionsThatDoNotCoverTheHeightExactlyInOrderAreRejected() {
    // spec: Chunk.of contract (IllegalArgumentException), CF-R22
    List<ChunkSection> missing = sections(-4, 18);
    List<ChunkSection> extra = sections(-4, 20);
    List<ChunkSection> lowShifted = sections(-5, 18);
    List<ChunkSection> swapped = sections(-4, 19);
    ChunkSection first = swapped.get(0);
    swapped.set(0, swapped.get(1));
    swapped.set(1, first);
    List<ChunkSection> duplicate = sections(-4, 18);
    duplicate.add(Chunks.airSection(registry, 18));

    for (List<ChunkSection> bad :
        List.of(missing, extra, lowShifted, swapped, duplicate, List.<ChunkSection>of())) {
      Failures.expect(IllegalArgumentException.class, () -> of(Chunks.OVERWORLD, bad));
    }
  }

  @Test
  void blockAtTheLowCornerOfANegativeChunkIsInSectionMinusFour() {
    // spec: CF-R3, CF-R4, CO1 (block (-1, -64, -1): chunk (-1, -1), local (15, 0, 15), index 255)
    Chunk chunk = overworld(NEGATIVE);

    BlockState previous = chunk.setBlockState(new BlockPos(-1, -64, -1), bedrock);

    assertThat(previous).isEqualTo(registry.air());
    assertThat(chunk.blockState(new BlockPos(-1, -64, -1))).isEqualTo(bedrock);
    assertThat(chunk.section(-4).blockStates().get(255)).isEqualTo(bedrock);
    assertThat(chunk.section(-4).blockStates().get(0)).isEqualTo(registry.air());
  }

  @Test
  void blockAtTheTopOfTheWorldIsInSectionNineteen() {
    // spec: CF-R3, CF-R4 (y 319: section 19, local 15; block (-16, 319, -16): local (0, 15, 0))
    Chunk chunk = overworld(NEGATIVE);

    chunk.setBlockState(new BlockPos(-16, 319, -16), bedrock);

    assertThat(chunk.section(19).blockStates().get(3840)).isEqualTo(bedrock);
    assertThat(chunk.blockState(new BlockPos(-16, 319, -16))).isEqualTo(bedrock);
  }

  @Test
  void blockIndexVariesXFirstThenZThenY() {
    // spec: CF-R4 ((ly * 16 + lz) * 16 + lx), here in chunk (2, -3) section 6
    Chunk chunk = overworld(new ChunkPos(2, -3));

    chunk.setBlockState(new BlockPos(47, 100, -33), bedrock);
    chunk.setBlockState(new BlockPos(33, 97, -47), registry.state(1));

    assertThat(chunk.section(6).blockStates().get((4 * 16 + 15) * 16 + 15)).isEqualTo(bedrock);
    assertThat(chunk.section(6).blockStates().get((1 * 16 + 1) * 16 + 1))
        .isEqualTo(registry.state(1));
  }

  @Test
  void positionsOutsideTheChunkOrTheHeightAreRejected() {
    // spec: Chunk contract (IllegalArgumentException), CF-R3 (y 320 and y -65 are outside)
    Chunk chunk = overworld(NEGATIVE);

    Failures.expect(IllegalArgumentException.class, () -> chunk.blockState(new BlockPos(0, 0, 0)));
    Failures.expect(
        IllegalArgumentException.class, () -> chunk.blockState(new BlockPos(-17, 0, -1)));
    Failures.expect(
        IllegalArgumentException.class, () -> chunk.blockState(new BlockPos(-1, 320, -1)));
    Failures.expect(
        IllegalArgumentException.class, () -> chunk.blockState(new BlockPos(-1, -65, -1)));
    Failures.expect(
        IllegalArgumentException.class, () -> chunk.setBlockState(new BlockPos(0, 0, -1), bedrock));
    Failures.expect(IllegalArgumentException.class, () -> chunk.biome(new BlockPos(-1, 320, -1)));
    Failures.expect(
        IllegalArgumentException.class,
        () -> chunk.setBiome(new BlockPos(-1, -65, -1), Chunks.DESERT));
  }

  @Test
  void biomeCellCoversFourByFourByFourBlocks() {
    // spec: CF-R5 (a cell holds 4 x 4 x 4 blocks)
    Chunk chunk = overworld(NEGATIVE);

    String previous = chunk.setBiome(new BlockPos(-1, -64, -1), Chunks.DESERT);

    assertThat(previous).isEqualTo(Chunks.PLAINS);
    assertThat(chunk.biome(new BlockPos(-4, -61, -4))).isEqualTo(Chunks.DESERT);
    assertThat(chunk.biome(new BlockPos(-5, -64, -1))).isEqualTo(Chunks.PLAINS);
    assertThat(chunk.biome(new BlockPos(-1, -60, -1))).isEqualTo(Chunks.PLAINS);
    assertThat(chunk.section(-4).biomes().get(15)).isEqualTo(Chunks.DESERT);
  }

  @Test
  void netherHeightBoundsAreEnforced() {
    // spec: CF-R1, CF-R3 (y 0 to 255)
    Chunk chunk = Chunks.empty(registry, new ChunkPos(0, 0), Chunks.NETHER);

    chunk.setBlockState(new BlockPos(0, 255, 0), bedrock);

    assertThat(chunk.section(15).blockStates().get(15 * 256)).isEqualTo(bedrock);
    Failures.expect(
        IllegalArgumentException.class, () -> chunk.blockState(new BlockPos(0, 256, 0)));
    Failures.expect(IllegalArgumentException.class, () -> chunk.blockState(new BlockPos(0, -1, 0)));
    Failures.expect(IllegalArgumentException.class, () -> chunk.section(16));
    Failures.expect(IllegalArgumentException.class, () -> chunk.section(-1));
  }

  @Test
  void attachmentsCanBeReplaced() {
    // spec: Chunk.setAttachments contract
    Chunk chunk = overworld(NEGATIVE);
    ChunkAttachments replacement = ChunkAttachments.empty(24);

    chunk.setAttachments(replacement);

    assertThat(chunk.attachments()).isSameAs(replacement);
  }
}
