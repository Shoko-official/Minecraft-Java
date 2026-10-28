package dev.shoko.minecraft.data.pack;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.testing.Expect;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.Packs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// spec: data-datapack-loading R12, R13, R14, R19, R24 (PackFiles: folder, archive and memory)
class PackFilesTest {

  enum Kind {
    MEMORY,
    DIRECTORY,
    ARCHIVE
  }

  private static PackFiles make(Kind kind, Path tmp, Map<String, String> tree) throws IOException {
    return switch (kind) {
      case MEMORY -> PackFiles.inMemory(Packs.bytes(tree));
      case DIRECTORY -> {
        Path folder = Files.createDirectory(tmp.resolve("pack"));
        Packs.writeTree(folder, tree);
        yield PackFiles.directory(folder);
      }
      case ARCHIVE -> PackFiles.archive(Packs.zip(tmp.resolve("pack.zip"), tree));
    };
  }

  private static Map<String, String> sample() {
    return Packs.tree(
        "pack.mcmeta", "{}",
        "data/minecraft/loot_table/a.json", "A",
        "data/minecraft/datapacks/trade/pack.mcmeta", "T",
        "data/minecraft/datapacks/trade/data/minecraft/x.json", "X",
        "data/minecraft/datapacks2/other.json", "O",
        "data/ab/y.json", "Y",
        "data/a/z.json", "Z");
  }

  @ParameterizedTest
  @EnumSource(Kind.class)
  void provisionalPathsAreSortedByCodePointWithSlashSeparators(Kind kind, @TempDir Path tmp)
      throws IOException {
    // spec: PackFiles.paths Javadoc; R24 ("/" on every system)
    Map<String, String> tree = new LinkedHashMap<>();
    for (String path :
        new String[] {"b.json", "a/b.json", "a.json", "Z.json", "\351.json", "a_b"}) {
      tree.put(path, path);
    }
    assertThat(make(kind, tmp, tree).paths())
        .containsExactly("Z.json", "a.json", "a/b.json", "a_b", "b.json", "\351.json");
  }

  @ParameterizedTest
  @EnumSource(Kind.class)
  void readReturnsTheBytesOfAFile(Kind kind, @TempDir Path tmp) throws IOException {
    PackFiles files = make(kind, tmp, sample());
    assertThat(files.read("data/minecraft/loot_table/a.json")).isEqualTo(Jx.utf8("A"));
    assertThat(files.read("pack.mcmeta")).isEqualTo(Jx.utf8("{}"));
  }

  private static final byte[] BINARY = Jx.bytes(0, 255, 128, 10, 13, 0x1f, 0xEF, 0xBB, 0xBF);

  @Test
  void anInMemorySetKeepsBinaryContentUnchanged() throws IOException {
    // spec: R10 (structure files are NBT bytes, kept as they are)
    assertThat(PackFiles.inMemory(Map.of("data/b.nbt", BINARY)).read("data/b.nbt"))
        .isEqualTo(BINARY);
  }

  @Test
  void aFolderKeepsBinaryContentUnchanged(@TempDir Path tmp) throws IOException {
    Path root = Files.createDirectories(tmp.resolve("pack/data"));
    Files.write(root.resolve("b.nbt"), BINARY);
    assertThat(PackFiles.directory(tmp.resolve("pack")).read("data/b.nbt")).isEqualTo(BINARY);
  }

  @Test
  void anArchiveKeepsBinaryContentUnchanged(@TempDir Path tmp) throws IOException {
    Path archive = tmp.resolve("pack.zip");
    try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
      out.putNextEntry(new ZipEntry("data/b.nbt"));
      out.write(BINARY);
      out.closeEntry();
    }
    assertThat(PackFiles.archive(archive).read("data/b.nbt")).isEqualTo(BINARY);
  }

  @ParameterizedTest
  @EnumSource(Kind.class)
  void readingAMissingFileIsAnIoFailure(Kind kind, @TempDir Path tmp) throws IOException {
    PackFiles files = make(kind, tmp, sample());
    assertThat(Expect.thrown(() -> files.read("data/missing.json")))
        .isInstanceOf(IOException.class);
  }

  @ParameterizedTest
  @EnumSource(Kind.class)
  void provisionalSubtreeIsAViewWithPathsRelativeToTheDirectory(Kind kind, @TempDir Path tmp)
      throws IOException {
    // spec: R14 (built-in packs under data/minecraft/datapacks/<name>/), R48 (overlay directories)
    PackFiles view = make(kind, tmp, sample()).subtree("data/minecraft/datapacks/trade");
    assertThat(view.paths()).containsExactly("data/minecraft/x.json", "pack.mcmeta");
    assertThat(view.read("pack.mcmeta")).isEqualTo(Jx.utf8("T"));
    assertThat(view.read("data/minecraft/x.json")).isEqualTo(Jx.utf8("X"));
  }

  @ParameterizedTest
  @EnumSource(Kind.class)
  void subtreeStopsAtTheDirectoryBoundary(Kind kind, @TempDir Path tmp) throws IOException {
    PackFiles files = make(kind, tmp, sample());
    assertThat(files.subtree("data/a").paths()).containsExactly("z.json");
    assertThat(files.subtree("data/minecraft/datapacks").paths())
        .containsExactly("trade/data/minecraft/x.json", "trade/pack.mcmeta");
  }

  @ParameterizedTest
  @EnumSource(Kind.class)
  void subtreeOfAnUnknownDirectoryHasNoFile(Kind kind, @TempDir Path tmp) throws IOException {
    assertThat(make(kind, tmp, sample()).subtree("data/nothing").paths()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(Kind.class)
  void withoutHidesTheDirectoryAndKeepsTheRest(Kind kind, @TempDir Path tmp) throws IOException {
    // spec: R14 (the vanilla pack never walks data/minecraft/datapacks/)
    PackFiles view = make(kind, tmp, sample()).without("data/minecraft/datapacks");
    assertThat(view.paths())
        .containsExactly(
            "data/a/z.json",
            "data/ab/y.json",
            "data/minecraft/datapacks2/other.json",
            "data/minecraft/loot_table/a.json",
            "pack.mcmeta");
    assertThat(view.read("data/ab/y.json")).isEqualTo(Jx.utf8("Y"));
  }

  @ParameterizedTest
  @EnumSource(Kind.class)
  void withoutOfAnUnknownDirectoryChangesNothing(Kind kind, @TempDir Path tmp) throws IOException {
    PackFiles files = make(kind, tmp, sample());
    assertThat(files.without("data/nothing").paths()).isEqualTo(files.paths());
  }

  @ParameterizedTest
  @EnumSource(Kind.class)
  void aFileHiddenByWithoutCannotBeReadThroughTheView(Kind kind, @TempDir Path tmp)
      throws IOException {
    PackFiles view = make(kind, tmp, sample()).without("data/minecraft/datapacks");
    assertThat(Expect.thrown(() -> view.read("data/minecraft/datapacks/trade/pack.mcmeta")))
        .isInstanceOf(IOException.class);
  }

  @Test
  void anInMemorySetDoesNotFollowLaterChangesToTheMap() throws IOException {
    Map<String, byte[]> map = new LinkedHashMap<>();
    map.put("a.json", Jx.utf8("1"));
    PackFiles files = PackFiles.inMemory(map);
    map.put("b.json", Jx.utf8("2"));
    map.remove("a.json");
    assertThat(files.paths()).containsExactly("a.json");
    assertThat(files.read("a.json")).isEqualTo(Jx.utf8("1"));
  }

  @Test
  void anEmptyInMemorySetHasNoFile() throws IOException {
    assertThat(PackFiles.inMemory(Map.of()).paths()).isEmpty();
  }

  @Test
  void aFolderIgnoresEmptyDirectories(@TempDir Path tmp) throws IOException {
    // spec: PackFiles.paths ("directories excluded")
    Path root = Files.createDirectories(tmp.resolve("pack"));
    Files.createDirectories(root.resolve("data/empty/sub"));
    Packs.writeTree(root, Packs.tree("data/full/a.json", "x"));
    assertThat(PackFiles.directory(root).paths()).containsExactly("data/full/a.json");
  }

  @Test
  void provisionalAnArchiveIgnoresDirectoryEntriesAndStartsAtTheRootOfTheArchive(@TempDir Path tmp)
      throws IOException {
    // spec: R19
    Path archive = tmp.resolve("pack.zip");
    try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
      out.putNextEntry(new ZipEntry("data/"));
      out.closeEntry();
      out.putNextEntry(new ZipEntry("data/minecraft/"));
      out.closeEntry();
      out.putNextEntry(new ZipEntry("data/minecraft/a.json"));
      out.write(Jx.utf8("{}"));
      out.closeEntry();
      out.putNextEntry(new ZipEntry("pack.mcmeta"));
      out.write(Jx.utf8("{}"));
      out.closeEntry();
    }
    assertThat(PackFiles.archive(archive).paths())
        .containsExactly("data/minecraft/a.json", "pack.mcmeta");
  }

  @Test
  void aFolderListsFilesCreatedInAnyOrderTheSame(@TempDir Path tmp) throws IOException {
    // spec: determinism: the order never depends on the file system
    Path first = Files.createDirectories(tmp.resolve("one"));
    Path second = Files.createDirectories(tmp.resolve("two"));
    Packs.writeTree(first, Packs.tree("b/x.json", "1", "a/x.json", "2", "c.json", "3"));
    Packs.writeTree(second, Packs.tree("c.json", "3", "a/x.json", "2", "b/x.json", "1"));
    assertThat(PackFiles.directory(first).paths()).isEqualTo(PackFiles.directory(second).paths());
    assertThat(PackFiles.directory(first).paths())
        .containsExactly("a/x.json", "b/x.json", "c.json");
  }
}
