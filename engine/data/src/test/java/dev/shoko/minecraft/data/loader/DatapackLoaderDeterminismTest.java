package dev.shoko.minecraft.data.loader;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackFiles;
import dev.shoko.minecraft.data.pack.PackOrigin;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.data.testing.MiniData;
import dev.shoko.minecraft.data.testing.Packs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

// spec: data-datapack-loading R19, R30, R40, R41 and ARCHITECTURE determinism: the same input gives
// the same output, whatever the file system, the archive or the order files were given in
class DatapackLoaderDeterminismTest {

  private static Map<String, String> sampleTree() {
    return Packs.tree(
        "data/minecraft/loot_table/b.json", "{\"b\":1.0}",
        "data/minecraft/loot_table/a.json", "{\"a\":[1,2,{\"c\":null}]}",
        "data/minecraft/loot_table/broken.json", "{",
        "data/minecraft/loot_table/dup.json", "{\"k\":1,\"k\":2}",
        "data/sonde/villager_trade/t/x.json", "[]",
        "data/minecraft/tags/block/logs.json", "{\"values\":[\"oak_log\",\"#more\"]}",
        "data/minecraft/tags/block/more.json", "{\"values\":[\"birch_log\",\"oak_log\"]}",
        "data/minecraft/tags/block/bad.json", "{\"values\":[\"nope\"]}",
        "data/sonde/function/f.mcfunction", "say 1",
        "data/sonde/tags/function/t.json", "{\"values\":[\"sonde:f\"]}",
        "data/sonde/structure/s.nbt", "bytes",
        "data/sonde/inconnu/x.json", "{}",
        "data/sonde/loot_table/Maj.json", "{}",
        "pack.mcmeta", Packs.MCMETA);
  }

  private static Pack pack(String id, PackFiles files) throws IOException {
    return Pack.open(id, PackOrigin.WORLD, PackType.DATA, files).value().orElseThrow();
  }

  private static String snapshot(Pack... packs) throws IOException {
    return MiniData.snapshot(MiniData.load(packs));
  }

  @Test
  void loadingTwiceGivesTheSameOutputDiagnosticsIncluded() throws IOException {
    // spec: determinism
    Pack pack = pack("W", PackFiles.inMemory(Packs.bytes(sampleTree())));
    Pack vanilla = Packs.implicit("vanilla", "data/minecraft/loot_table/v.json", "{}");
    assertThat(snapshot(vanilla, pack)).isEqualTo(snapshot(vanilla, pack));
    assertThat(snapshot(vanilla, pack)).contains("D ").contains("E ").contains("T ").contains("F ");
  }

  @Test
  void provisionalTheOrderFilesAreGivenInDoesNotMatter() throws IOException {
    // spec: R30, PackFiles.paths sorted by code point
    Map<String, String> forward = sampleTree();
    List<String> names = new ArrayList<>(forward.keySet());
    Collections.reverse(names);
    Map<String, String> backward = new LinkedHashMap<>();
    for (String name : names) {
      backward.put(name, forward.get(name));
    }
    assertThat(names).isNotEqualTo(new ArrayList<>(forward.keySet()));
    String one = snapshot(pack("W", PackFiles.inMemory(Packs.bytes(forward))));
    String two = snapshot(pack("W", PackFiles.inMemory(Packs.bytes(backward))));
    assertThat(two).isEqualTo(one);
  }

  @Test
  void provisionalAFolderAnArchiveAndMemoryGiveTheSameOutput(@TempDir Path tmp) throws IOException {
    // spec: R19 (archives accepted like folders), determinism
    Path folder = Files.createDirectories(tmp.resolve("folder"));
    Packs.writeTree(folder, sampleTree());
    Path archive = Packs.zip(tmp.resolve("pack.zip"), sampleTree());
    String memory = snapshot(pack("W", PackFiles.inMemory(Packs.bytes(sampleTree()))));
    String directory = snapshot(pack("W", PackFiles.directory(folder)));
    String zipped = snapshot(pack("W", PackFiles.archive(archive)));
    assertThat(directory).isEqualTo(memory);
    assertThat(zipped).isEqualTo(memory);
  }

  @Test
  void provisionalDiagnosticsFollowThePackStackThenTheSortedPaths() {
    // spec: DatapackLoader.load Javadoc ("in the order of the pack stack then of the sorted paths")
    Pack low =
        Packs.implicit(
            "low",
            "data/minecraft/loot_table/z.json",
            "{",
            "data/minecraft/loot_table/b.json",
            "{");
    Pack high =
        Packs.world(
            "high",
            "data/minecraft/loot_table/y.json",
            "{",
            "data/minecraft/loot_table/a.json",
            "{");
    Outcome<LoadedData> outcome = MiniData.load(low, high);
    assertThat(outcome.diagnostics())
        .extracting(d -> d.origin().pack() + ":" + d.origin().path())
        .containsExactly(
            "low:data/minecraft/loot_table/b.json",
            "low:data/minecraft/loot_table/z.json",
            "high:data/minecraft/loot_table/a.json",
            "high:data/minecraft/loot_table/y.json");
  }

  @Test
  @Timeout(60)
  void randomDatapacksLoadIdenticallyWhateverTheInsertionOrder() throws IOException {
    // spec: determinism over 60 random packs of 3 layers each
    Random random = new Random(0x5EEDL);
    for (int round = 0; round < 60; round++) {
      List<Map<String, String>> trees = new ArrayList<>();
      for (int layer = 0; layer < 3; layer++) {
        trees.add(randomTree(random));
      }
      String first = snapshotOf(trees, false);
      String second = snapshotOf(trees, true);
      assertThat(second).as("round %s", round).isEqualTo(first);
    }
  }

  private static Map<String, String> randomTree(Random random) {
    String[] registries = {"loot_table", "villager_trade", "worldgen/biome", "dimension"};
    String[] contents = {"{}", "[1]", "1", "1.0", "{\"a\":1,\"a\":2}", "{", "null", "\"s\""};
    Map<String, String> tree = new LinkedHashMap<>();
    int files = 3 + random.nextInt(12);
    for (int i = 0; i < files; i++) {
      String path =
          "data/"
              + (random.nextBoolean() ? "minecraft" : "sonde")
              + "/"
              + registries[random.nextInt(registries.length)]
              + "/e"
              + random.nextInt(6)
              + ".json";
      tree.put(path, contents[random.nextInt(contents.length)]);
    }
    for (int i = 0; i < 4; i++) {
      String tag = "t" + random.nextInt(4);
      String value =
          random.nextBoolean() ? "\"#t" + random.nextInt(4) + "\"" : "\"minecraft:stone\"";
      String path = "data/minecraft/tags/block/" + tag + ".json";
      tree.put(path, "{\"replace\":" + random.nextBoolean() + ",\"values\":[" + value + "]}");
    }
    return tree;
  }

  private static String snapshotOf(List<Map<String, String>> trees, boolean reversed)
      throws IOException {
    Pack[] packs = new Pack[trees.size()];
    for (int i = 0; i < packs.length; i++) {
      Map<String, String> tree = new LinkedHashMap<>();
      List<String> names = new ArrayList<>(trees.get(i).keySet());
      if (reversed) {
        Collections.reverse(names);
      }
      for (String name : names) {
        tree.put(name, trees.get(i).get(name));
      }
      tree.put("pack.mcmeta", Packs.MCMETA);
      packs[i] = pack("P" + i, PackFiles.inMemory(Packs.bytes(tree)));
    }
    return MiniData.snapshot(MiniData.load(packs));
  }

  @Test
  void theDiagnosticsOfTwoLoadsAreEqualRecordByRecord() throws IOException {
    // spec: determinism (code, file, path, in the same order)
    Pack pack = pack("W", PackFiles.inMemory(Packs.bytes(sampleTree())));
    List<Diagnostic> one = MiniData.load(pack).diagnostics();
    List<Diagnostic> two = MiniData.load(pack).diagnostics();
    assertThat(one).hasSameSizeAs(two).isNotEmpty();
    for (int i = 0; i < one.size(); i++) {
      assertThat(two.get(i).code()).isEqualTo(one.get(i).code());
      assertThat(two.get(i).origin()).isEqualTo(one.get(i).origin());
      assertThat(two.get(i).path()).isEqualTo(one.get(i).path());
    }
  }
}
