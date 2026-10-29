package dev.shoko.minecraft.data.pack;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.data.testing.Expect;
import dev.shoko.minecraft.data.testing.Jx;
import dev.shoko.minecraft.data.testing.Packs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R34 (layers), R40 (priority), R41 (replacement), R47 (filter.block),
// R48 (overlays); PackStack and PackFileRef
class PackStackTest {

  private static final String DIRT = "data/minecraft/loot_table/blocks/dirt.json";
  private static final String FORMAT_121 = "\"min_format\":121,\"max_format\":121";

  private static String text(PackFileRef ref) throws IOException {
    return new String(ref.read(), StandardCharsets.UTF_8);
  }

  private static List<String> packsOf(List<PackFileRef> refs) {
    return refs.stream().map(PackFileRef::pack).toList();
  }

  private static Pack plain(String id, String... files) {
    return Packs.implicit(id, files);
  }

  @Test
  void provisionalPacksAreReturnedLowestFirst() {
    // spec: R40
    Pack a = plain("a");
    Pack b = plain("b");
    Pack c = plain("c");
    assertThat(Packs.stack(a, b, c).packs()).extracting(Pack::id).containsExactly("a", "b", "c");
  }

  @Test
  void twoPacksWithTheSameIdentifierAreRefused() {
    // spec: PackStack.of Javadoc
    Expect.illegalArgument(() -> Packs.stack(plain("a"), plain("a")));
  }

  @Test
  void aPackOfAnotherKindIsRefused() {
    // spec: PackStack.of Javadoc
    Pack resource =
        Pack.implicit("r", PackOrigin.WORLD, PackType.RESOURCE, Packs.inMemory("assets/x", "1"));
    Expect.illegalArgument(() -> PackStack.of(PackType.DATA, List.of(plain("a"), resource)));
  }

  @Test
  void laterChangesToTheGivenListDoNotAffectTheStack() {
    List<Pack> list = new ArrayList<>(List.of(plain("a")));
    PackStack stack = PackStack.of(PackType.DATA, list);
    list.add(plain("b"));
    assertThat(stack.packs()).hasSize(1);
  }

  @Test
  void pathsAreTheDistinctUnionSortedByCodePoint() throws IOException {
    // spec: PackStack.paths Javadoc
    PackStack stack =
        Packs.stack(
            plain("a", "data/x/b.json", "1", "data/x/a.json", "1"),
            plain("b", "data/x/b.json", "2", "data/x/c.json", "2", "data/w/z.json", "2"));
    assertThat(stack.paths())
        .containsExactly("data/w/z.json", "data/x/a.json", "data/x/b.json", "data/x/c.json");
  }

  @Test
  void provisionalTheHighestPriorityFileWinsInFull() throws IOException {
    // spec: R41 (no merge between packs), P4
    PackStack stack =
        Packs.stack(
            plain("vanilla", DIRT, "{\"a\":1,\"b\":2}"),
            plain("A", DIRT, "diamond"),
            plain("B", DIRT, "{\"b\":3}"));
    PackFileRef winner = stack.winner(DIRT).orElseThrow();
    assertThat(winner.pack()).isEqualTo("B");
    assertThat(text(winner)).isEqualTo("{\"b\":3}");
    assertThat(winner.path()).isEqualTo(DIRT);
    assertThat(winner.origin()).isEqualTo(new FileOrigin("B", DIRT));
  }

  @Test
  void provisionalLayersListEveryVersionFromTheLowestPriority() throws IOException {
    // spec: R34 (tags merge across packs), PackStack.layers Javadoc
    PackStack stack =
        Packs.stack(
            plain("a", DIRT, "1"), plain("b", "data/x/other.json", "x"), plain("c", DIRT, "3"));
    List<PackFileRef> layers = stack.layers(DIRT);
    assertThat(packsOf(layers)).containsExactly("a", "c");
    assertThat(text(layers.get(0))).isEqualTo("1");
    assertThat(text(layers.get(1))).isEqualTo("3");
  }

  @Test
  void anUnknownPathHasNoLayerAndNoWinner() throws IOException {
    PackStack stack = Packs.stack(plain("a", DIRT, "1"));
    assertThat(stack.layers("data/none.json")).isEmpty();
    assertThat(stack.winner("data/none.json")).isEmpty();
  }

  @Test
  void anEmptyStackHasNothing() throws IOException {
    PackStack stack = Packs.stack();
    assertThat(stack.paths()).isEmpty();
    assertThat(stack.winner(DIRT)).isEmpty();
  }

  @Test
  void provisionalABlockFilterHidesMatchingFilesOfLowerPriorityPacks() throws IOException {
    // spec: R47, P15
    Pack vanilla = plain("vanilla", DIRT, "vanilla");
    Pack lower = Packs.world("A", DIRT, "A");
    Pack filter =
        Packs.withMeta(
            "F",
            "{\"pack\":{"
                + FORMAT_121
                + "},\"filter\":{\"block\":[{\"namespace\":\"minecraft\","
                + "\"path\":\"loot_table/blocks/dirt\\\\.json\"}]}}");
    PackStack stack = Packs.stack(vanilla, lower, filter);
    assertThat(stack.layers(DIRT)).isEmpty();
    assertThat(stack.winner(DIRT)).isEmpty();
    assertThat(stack.paths()).doesNotContain(DIRT);
  }

  @Test
  void provisionalTheFilterDoesNotActOnTheFilteringPackNorOnHigherPacks() throws IOException {
    // spec: R47 ("le filtre n'agit pas sur le pack qui le porte")
    Pack vanilla = plain("vanilla", DIRT, "vanilla");
    Pack filtering =
        Packs.withMeta(
            "F",
            "{\"pack\":{" + FORMAT_121 + "},\"filter\":{\"block\":[{\"path\":\"loot_table/.*\"}]}}",
            DIRT,
            "F");
    Pack higher = Packs.world("H", DIRT, "H");
    PackStack stack = Packs.stack(vanilla, filtering, higher);
    assertThat(packsOf(stack.layers(DIRT))).containsExactly("F", "H");
    assertThat(stack.winner(DIRT).orElseThrow().pack()).isEqualTo("H");
  }

  @Test
  void provisionalTheFilterOnlyHidesWhatBothExpressionsMatch() throws IOException {
    // spec: R47
    String recipe = "data/minecraft/recipe/a.json";
    String other = "data/other/loot_table/blocks/dirt.json";
    Pack lower = plain("vanilla", DIRT, "d", recipe, "r", other, "o");
    Pack filtering =
        Packs.withMeta(
            "F",
            "{\"pack\":{"
                + FORMAT_121
                + "},\"filter\":{\"block\":[{\"namespace\":\"minecraft\","
                + "\"path\":\"loot_table/.*\"}]}}");
    PackStack stack = Packs.stack(lower, filtering);
    assertThat(stack.winner(DIRT)).isEmpty();
    assertThat(stack.winner(recipe)).isPresent();
    assertThat(stack.winner(other)).isPresent();
  }

  @Test
  void provisionalAPartialExpressionDoesNotMatch() throws IOException {
    // spec: R47 ("correspondance complete"), P15 variants without extension
    Pack lower = plain("vanilla", DIRT, "d");
    Pack filtering =
        Packs.withMeta(
            "F",
            "{\"pack\":{"
                + FORMAT_121
                + "},\"filter\":{\"block\":[{\"namespace\":\"minecraft\","
                + "\"path\":\"loot_table/blocks/dirt\"}]}}");
    assertThat(Packs.stack(lower, filtering).winner(DIRT)).isPresent();
  }

  @Test
  void provisionalAnOverlayOfTheTargetVersionReplacesTheFileOfTheSamePath() throws IOException {
    // spec: R48, P17
    String path = "data/sonde/loot_table/o.json";
    Pack pack =
        Packs.withMeta(
            "O",
            "{\"pack\":{"
                + FORMAT_121
                + "},\"overlays\":{\"entries\":["
                + "{\"directory\":\"ov1\",\"min_format\":121,\"max_format\":121},"
                + "{\"directory\":\"ov2\",\"min_format\":122,\"max_format\":122}]}}",
            path,
            "base",
            "ov1/" + path,
            "emerald",
            "ov2/" + path,
            "lapis");
    PackStack stack = Packs.stack(pack);
    PackFileRef winner = stack.winner(path).orElseThrow();
    assertThat(text(winner)).isEqualTo("emerald");
    assertThat(winner.path()).isEqualTo(path);
    assertThat(winner.origin()).isEqualTo(new FileOrigin("O", "ov1/" + path));
    assertThat(stack.layers(path)).hasSize(1);
  }

  @Test
  void provisionalALaterOverlayEntryWins() throws IOException {
    // spec: R48 (entries apply in list order, a later one wins), P17 variant with ov3
    String path = "data/sonde/loot_table/o.json";
    Pack pack =
        Packs.withMeta(
            "O",
            "{\"pack\":{"
                + FORMAT_121
                + "},\"overlays\":{\"entries\":["
                + "{\"directory\":\"ov1\",\"min_format\":121,\"max_format\":121},"
                + "{\"directory\":\"ov3\",\"min_format\":121,\"max_format\":121}]}}",
            path,
            "base",
            "ov1/" + path,
            "emerald",
            "ov3/" + path,
            "coal");
    assertThat(text(Packs.stack(pack).winner(path).orElseThrow())).isEqualTo("coal");
  }

  @Test
  void provisionalAnOverlayCanAddAFileTheBaseDoesNotHave() throws IOException {
    // spec: R48 (the overlay directory is a tree of the same shape as the root)
    Pack pack =
        Packs.withMeta(
            "O",
            "{\"pack\":{"
                + FORMAT_121
                + "},\"overlays\":{\"entries\":["
                + "{\"directory\":\"ov\",\"min_format\":121,\"max_format\":121}]}}",
            "ov/data/x/new.json",
            "new");
    PackFileRef ref = Packs.stack(pack).winner("data/x/new.json").orElseThrow();
    assertThat(text(ref)).isEqualTo("new");
    assertThat(ref.origin().path()).isEqualTo("ov/data/x/new.json");
  }

  @Test
  void provisionalAnOverlayReplacesOnlyFilesOfItsOwnPack() throws IOException {
    // spec: R48 then R41: overlays apply inside each pack, then the packs are stacked
    String path = "data/x/a.json";
    Pack low =
        Packs.withMeta(
            "low",
            "{\"pack\":{"
                + FORMAT_121
                + "},\"overlays\":{\"entries\":["
                + "{\"directory\":\"ov\",\"min_format\":121,\"max_format\":121}]}}",
            path,
            "low-base",
            "ov/" + path,
            "low-overlay");
    Pack high = Packs.world("high", path, "high-base");
    PackStack stack = Packs.stack(low, high);
    assertThat(packsOf(stack.layers(path))).containsExactly("low", "high");
    assertThat(text(stack.layers(path).get(0))).isEqualTo("low-overlay");
    assertThat(text(stack.winner(path).orElseThrow())).isEqualTo("high-base");
  }

  @Test
  void provisionalPackMetadataOfAnOverlayDirectoryIsIgnored() throws IOException {
    // spec: R48 ("pack.mcmeta et pack.png d'un dossier d'overlay sont ignores")
    Pack pack =
        Packs.withMeta(
            "O",
            "{\"pack\":{"
                + FORMAT_121
                + "},\"overlays\":{\"entries\":["
                + "{\"directory\":\"ov\",\"min_format\":121,\"max_format\":121}]}}",
            "ov/pack.mcmeta",
            "{\"overlay\":true}",
            "ov/pack.png",
            "png");
    PackStack stack = Packs.stack(pack);
    assertThat(stack.layers("pack.mcmeta"))
        .allSatisfy(ref -> assertThat(ref.origin().path()).isEqualTo("pack.mcmeta"));
    assertThat(stack.layers("pack.png")).isEmpty();
  }

  @Test
  void aPackFileRefReadsTheBytesOfItsFile() throws IOException {
    PackStack stack = Packs.stack(plain("a", DIRT, "\351"));
    assertThat(stack.winner(DIRT).orElseThrow().read()).isEqualTo(Jx.utf8("\351"));
  }

  @Test
  void identicalInputGivesIdenticalLayersWhateverTheFileCreationOrder() throws IOException {
    // spec: determinism
    PackStack one = Packs.stack(plain("a", "data/x/b.json", "1", "data/x/a.json", "2"));
    PackStack two = Packs.stack(plain("a", "data/x/a.json", "2", "data/x/b.json", "1"));
    assertThat(one.paths()).isEqualTo(two.paths());
  }
}
