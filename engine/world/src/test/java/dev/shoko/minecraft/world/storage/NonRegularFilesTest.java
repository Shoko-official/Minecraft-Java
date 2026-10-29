package dev.shoko.minecraft.world.storage;

import static dev.shoko.minecraft.world.support.RegionFixtures.CHUNK_A;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.shoko.minecraft.world.AbsentCause;
import dev.shoko.minecraft.world.block.BlockStateRegistry;
import dev.shoko.minecraft.world.chunk.Chunk;
import dev.shoko.minecraft.world.support.Blocks;
import dev.shoko.minecraft.world.support.Chunks;
import dev.shoko.minecraft.world.support.Failures;
import dev.shoko.minecraft.world.support.RegionFixtures;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A region file {@code r.X.Z.mca} or an external file {@code c.X.Z.mcc} that is not a plain file (a
 * directory, a symbolic link, even a dangling one) is a failure of the file system (AR-R27): an
 * IOException, nothing is written, followed or marked. The message is part of what is pinned: it
 * tells the plain-file check apart from the failure the operating system would give later.
 */
class NonRegularFilesTest {

  private static final String NOT_REGULAR = "not a regular file";

  @TempDir Path base;

  private Path folder;
  private Path outside;

  private final BlockStateRegistry registry = Blocks.registry();

  private Path region() throws IOException {
    if (folder == null) {
      folder = Files.createDirectories(base.resolve("world").resolve("region"));
    }
    return folder;
  }

  private Path outside() throws IOException {
    if (outside == null) {
      outside = Files.createDirectories(base.resolve("elsewhere"));
    }
    return outside;
  }

  private AnvilChunkStore open() throws IOException {
    return AnvilChunkStore.open(region(), Chunks.OVERWORLD, Chunks.codec(), RegionCompression.ZLIB);
  }

  private Chunk chunk() {
    return Chunks.empty(registry, CHUNK_A, Chunks.OVERWORLD);
  }

  private static void symlinkOrSkip(Path link, Path target) throws IOException {
    try {
      Files.createSymbolicLink(link, target);
    } catch (UnsupportedOperationException | FileSystemException e) {
      assumeTrue(false, "this platform does not let the test create a symbolic link: " + e);
    }
  }

  private static void assertNotRegular(IOException failure) {
    assertThat(failure).isInstanceOf(FileSystemException.class).hasMessageContaining(NOT_REGULAR);
  }

  @Test
  void aDirectoryNamedLikeTheRegionFileFailsTheLoadAndTheSaveAndSetsNoMark() throws IOException {
    // spec: AR-R27 (a file system failure is an IOException and sets no mark)
    Path regionFile = RegionFixtures.regionFile(region());
    Files.createDirectory(regionFile);

    try (AnvilChunkStore store = open()) {
      assertNotRegular(Failures.expect(IOException.class, () -> store.load(CHUNK_A)));
      assertNotRegular(Failures.expect(IOException.class, () -> store.save(chunk(), 9L)));
      assertThat(RegionFixtures.names(region()))
          .containsExactly(regionFile.getFileName().toString());
      assertThat(Files.isDirectory(regionFile, java.nio.file.LinkOption.NOFOLLOW_LINKS)).isTrue();

      Files.delete(regionFile);
      RegionFixtures.writeRg1(region(), 2);
      assertThat(Chunks.present(store.load(CHUNK_A)).value().pos()).isEqualTo(CHUNK_A);
    }
  }

  @Test
  void aDirectoryNamedLikeTheRegionFileFailsTheRegionFileToo() throws IOException {
    // spec: AR-R27 and the RegionFile contract: read and write refuse it, nothing is created in it
    Path regionFile = RegionFixtures.regionFile(region());
    Files.createDirectory(regionFile);
    Files.writeString(regionFile.resolve("inside"), "keep");

    assertNotRegular(Failures.expect(IOException.class, () -> RegionFixtures.open(region())));

    assertThat(RegionFixtures.names(regionFile)).containsExactly("inside");
  }

  @Test
  void aDirectoryNamedLikeTheExternalFileFailsTheLoadAndSetsNoMark() throws IOException {
    // spec: AR-R17 and AR-R27: the external stream of a chunk is a plain file or an IOException
    RegionFixtures.writeVariant(region(), "external");
    Path mcc = region().resolve("c.-1.-1.mcc");
    byte[] stream = RegionFixtures.read(mcc);
    Files.delete(mcc);
    Files.createDirectory(mcc);
    byte[] regionBefore = RegionFixtures.read(RegionFixtures.regionFile(region()));

    try (AnvilChunkStore store = open()) {
      assertNotRegular(Failures.expect(IOException.class, () -> store.load(CHUNK_A)));
      assertNotRegular(Failures.expect(IOException.class, () -> store.load(CHUNK_A)));
      assertThat(Files.isDirectory(mcc, java.nio.file.LinkOption.NOFOLLOW_LINKS)).isTrue();
      assertThat(RegionFixtures.read(RegionFixtures.regionFile(region()))).isEqualTo(regionBefore);

      Files.delete(mcc);
      Files.write(mcc, stream);
      assertThat(Chunks.present(store.load(CHUNK_A)).value().pos()).isEqualTo(CHUNK_A);
    }
  }

  @Test
  void aDirectoryNamedLikeTheExternalFileFailsTheWriteOfAnExternalChunk() throws IOException {
    // spec: AR-R17, AR-R27: a chunk that goes to its .mcc cannot replace a directory; no location
    // is written, so the position stays empty
    Path mcc = region().resolve("c.-1.-1.mcc");
    Files.createDirectory(mcc);
    byte[] largeNbt = new byte[1_100_000];

    try (RegionFile file = RegionFixtures.open(region())) {
      Failures.expect(
          IOException.class, () -> file.write(CHUNK_A, largeNbt, RegionCompression.NONE, 1L));
      assertThat(Chunks.absent(file.read(CHUNK_A)).cause()).isEqualTo(AbsentCause.EMPTY_SLOT);
    }

    assertThat(Files.isDirectory(mcc, java.nio.file.LinkOption.NOFOLLOW_LINKS)).isTrue();
    assertThat(RegionFixtures.names(region()))
        .containsExactlyInAnyOrder("c.-1.-1.mcc", "r.-1.-1.mca");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void aDirectoryNamedLikeTheExternalFileFailsTheSaveOfAChunkThatFitsBeforeAnyWrite(
      boolean withContent) throws IOException {
    // spec: AR-R17, AR-R18 and AR-R27: a chunk that fits would remove a leftover .mcc, which is a
    // plain file or nothing; the refusal comes before the record is written, so the caller never
    // sees a failure for data that is committed
    byte[] regionBefore = RegionFixtures.writeRg1(region(), 2);
    Path mcc = region().resolve("c.-1.-1.mcc");
    Files.createDirectory(mcc);
    if (withContent) {
      Files.writeString(mcc.resolve("inside"), "keep");
    }

    try (AnvilChunkStore store = open()) {
      assertNotRegular(Failures.expect(IOException.class, () -> store.save(chunk(), 9L)));
      assertThat(RegionFixtures.read(RegionFixtures.regionFile(region()))).isEqualTo(regionBefore);
      assertThat(Files.isDirectory(mcc, LinkOption.NOFOLLOW_LINKS)).isTrue();
      assertThat(RegionFixtures.names(mcc)).isEqualTo(withContent ? List.of("inside") : List.of());

      Files.deleteIfExists(mcc.resolve("inside"));
      Files.delete(mcc);
      store.save(chunk(), 9L);
      assertThat(Chunks.present(store.load(CHUNK_A)).value().pos()).isEqualTo(CHUNK_A);
    }
  }

  @Test
  void aDirectoryNamedLikeTheExternalFileLeavesAMissingRegionFileMissing() throws IOException {
    // spec: AR-R27: nothing is created before the refusal, not even the region file
    Path mcc = region().resolve("c.-1.-1.mcc");
    Files.createDirectory(mcc);

    try (AnvilChunkStore store = open()) {
      assertNotRegular(Failures.expect(IOException.class, () -> store.save(chunk(), 9L)));
    }

    assertThat(RegionFixtures.names(region())).containsExactly("c.-1.-1.mcc");
  }

  @Test
  void aSymbolicLinkNamedLikeTheExternalFileFailsTheSaveOfAChunkThatFits() throws IOException {
    // spec: AR-R27: a link is neither followed nor removed by the cleanup of a leftover .mcc
    byte[] regionBefore = RegionFixtures.writeRg1(region(), 2);
    Path targetFile = outside().resolve("target.mcc");
    Files.writeString(targetFile, "keep");
    Path mcc = region().resolve("c.-1.-1.mcc");
    symlinkOrSkip(mcc, targetFile);

    try (AnvilChunkStore store = open()) {
      assertNotRegular(Failures.expect(IOException.class, () -> store.save(chunk(), 9L)));
    }

    assertThat(RegionFixtures.read(RegionFixtures.regionFile(region()))).isEqualTo(regionBefore);
    assertThat(Files.isSymbolicLink(mcc)).isTrue();
    assertThat(Files.readString(targetFile)).isEqualTo("keep");
  }

  @Test
  void aSymbolicLinkToARegionFileOutsideTheWorldIsRefused() throws IOException {
    // spec: AR-R27: a link is not followed, to read nor to write
    byte[] target = RegionFixtures.writeRg1(outside(), 2);
    Path targetFile = RegionFixtures.regionFile(outside());
    symlinkOrSkip(RegionFixtures.regionFile(region()), targetFile);

    try (AnvilChunkStore store = open()) {
      assertNotRegular(Failures.expect(IOException.class, () -> store.load(CHUNK_A)));
      assertNotRegular(Failures.expect(IOException.class, () -> store.save(chunk(), 9L)));
    }

    assertThat(RegionFixtures.read(targetFile)).isEqualTo(target);
    assertThat(Files.isSymbolicLink(RegionFixtures.regionFile(region()))).isTrue();
  }

  @Test
  void aSymbolicLinkToARegionFileIsRefusedByTheRegionFileToo() throws IOException {
    // spec: AR-R27 (RegionFile.open)
    byte[] target = RegionFixtures.writeRg1(outside(), 2);
    Path targetFile = RegionFixtures.regionFile(outside());
    symlinkOrSkip(RegionFixtures.regionFile(region()), targetFile);

    assertNotRegular(
        Failures.expect(IOException.class, () -> RegionFile.open(region(), RegionFixtures.REGION)));

    assertThat(RegionFixtures.read(targetFile)).isEqualTo(target);
  }

  @Test
  void aDanglingSymbolicLinkIsNotAnAbsentRegionFile() throws IOException {
    // spec: AR-R27: a link whose target does not exist is not "no region file" (AR-R19), and it is
    // not created through by a write
    Path target = outside().resolve("r.-1.-1.mca");
    symlinkOrSkip(RegionFixtures.regionFile(region()), target);

    try (AnvilChunkStore store = open()) {
      assertNotRegular(Failures.expect(IOException.class, () -> store.load(CHUNK_A)));
      assertNotRegular(Failures.expect(IOException.class, () -> store.save(chunk(), 9L)));
    }

    assertThat(Files.exists(target)).isFalse();
  }

  @Test
  void aSymbolicLinkToAnExternalFileOutsideTheWorldIsRefused() throws IOException {
    // spec: AR-R17 and AR-R27: the .mcc is not followed either
    RegionFixtures.writeVariant(region(), "external");
    Path mcc = region().resolve("c.-1.-1.mcc");
    byte[] stream = RegionFixtures.read(mcc);
    Files.delete(mcc);
    Path targetFile = outside().resolve("stolen.mcc");
    Files.write(targetFile, stream);
    symlinkOrSkip(mcc, targetFile);

    try (AnvilChunkStore store = open()) {
      assertNotRegular(Failures.expect(IOException.class, () -> store.load(CHUNK_A)));
    }

    assertThat(RegionFixtures.read(targetFile)).isEqualTo(stream);
    assertThat(Files.isSymbolicLink(mcc)).isTrue();
  }

  @Test
  void aDanglingSymbolicLinkToAnExternalFileIsNotAMissingExternalFile() throws IOException {
    // spec: AR-R18 (a missing .mcc refuses the chunk) does not cover a link: AR-R27
    RegionFixtures.writeVariant(region(), "external");
    Path mcc = region().resolve("c.-1.-1.mcc");
    Files.delete(mcc);
    symlinkOrSkip(mcc, outside().resolve("nothing.mcc"));

    try (AnvilChunkStore store = open()) {
      assertNotRegular(Failures.expect(IOException.class, () -> store.load(CHUNK_A)));
    }
  }
}
