package dev.shoko.minecraft.data.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackFiles;
import dev.shoko.minecraft.data.pack.PackOrigin;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.data.testing.MiniData;
import dev.shoko.minecraft.data.testing.Packs;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;

// spec: data-datapack-loading R47 (filter.block, regular expressions written by pack authors) and
// the loader contract: errors in the data are diagnostics, never exceptions, and a third-party pack
// cannot freeze or crash the load. The tests run in a separate thread because a regular expression
// match cannot be interrupted.
class FilterRegexHuntTest {

  private static final String HOSTILE = "file/hostile";

  private static String mcmeta(String namespacePattern, String pathPattern) {
    return "{\"pack\":{\"description\":\"t\",\"min_format\":121,\"max_format\":121},"
        + "\"filter\":{\"block\":[{\"namespace\":\""
        + namespacePattern
        + "\",\"path\":\""
        + pathPattern
        + "\"}]}}";
  }

  private static Outcome<Pack> open(String mcmeta) throws IOException {
    return Pack.open(
        HOSTILE,
        PackOrigin.WORLD,
        PackType.DATA,
        PackFiles.inMemory(Packs.bytes(Packs.tree("pack.mcmeta", mcmeta))));
  }

  /** Opens the hostile pack, loads it above the low one, and returns every diagnostic met. */
  private static List<Diagnostic> openAndLoad(String mcmeta, Pack low) throws IOException {
    Outcome<Pack> opened = open(mcmeta);
    List<Diagnostic> diagnostics = new ArrayList<>(opened.diagnostics());
    List<Pack> stack = new ArrayList<>(List.of(low));
    opened.value().ifPresent(stack::add);
    Outcome<LoadedData> loaded = MiniData.load(stack.toArray(Pack[]::new));
    assertThat(loaded.value()).isPresent();
    diagnostics.addAll(loaded.diagnostics());
    return diagnostics;
  }

  private static void assertRefusedWithADiagnostic(List<Diagnostic> diagnostics) {
    assertThat(diagnostics)
        .as("the pack with an unmanageable filter is refused with a diagnostic")
        .anyMatch(d -> d.origin().pack().equals(HOSTILE));
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
  void aBacktrackingPathExpressionDoesNotFreezeTheLoad() throws IOException {
    Pack low = Packs.world("file/low", "data/sonde/loot_table/" + "a".repeat(40) + ".json", "{}");
    List<Diagnostic> diagnostics = openAndLoad(mcmeta(".*", "(.*a){25}b"), low);
    assertRefusedWithADiagnostic(diagnostics);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
  void aBacktrackingNamespaceExpressionDoesNotFreezeTheLoad() throws IOException {
    Pack low = Packs.world("file/low", "data/" + "a".repeat(40) + "/loot_table/x.json", "{}");
    // Some JDK versions match this shape in polynomial time: the load must finish, whatever the
    // implementation decides about the pack.
    assertThat(openAndLoad(mcmeta("(a+)+b", ".*"), low)).isNotNull();
  }

  // A pattern that the implementation may accept: then it must hide the file, and a very long name
  // must not exhaust the stack of the matcher. Otherwise the pack is refused with a diagnostic.
  @Test
  @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
  void aLongNameAgainstARepeatedAlternativeNeitherOverflowsTheStackNorHidesWrongly()
      throws IOException {
    String name = "a".repeat(20_000);
    String path = "data/sonde/loot_table/" + name + ".json";
    Pack low = Packs.world("file/low", path, "{}", "data/sonde/loot_table/keep.json", "{}");
    String hostile = mcmeta(".*", "loot_table/(a|b)*[.]json");
    Outcome<Pack> opened = open(hostile);
    List<Diagnostic> diagnostics = openAndLoad(hostile, low);
    if (opened.diagnostics().isEmpty() && opened.value().isPresent()) {
      Pack pack = opened.value().get();
      LoadedData data = MiniData.load(low, pack).value().orElseThrow();
      assertThat(MiniData.elementIds(data, "minecraft:loot_table")).containsExactly("sonde:keep");
    } else {
      assertRefusedWithADiagnostic(diagnostics);
    }
  }
}
