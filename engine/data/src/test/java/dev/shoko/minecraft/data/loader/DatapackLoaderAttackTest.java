package dev.shoko.minecraft.data.loader;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackFiles;
import dev.shoko.minecraft.data.pack.PackOrigin;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.data.tag.TagResolution;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.MiniData;
import dev.shoko.minecraft.data.testing.Packs;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: data-datapack-loading R15, R24, R33, R54, R57, R59 (adversarial): hostile packs. Errors in
// the data are diagnostics and never exceptions; the output keeps its invariants. Fixed seeds.
class DatapackLoaderAttackTest {

  private static final String[] FOLDERS = {
    "loot_table", "villager_trade", "worldgen/biome", "dimension", "block", "tags/block",
    "tags/loot_table", "tags/function", "tags/structure", "function", "structure", "unknown",
    "tags/unknown", ""
  };
  private static final String[] NAMES = {
    "a", "b", "c/d", "Up", "x y", "..", "e.json", "sub/dir/f", "z", ".", "caf\351", "a_b-c.d"
  };
  private static final String[] NAMESPACES = {"minecraft", "sonde", "Sonde", "a b", "..", "x-y.z"};
  private static final String[] EXTENSIONS = {
    ".json", ".json", ".json", ".mcfunction", ".nbt", ".txt", ""
  };
  private static final String[] CONTENTS = {
    "{}",
    "[]",
    "1",
    "1.0",
    "-0",
    "null",
    "\"s\"",
    "{",
    "[1,",
    "{\"values\":[]}",
    "{\"values\":[\"minecraft:stone\"]}",
    "{\"values\":[\"#a\",\"b\"]}",
    "{\"values\":[\"#b\"]}",
    "{\"values\":[{\"id\":\"a\",\"required\":false}]}",
    "{\"replace\":true,\"values\":[\"a\"]}",
    "{\"values\":5}",
    "1e999",
    "99999999999999999999",
    "{\"a\":1,\"a\":2}",
    "\ud800",
    "say 1",
    ""
  };

  private static Map<String, String> randomTree(Random random) {
    Map<String, String> tree = new LinkedHashMap<>();
    int files = random.nextInt(25);
    for (int i = 0; i < files; i++) {
      String folder = FOLDERS[random.nextInt(FOLDERS.length)];
      String path =
          "data/"
              + NAMESPACES[random.nextInt(NAMESPACES.length)]
              + "/"
              + folder
              + (folder.isEmpty() ? "" : "/")
              + NAMES[random.nextInt(NAMES.length)]
              + EXTENSIONS[random.nextInt(EXTENSIONS.length)];
      tree.put(path, CONTENTS[random.nextInt(CONTENTS.length)]);
    }
    return tree;
  }

  private static Pack worldPack(String id, Map<String, String> tree) {
    Map<String, String> files = new LinkedHashMap<>(tree);
    files.put("pack.mcmeta", Packs.MCMETA);
    return Packs.open(id, PackOrigin.WORLD, files);
  }

  private static Set<String> ids(Collection<Identifier> ids) {
    Set<String> set = new HashSet<>();
    ids.forEach(id -> set.add(id.toString()));
    return set;
  }

  private static void assertInvariants(Outcome<LoadedData> outcome, String context) {
    assertThat(outcome.value()).as(context).isPresent();
    LoadedData data = outcome.value().get();
    Map<String, Set<String>> universe = new HashMap<>();
    for (ElementRegistryData registry : data.elementRegistries()) {
      List<Identifier> sorted = new ArrayList<>(registry.elements().keySet());
      sorted.sort(null);
      assertThat(new ArrayList<>(registry.elements().keySet())).as(context).isEqualTo(sorted);
      universe.put(registry.registry().toString(), ids(registry.elements().keySet()));
    }
    data.staticRegistries().forEach(r -> universe.put(r.registry().toString(), ids(r.ids())));
    universe.put("minecraft:function", ids(data.functions().keySet()));
    for (String name :
        List.of(
            "minecraft:block",
            "minecraft:item",
            "minecraft:loot_table",
            "minecraft:villager_trade",
            "minecraft:worldgen/biome",
            "minecraft:dimension",
            "minecraft:function")) {
      TagResolution tags = data.tags(Identifier.parse(name)).orElseThrow();
      Set<String> resolved = ids(tags.tags());
      Set<String> failed = ids(tags.failed());
      assertThat(Collections.disjoint(resolved, failed)).as("%s %s", context, name).isTrue();
      for (Identifier tag : tags.tags()) {
        List<Identifier> members = tags.members(tag).orElseThrow();
        assertThat(ids(members)).as("%s tag %s", context, tag).isSubsetOf(universe.get(name));
        assertThat(ids(members)).as("no duplicate in %s", tag).hasSameSizeAs(members);
      }
      for (Identifier tag : tags.failed()) {
        assertThat(tags.members(tag)).as("failed tag %s", tag).isEmpty();
      }
    }
    assertThat(data.packReports()).as(context).isNotEmpty();
  }

  @Test
  @Timeout(60)
  void provisionalRandomHostilePacksLoadWithoutExceptionAndKeepTheInvariants() throws IOException {
    // spec: R15, R24, R33, R54
    Random random = new Random(0xBADC0DEL);
    for (int i = 0; i < 400; i++) {
      Pack vanilla = Packs.implicit("vanilla", flatten(randomTree(random)));
      Pack first = worldPack("A", randomTree(random));
      Pack second = worldPack("B", randomTree(random));
      Outcome<LoadedData> outcome = MiniData.load(vanilla, first, second);
      assertInvariants(outcome, "round " + i);
      assertThat(Diags.codes(outcome))
          .as("round %s", i)
          .doesNotContain(DiagnosticCode.PACK_FLAGS_INACTIVE, DiagnosticCode.PACK_INVALID);
    }
  }

  private static String[] flatten(Map<String, String> tree) {
    List<String> flat = new ArrayList<>();
    tree.forEach(
        (path, content) -> {
          flat.add(path);
          flat.add(content);
        });
    return flat.toArray(String[]::new);
  }

  @Test
  @Timeout(60)
  void provisionalHostileFilesAreReportedWhileTheOthersLoad() throws IOException {
    // spec: R54, R57, R59: depth bomb, huge number, bad UTF-8 and a BOM next to a valid file
    Map<String, byte[]> files = new LinkedHashMap<>();
    files.put("pack.mcmeta", Jx.utf8(Packs.MCMETA));
    files.put("data/sonde/loot_table/deep.json", Jx.utf8("[".repeat(200_000)));
    files.put("data/sonde/loot_table/number.json", Jx.utf8("[" + "9".repeat(100_000) + "]"));
    files.put("data/sonde/loot_table/utf8.json", Jx.bytes('"', 0xED, 0xA0, 0x80, '"'));
    files.put(
        "data/sonde/loot_table/bom.json", Jx.concat(Jx.bytes(0xEF, 0xBB, 0xBF), Jx.utf8("{}")));
    files.put("data/sonde/loot_table/empty.json", new byte[0]);
    files.put("data/sonde/loot_table/blank.json", Jx.utf8(" \n\t "));
    files.put("data/sonde/loot_table/ok.json", Jx.utf8("{}"));
    Pack pack =
        Pack.open("W", PackOrigin.WORLD, PackType.DATA, PackFiles.inMemory(files))
            .value()
            .orElseThrow();
    Outcome<LoadedData> outcome = MiniData.load(pack);
    assertThat(MiniData.elementIds(outcome.value().orElseThrow(), "minecraft:loot_table"))
        .containsExactly("sonde:ok");
    assertThat(Diags.codes(outcome))
        .containsExactlyInAnyOrder(
            DiagnosticCode.JSON_TOO_DEEP,
            DiagnosticCode.JSON_NUMBER_RANGE,
            DiagnosticCode.JSON_UTF8,
            DiagnosticCode.JSON_BOM,
            DiagnosticCode.JSON_EMPTY,
            DiagnosticCode.JSON_EMPTY);
  }

  @Test
  @Timeout(60)
  void provisionalAThousandFilesAndLongPathsLoad() throws IOException {
    // spec: R21 (longest real path is 139 characters), R8
    Map<String, String> tree = new LinkedHashMap<>();
    for (int i = 0; i < 3_000; i++) {
      tree.put("data/sonde/loot_table/dir" + (i % 17) + "/e" + i + ".json", "{\"i\":" + i + "}");
    }
    tree.put("data/sonde/loot_table/" + "d/".repeat(500) + "deep.json", "{}");
    Outcome<LoadedData> outcome = MiniData.load(worldPack("W", tree));
    assertThat(outcome.diagnostics()).isEmpty();
    assertThat(MiniData.elementIds(outcome.value().orElseThrow(), "minecraft:loot_table"))
        .hasSize(3_001);
  }

  @Test
  @Timeout(60)
  void provisionalAHugeTagCycleAcrossPacksTerminates() throws IOException {
    // spec: R37 (no unbounded loop, whatever the number of tags in the loop)
    int length = 1_500;
    Map<String, String> tree = new LinkedHashMap<>();
    for (int i = 0; i < length; i++) {
      tree.put(
          "data/sonde/tags/block/c" + i + ".json",
          "{\"values\":[\"#sonde:c" + ((i + 1) % length) + "\"]}");
    }
    Outcome<LoadedData> outcome = MiniData.load(worldPack("W", tree));
    TagResolution block =
        outcome.value().orElseThrow().tags(Identifier.parse("block")).orElseThrow();
    assertThat(block.failed()).hasSize(length);
    assertThat(block.tags()).isEmpty();
  }

  @Test
  @Timeout(60)
  void provisionalPacksWithNothingInThemLoadToEmptyData() throws IOException {
    // spec: R28
    Outcome<LoadedData> outcome = MiniData.load(worldPack("A", Map.of()), worldPack("B", Map.of()));
    assertInvariants(outcome, "empty packs");
    assertThat(outcome.diagnostics()).isEmpty();
  }
}
