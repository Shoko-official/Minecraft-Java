package dev.shoko.minecraft.world.support;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.coord.WorldHeight;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.biome.BiomeCatalog;
import dev.shoko.minecraft.world.block.BlockState;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.chunk.ChunkAttachments;
import dev.shoko.minecraft.world.chunk.ChunkCodec;
import dev.shoko.minecraft.world.chunk.ChunkHeader;
import dev.shoko.minecraft.world.chunk.ChunkSection;
import dev.shoko.minecraft.world.chunk.ChunkStatus;
import dev.shoko.minecraft.world.chunk.LightOnlySection;
import dev.shoko.minecraft.world.chunk.NibbleArray;
import dev.shoko.minecraft.world.chunk.SectionLight;
import dev.shoko.minecraft.world.heightmap.Heightmap;
import dev.shoko.minecraft.world.heightmap.HeightmapType;
import dev.shoko.minecraft.world.heightmap.Heightmaps;
import dev.shoko.minecraft.world.palette.PalettedContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;

/** Fixtures and comparisons for chunks. */
public final class Chunks {

  public static final WorldHeight OVERWORLD = new WorldHeight(-64, 384);
  public static final WorldHeight NETHER = new WorldHeight(0, 256);
  public static final String PLAINS = "minecraft:plains";
  public static final String DESERT = "minecraft:desert";
  private static final Set<String> BIOMES = Set.of(PLAINS, DESERT, "minecraft:ocean");

  private Chunks() {}

  public static BiomeCatalog biomes() {
    return BIOMES::contains;
  }

  public static ChunkCodec codec() {
    return ChunkCodec.of(Blocks.registry(), biomes());
  }

  /** Decodes a chunk root of the Overworld with the made-up registry and biome catalog. */
  public static ReadResult<Chunk> decode(CompoundTag root) {
    return codec().decode(root, OVERWORLD);
  }

  /** Decodes a chunk root of the Overworld that must be present, without warning. */
  public static Chunk decodeClean(CompoundTag root) {
    ReadResult.Present<Chunk> present = present(decode(root));
    assertThat(present.warnings()).isEmpty();
    return present.value();
  }

  public static ChunkSection airSection(BlockStateRegistry registry, int y) {
    return ChunkSection.uniform(y, registry.air(), PLAINS);
  }

  /** A full-status chunk of air and plains, with every heightmap unknown. */
  public static Chunk empty(BlockStateRegistry registry, ChunkPos pos, WorldHeight height) {
    List<ChunkSection> sections = new ArrayList<>();
    for (int y = height.minSectionY(); y <= height.maxSectionY(); y++) {
      sections.add(airSection(registry, y));
    }
    ChunkHeader header = new ChunkHeader(pos, height, ChunkStatus.FULL, 0L, 0L);
    return Chunk.of(
        header, sections, Heightmaps.empty(), ChunkAttachments.empty(height.sectionCount()));
  }

  /** Attachments with nothing but the given light-only sections, bottom to top by insertion. */
  public static ChunkAttachments attachmentsWithLight(
      int sectionCount, SequencedMap<Integer, LightOnlySection> lightOnly) {
    ChunkAttachments empty = ChunkAttachments.empty(sectionCount);
    return new ChunkAttachments(
        empty.blockEntities(),
        empty.blockTicks(),
        empty.fluidTicks(),
        empty.postProcessing(),
        empty.structures(),
        empty.entities(),
        lightOnly);
  }

  /** A chunk of air sections with the given attachments. */
  public static Chunk withAttachments(
      BlockStateRegistry registry, ChunkPos pos, WorldHeight height, ChunkAttachments attachments) {
    Chunk plain = empty(registry, pos, height);
    return Chunk.of(
        new ChunkHeader(pos, height, ChunkStatus.FULL, 0L, 0L),
        plain.sections(),
        Heightmaps.empty(),
        attachments);
  }

  public static NibbleArray nibbles(java.util.function.IntUnaryOperator values) {
    NibbleArray array = NibbleArray.zeros();
    for (int i = 0; i < 4096; i++) {
      array.set(i, values.applyAsInt(i));
    }
    return array;
  }

  public static PalettedContainer<BlockState> filledBlocks(BlockState state) {
    return PalettedContainer.filled(4096, state);
  }

  public static PalettedContainer<String> filledBiomes(String biome) {
    return PalettedContainer.filled(64, biome);
  }

  public static SectionLight lightWithSky(NibbleArray sky) {
    SectionLight light = SectionLight.none();
    light.setSkyLight(sky);
    return light;
  }

  /** Unwraps a present result, failing the test with the issue otherwise. */
  public static <T> ReadResult.Present<T> present(ReadResult<T> result) {
    if (result instanceof ReadResult.Present<T> present) {
      return present;
    }
    throw new AssertionError("expected a present chunk but was " + result);
  }

  /** Unwraps an absent result, failing the test with the issue otherwise. */
  public static <T> ReadResult.Absent<T> absent(ReadResult<T> result) {
    if (result instanceof ReadResult.Absent<T> absent) {
      return absent;
    }
    throw new AssertionError("expected an absent chunk but was " + result);
  }

  /** Unwraps a refused result, failing the test with the issue otherwise. */
  public static <T> ReadResult.Refused<T> refused(ReadResult<T> result) {
    if (result instanceof ReadResult.Refused<T> refused) {
      return refused;
    }
    throw new AssertionError("expected a refused chunk but was " + result);
  }

  /** Asserts that every observable part of two chunks is equal. */
  public static void assertSameChunk(Chunk expected, Chunk actual) {
    assertThat(actual.pos()).isEqualTo(expected.pos());
    assertThat(actual.height()).isEqualTo(expected.height());
    assertThat(actual.status()).isEqualTo(expected.status());
    assertThat(actual.lastUpdate()).isEqualTo(expected.lastUpdate());
    assertThat(actual.inhabitedTime()).isEqualTo(expected.inhabitedTime());
    assertThat(actual.sections()).hasSameSizeAs(expected.sections());
    for (int k = 0; k < expected.sections().size(); k++) {
      assertSameSection(expected.sections().get(k), actual.sections().get(k));
    }
    assertThat(actual.heightmaps().unknownTypes()).isEqualTo(expected.heightmaps().unknownTypes());
    for (HeightmapType type : HeightmapType.values()) {
      Optional<Heightmap> want = expected.heightmaps().get(type);
      assertThat(actual.heightmaps().get(type).isPresent()).isEqualTo(want.isPresent());
      want.ifPresent(
          h ->
              assertThat(actual.heightmaps().get(type).orElseThrow().toLongs())
                  .isEqualTo(h.toLongs()));
    }
    assertSameAttachments(expected.attachments(), actual.attachments());
  }

  public static void assertSameAttachments(ChunkAttachments expected, ChunkAttachments actual) {
    assertThat(actual.blockEntities()).isEqualTo(expected.blockEntities());
    assertThat(actual.blockTicks()).isEqualTo(expected.blockTicks());
    assertThat(actual.fluidTicks()).isEqualTo(expected.fluidTicks());
    assertThat(actual.postProcessing()).isEqualTo(expected.postProcessing());
    assertThat(actual.structures()).isEqualTo(expected.structures());
    assertThat(actual.entities()).isEqualTo(expected.entities());
    assertThat(List.copyOf(actual.lightOnlySections().keySet()))
        .isEqualTo(List.copyOf(expected.lightOnlySections().keySet()));
    expected
        .lightOnlySections()
        .forEach(
            (y, section) -> {
              LightOnlySection other = actual.lightOnlySections().get(y);
              assertThat(other.otherFields()).isEqualTo(section.otherFields());
              assertSameLight(section.light().blockLight(), other.light().blockLight());
              assertSameLight(section.light().skyLight(), other.light().skyLight());
            });
  }

  public static void assertSameSection(ChunkSection expected, ChunkSection actual) {
    assertThat(actual.y()).isEqualTo(expected.y());
    for (int i = 0; i < 4096; i++) {
      assertThat(actual.blockStates().get(i))
          .as("block " + i)
          .isEqualTo(expected.blockStates().get(i));
    }
    for (int i = 0; i < 64; i++) {
      assertThat(actual.biomes().get(i)).as("biome " + i).isEqualTo(expected.biomes().get(i));
    }
    assertSameLight(expected.light().blockLight(), actual.light().blockLight());
    assertSameLight(expected.light().skyLight(), actual.light().skyLight());
  }

  private static void assertSameLight(
      Optional<NibbleArray> expected, Optional<NibbleArray> actual) {
    assertThat(actual.isPresent()).isEqualTo(expected.isPresent());
    expected.ifPresent(e -> assertThat(actual.get().toBytes()).isEqualTo(e.toBytes()));
  }
}
