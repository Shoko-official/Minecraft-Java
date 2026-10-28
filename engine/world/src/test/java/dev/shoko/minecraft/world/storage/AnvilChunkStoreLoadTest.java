package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_A;
import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_B;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.coord.ChunkPos;
import dev.shoko.minecraft.world.AbsentCause;
import dev.shoko.minecraft.world.ReadResult;
import dev.shoko.minecraft.world.ReadWarning;
import dev.shoko.minecraft.world.RefusedCause;
import dev.shoko.minecraft.world.WarningCode;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.support.ChunkNbt;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.NestedNbt;
import dev.shoko.minecraft.world.support.RegionFixtures;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** AR-R19 to AR-R21, AR-R26, AR-R29, AR-R30, CF-R35: loading a chunk through the store. */
class AnvilChunkStoreLoadTest {

  @TempDir Path folder;

  private AnvilChunkStore open() {
    return AnvilChunkStore.open(folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB);
  }

  private ReadResult<Chunk> load(ChunkPos pos) throws IOException {
    try (AnvilChunkStore store = open()) {
      return store.load(pos);
    }
  }

  @Test
  void worldWithoutRegionFilesHasNoChunk() throws IOException {
    // spec: AR-R19 (NO_REGION_FILE, normal: no warning)
    ReadResult.Absent<Chunk> result = Chunks.absent(load(CHUNK_A));

    assertThat(result.cause()).isEqualTo(AbsentCause.NO_REGION_FILE);
    assertThat(result.warnings()).isEmpty();
    assertThat(Files.exists(RegionFixtures.regionFile(folder))).isFalse();
  }

  @Test
  void regionFolderThatDoesNotExistYetIsValid() throws IOException {
    // spec: AnvilChunkStore.open contract (the region folder may not exist yet)
    Path missing = folder.resolve("dimensions/minecraft/overworld/region");

    try (AnvilChunkStore store =
        AnvilChunkStore.open(missing, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB)) {
      assertThat(Chunks.absent(store.load(CHUNK_A)).cause()).isEqualTo(AbsentCause.NO_REGION_FILE);
    }
    assertThat(Files.exists(missing)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4})
  void chunkAIsLoadedWhateverItsCompression(int type) throws IOException {
    // spec: RG1, RG2 (the four types), CF-R20
    RegionFixtures.writeRg1(folder, type);

    ReadResult.Present<Chunk> result = Chunks.present(load(CHUNK_A));

    assertThat(result.warnings()).isEmpty();
    Chunks.assertSameChunk(
        Chunks.decodeClean(ChunkNbt.chunk(-1, -1, true, "bedrock")), result.value());
  }

  @Test
  void chunkBIsLoadedFromItsSlotZero() throws IOException {
    // spec: RG1 chunk B (-32, -32), index 0
    RegionFixtures.writeRg1(folder, 2);

    Chunk chunk = Chunks.present(load(CHUNK_B)).value();

    assertThat(chunk.pos()).isEqualTo(CHUNK_B);
    Chunks.assertSameChunk(Chunks.decodeClean(ChunkNbt.chunk(-32, -32, false, "bedrock")), chunk);
  }

  @Test
  void externalChunkIsLoaded() throws IOException {
    // spec: RG2 external, AR-R17
    RegionFixtures.writeVariant(folder, "external");

    assertThat(Chunks.present(load(CHUNK_A)).value().section(-4).blockStates().get(0).id())
        .isEqualTo(4);
  }

  @ParameterizedTest
  @CsvSource({
    "string_palette",
    "partial_props",
    "unknown_block",
    "bad_value",
    "unknown_biome",
    "missing_block_states",
    "missing_biomes",
    "missing_section",
    "light_sections",
    "far_sections",
    "unsorted_sections",
    "ypos_wrong",
    "ypos_absent",
    "skylight_chain",
    "bad_heightmap_length",
    "heightmap_overflow",
    "postprocessing"
  })
  void variantsThatAreStillReadableArePresent(String variant) throws IOException {
    // spec: RG3 and RG4 (PRESENT rows)
    RegionFixtures.writeVariant(folder, variant);

    Chunks.present(load(CHUNK_A));
  }

  @ParameterizedTest
  @CsvSource({
    "bad_length,INVALID_CONTAINER", "empty_palette,INVALID_CONTAINER",
    "index_overflow,INVALID_CONTAINER", "duplicate_section,INVALID_SECTION",
    "no_section_y,INVALID_SECTION", "no_sections_key,INVALID_FIELD",
    "compression_127,UNSUPPORTED_COMPRESSION", "missing_mcc,MISSING_EXTERNAL_FILE"
  })
  void variantsThatAreRefusedHaveTheirCause(String variant, RefusedCause cause) throws IOException {
    // spec: RG3 and RG4 (REFUSED rows), AR-R26 (message names the file and the index)
    RegionFixtures.writeVariant(folder, variant);

    ReadResult.Refused<Chunk> result = Chunks.refused(load(CHUNK_A));

    assertThat(result.cause()).isEqualTo(cause);
    assertThat(result.message()).contains("r.-1.-1.mca").contains("1023");
  }

  @ParameterizedTest
  @CsvSource({"root_not_compound", "invalid_nbt", "corrupt_zlib", "bomb"})
  void provisionalUnreadableNbtMakesTheChunkAbsentWithAWarning(String variant) throws IOException {
    // spec: AR-R30 a, b, RG4 (root_not_compound, invalid_nbt, corrupt_zlib, bomb: ABSENT
    // UNREADABLE_RECORD)
    RegionFixtures.writeVariant(folder, variant);

    ReadResult.Absent<Chunk> result = Chunks.absent(load(CHUNK_A));

    assertThat(result.cause()).isEqualTo(AbsentCause.UNREADABLE_RECORD);
    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.UNREADABLE_RECORD);
  }

  @ParameterizedTest
  @CsvSource({"bad_offset", "offset_beyond"})
  void invalidLocationMakesTheChunkAbsentWithAWarning(String variant) throws IOException {
    // spec: AR-R20, RG3 bad_offset and offset_beyond
    RegionFixtures.writeVariant(folder, variant);

    ReadResult.Absent<Chunk> result = Chunks.absent(load(CHUNK_A));

    assertThat(result.cause()).isEqualTo(AbsentCause.INVALID_LOCATION);
    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.INVALID_LOCATION);
  }

  @Test
  void positionThatDisagreesWithTheNbtMakesTheChunkAbsent() throws IOException {
    // spec: AR-R21, RG3 wrong_pos (xPos -2 at the index of chunk (-1, -1))
    RegionFixtures.writeVariant(folder, "wrong_pos");

    ReadResult.Absent<Chunk> result = Chunks.absent(load(CHUNK_A));

    assertThat(result.cause()).isEqualTo(AbsentCause.POSITION_MISMATCH);
    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.POSITION_MISMATCH);
  }

  @Test
  void rootWithANameIsAcceptedWithAWarning() throws IOException {
    // spec: AR-R30 c, RG4 root_named (the name is ignored)
    RegionFixtures.writeVariant(folder, "root_named");

    ReadResult.Present<Chunk> result = Chunks.present(load(CHUNK_A));

    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.ROOT_NAME_IGNORED);
    assertThat(result.value().section(-4).blockStates().get(0).id()).isEqualTo(4);
  }

  @Test
  void bytesAfterTheRootAreIgnoredWithAWarning() throws IOException {
    // spec: AR-R30 d, RG4 trailing_bytes
    RegionFixtures.writeVariant(folder, "trailing_bytes");

    ReadResult.Present<Chunk> result = Chunks.present(load(CHUNK_A));

    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.TRAILING_BYTES);
  }

  @Test
  void warningsOfTheCodecComeThroughTheStore() throws IOException {
    // spec: AR-R26 (a warning list on a present result), BS-R21
    RegionFixtures.writeVariant(folder, "unknown_block");

    ReadResult.Present<Chunk> result = Chunks.present(load(CHUNK_A));

    assertThat(result.warnings())
        .extracting(ReadWarning::code)
        .containsExactly(WarningCode.UNKNOWN_BLOCK);
  }

  @Test
  void nestingBeyond512LevelsIsRefusedAsTooDeep() throws IOException {
    // spec: AR-R30 e, core-nbt R31 (512 nested containers are read, 513 refused)
    byte[] nbt = NestedNbt.chunkWithDepth(RegionFixtures.nbtOfA(), 513);
    RegionFixtures.write(RegionFixtures.regionFile(folder), recordOf(nbt));

    ReadResult.Refused<Chunk> result = Chunks.refused(load(CHUNK_A));

    assertThat(result.cause()).isEqualTo(RefusedCause.TOO_DEEP);
  }

  @Test
  void nestingOf512LevelsIsStillReadAsAChunk() throws IOException {
    // spec: core-nbt R31 (512 levels, the root included, are accepted), CF-R21 (unknown key
    // ignored)
    byte[] nbt = NestedNbt.chunkWithDepth(RegionFixtures.nbtOfA(), 512);
    RegionFixtures.write(RegionFixtures.regionFile(folder), recordOf(nbt));

    ReadResult.Present<Chunk> result = Chunks.present(load(CHUNK_A));

    assertThat(result.value().section(-4).blockStates().get(0).id()).isEqualTo(4);
  }

  @Test
  void announcedGiantArrayIsRefusedAsTooLargeBeforeItIsRead() throws IOException {
    // spec: AR-R30 e, core-nbt R60 (a Long_Array header of 2^31 - 1 elements on a truncated input)
    byte[] bytes = dev.shoko.minecraft.world.support.Hex.bytes("0a 00 00 0c 00 01 61 7f ff ff ff");
    RegionFixtures.write(RegionFixtures.regionFile(folder), recordOf(bytes));

    ReadResult.Refused<Chunk> result = Chunks.refused(load(CHUNK_A));

    assertThat(result.cause()).isEqualTo(RefusedCause.TOO_LARGE);
  }

  private static byte[] recordOf(byte[] nbt) {
    return dev.shoko.minecraft.world.support.Regions.file(
        java.util.List.of(
            new dev.shoko.minecraft.world.support.Regions.Blob(
                RegionFixtures.INDEX_A, 3, nbt, 1L)));
  }

  @Test
  void decompressionLimitOfTheStoreAppliesToTheRecord() throws IOException {
    // spec: AR-R29, LIM1 (limit 5999 present, 5998 TOO_LARGE), bomb with 1 MiB
    RegionFixtures.writeRg1(folder, 2);
    try (AnvilChunkStore exact =
            AnvilChunkStore.open(
                folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB, 5999);
        AnvilChunkStore tight =
            AnvilChunkStore.open(
                folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB, 5998)) {
      Chunks.present(exact.load(CHUNK_A));
      assertThat(Chunks.refused(tight.load(CHUNK_A)).cause()).isEqualTo(RefusedCause.TOO_LARGE);
    }
    RegionFixtures.writeVariant(folder, "bomb");
    try (AnvilChunkStore small =
        AnvilChunkStore.open(
            folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB, 1 << 20)) {
      assertThat(Chunks.refused(small.load(CHUNK_A)).cause()).isEqualTo(RefusedCause.TOO_LARGE);
    }
  }

  @Test
  void negativeLimitIsRejected() {
    // spec: AnvilChunkStore.open contract (IllegalArgumentException)
    Failures.expect(
        IllegalArgumentException.class,
        () ->
            AnvilChunkStore.open(
                folder, Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB, -1));
  }

  @Test
  void aDirectoryInPlaceOfTheRegionFileIsAnInputOutputFailure() throws IOException {
    // spec: AR-R27 (a file system failure is an IOException, not one of the three issues)
    Files.createDirectories(RegionFixtures.regionFile(folder));

    Failures.expect(IOException.class, () -> load(CHUNK_A));
  }

  @Test
  void otherRegionsAreIndependent() throws IOException {
    // spec: AR-R4 (chunk (0, 0) is in r.0.0.mca, which does not exist)
    RegionFixtures.writeRg1(folder, 2);

    assertThat(Chunks.absent(load(new ChunkPos(0, 0))).cause())
        .isEqualTo(AbsentCause.NO_REGION_FILE);
    assertThat(Chunks.absent(load(new ChunkPos(-2, -1))).cause()).isEqualTo(AbsentCause.EMPTY_SLOT);
  }

  @Test
  void loadingNeverWrites() throws IOException {
    // spec: AR "Entrees / sorties" (the reader never writes during a read)
    byte[] before = RegionFixtures.writeVariant(folder, "unknown_block");

    load(CHUNK_A);
    load(CHUNK_B);

    assertThat(RegionFixtures.read(RegionFixtures.regionFile(folder))).isEqualTo(before);
    assertThat(RegionFixtures.names(folder)).containsExactly("r.-1.-1.mca");
  }
}
