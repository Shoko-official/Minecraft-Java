package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_A;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NamedTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.binary.NbtCompression;
import dev.shoko.minecraft.core.nbt.binary.NbtReader;
import dev.shoko.minecraft.core.nbt.binary.NbtWriter;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.RegionFixtures;
import dev.shoko.minecraft.world.support.Regions;
import dev.shoko.minecraft.world.support.RichChunk;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** AR-R9, AR-R24 and "Ecriture": saving chunks through the store. */
class AnvilChunkStoreSaveTest {

  @TempDir Path base;

  private final BlockStateRegistry registry = Blocks.registry();

  private Path regionFolder() {
    return base.resolve("dimensions/minecraft/overworld/region");
  }

  private AnvilChunkStore open(RegionCompression compression) {
    return AnvilChunkStore.open(regionFolder(), Chunks.OVERWORLD, Chunks.codec(), compression);
  }

  private Chunk load(ChunkPos pos) throws IOException {
    try (AnvilChunkStore store = open(RegionCompression.ZLIB)) {
      ReadResult<Chunk> result = store.load(pos);
      return Chunks.present(result).value();
    }
  }

  @Test
  void savedChunkIsLoadedBackIdentical() throws IOException {
    // spec: AR-R19 to AR-R24, CF-R20 to CF-R26 (the region folder is created by the first save)
    Chunk original = RichChunk.build(registry);

    try (AnvilChunkStore store = open(RegionCompression.ZLIB)) {
      store.save(original, 1_790_000_000L);
    }

    assertThat(Files.isDirectory(regionFolder())).isTrue();
    assertThat(RegionFixtures.names(regionFolder())).containsExactly("r.-1.0.mca");
    Chunks.assertSameChunk(original, load(RichChunk.POS));
  }

  @ParameterizedTest
  @EnumSource(RegionCompression.class)
  void recordCarriesTheConfiguredCompressionAndTheGivenTimestamp(RegionCompression compression)
      throws IOException {
    // spec: AR-R9, AR-R13, AR-R14 (the type used for writing is configurable)
    Chunk original = RichChunk.build(registry);

    try (AnvilChunkStore store = open(compression)) {
      store.save(original, 4_294_967_295L);
    }

    byte[] file = RegionFixtures.read(regionFolder().resolve("r.-1.0.mca"));
    int index = RichChunk.POS.indexInRegion();
    assertThat(index).isEqualTo(29 + 32 * 7);
    assertThat(Regions.typeOf(file, index)).isEqualTo(compression.id());
    assertThat(Regions.timestamp(file, index)).isEqualTo(4_294_967_295L);
    Chunks.assertSameChunk(original, load(RichChunk.POS));
  }

  @Test
  void writtenNbtIsOneNamedRootWithAnEmptyNameAndNothingAfterIt() throws IOException, NbtException {
    // spec: AR-R24 (a single Compound root with an empty name, no byte after the root)
    try (AnvilChunkStore store = open(RegionCompression.NONE)) {
      store.save(RichChunk.build(registry), 1L);
    }

    byte[] file = RegionFixtures.read(regionFolder().resolve("r.-1.0.mca"));
    byte[] nbt = Regions.payloadOf(file, RichChunk.POS.indexInRegion());
    NamedTag root = NbtReader.of(NbtCompression.NONE).readNamed(nbt).orElseThrow();
    assertThat(root.name()).isEmpty();
    assertThat(root.tag()).isInstanceOf(CompoundTag.class);
    byte[] again = NbtWriter.of(NbtCompression.NONE).writeNamed(root);
    assertThat(again).hasSize(nbt.length);
    assertThat(nbt[0]).isEqualTo((byte) 0x0a);
    assertThat(nbt[1]).isZero();
    assertThat(nbt[2]).isZero();
  }

  @Test
  void savingTwiceKeepsTheLatestState() throws IOException {
    // spec: "Ecriture" (replacing what is stored at the position)
    Chunk chunk = Chunks.empty(registry, CHUNK_A, Chunks.OVERWORLD);
    try (AnvilChunkStore store = open(RegionCompression.ZLIB)) {
      store.save(chunk, 1L);
      chunk.setBlockState(new BlockPos(-1, -64, -1), registry.state(4));
      chunk.setLastUpdate(99L);
      store.save(chunk, 2L);
    }

    Chunk back = load(CHUNK_A);

    assertThat(back.blockState(new BlockPos(-1, -64, -1)).id()).isEqualTo(4);
    assertThat(back.lastUpdate()).isEqualTo(99L);
    byte[] file = RegionFixtures.read(regionFolder().resolve("r.-1.-1.mca"));
    assertThat(Regions.timestamp(file, 1023)).isEqualTo(2L);
  }

  @Test
  void chunksOfDifferentRegionsGoToDifferentFiles() throws IOException {
    // spec: AR-R4
    Chunk a = Chunks.empty(registry, new ChunkPos(0, 0), Chunks.OVERWORLD);
    Chunk b = Chunks.empty(registry, new ChunkPos(-1, -1), Chunks.OVERWORLD);
    Chunk c = Chunks.empty(registry, new ChunkPos(32, -33), Chunks.OVERWORLD);

    try (AnvilChunkStore store = open(RegionCompression.ZLIB)) {
      store.save(a, 1L);
      store.save(b, 2L);
      store.save(c, 3L);
    }

    assertThat(RegionFixtures.names(regionFolder()))
        .containsExactly("r.-1.-1.mca", "r.0.0.mca", "r.1.-2.mca");
    assertThat(load(new ChunkPos(32, -33)).pos()).isEqualTo(new ChunkPos(32, -33));
  }

  @Test
  void readThenSavedWithoutModificationGivesTheSameChunk() throws IOException {
    // spec: "Cas limites" of chunk-format (read then rewritten without change: same sections and
    // lights)
    for (String variant :
        new String[] {
          "light_sections",
          "skylight_chain",
          "postprocessing",
          "missing_section",
          "unsorted_sections"
        }) {
      Path folder = base.resolve("v-" + variant);
      RegionFixtures.writeVariant(folder, variant);
      try (AnvilChunkStore store =
          AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB)) {
        Chunk first = Chunks.present(store.load(CHUNK_A)).value();
        store.save(first, 5L);
        Chunk second = Chunks.present(store.load(CHUNK_A)).value();
        Chunks.assertSameChunk(first, second);
      }
    }
  }

  @Test
  void aSavedChunkLoadsWithoutWarningEvenIfTheOriginalNeededCorrections() throws IOException {
    // spec: BS-R21, CF-R32 f (the corrected value is written; the rewrite is complete)
    Path folder = base.resolve("corrections");
    RegionFixtures.writeVariant(folder, "bad_value");
    try (AnvilChunkStore store =
        AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB)) {
      Chunk first = Chunks.present(store.load(CHUNK_A)).value();
      store.save(first, 5L);
    }

    try (AnvilChunkStore store =
        AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB)) {
      assertThat(Chunks.present(store.load(CHUNK_A)).warnings()).isEmpty();
    }
  }

  @Test
  void sameSavesInTwoFoldersGiveTheSameFiles() throws IOException {
    // spec: determinism (same input, same output, byte for byte)
    Path other = base.resolve("other");
    for (Path folder : new Path[] {base.resolve("one"), other}) {
      try (AnvilChunkStore store =
          AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.LZ4)) {
        store.save(RichChunk.build(registry), 10L);
        store.save(Chunks.empty(registry, CHUNK_A, Chunks.OVERWORLD), 11L);
      }
    }

    for (String name : new String[] {"r.-1.0.mca", "r.-1.-1.mca"}) {
      assertThat(RegionFixtures.read(other.resolve(name)))
          .isEqualTo(RegionFixtures.read(base.resolve("one").resolve(name)));
    }
  }

  @ParameterizedTest
  @ValueSource(longs = {-1L, 4_294_967_296L})
  void timestampOutOfRangeIsRejected(long timestamp) throws IOException {
    // spec: AR-R9 (unsigned 32 bit); the store forwards it to the region file
    try (AnvilChunkStore store = open(RegionCompression.ZLIB)) {
      Failures.expect(
          IllegalArgumentException.class,
          () -> store.save(Chunks.empty(registry, CHUNK_A, Chunks.OVERWORLD), timestamp));
    }
  }

  @Test
  void savedHeightmapsAndChunkHeaderSurviveForTheNether() throws IOException {
    // spec: CF-R1, CF-R33 (a Nether store writes 16 sections and yPos 0)
    Chunk nether = Chunks.empty(registry, new ChunkPos(3, 4), Chunks.NETHER);
    nether.setInhabitedTime(42L);
    Path folder = base.resolve("nether");

    try (AnvilChunkStore store =
        AnvilChunkStore.open(folder, Chunks.NETHER, Chunks.codec(), RegionCompression.ZLIB)) {
      store.save(nether, 7L);
      Chunk back = Chunks.present(store.load(new ChunkPos(3, 4))).value();
      assertThat(back.sections()).hasSize(16);
      assertThat(back.inhabitedTime()).isEqualTo(42L);
    }
    byte[] file = RegionFixtures.read(folder.resolve("r.0.0.mca"));
    assertThat(file.length % 4096).isZero();
  }
}
