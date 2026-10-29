package dev.shoko.minecraft.item.recipe.codec;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.recipe.BrewingRecipe;
import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: RC-R1, RC-R5, RC-R13, RC-R55 (loader side): recipe elements found by the datapack loader
// become a RecipeSet; a recipe that fails is reported and left out, the others still load.
class RecipeLoaderTest {

  private static final String SHAPED =
      "{'type':'minecraft:crafting_shaped','key':{'#':'#minecraft:planks'},"
          + "'pattern':['##','##'],'result':{'id':'minecraft:chest'}}";
  private static final String SMELTING =
      "{'type':'minecraft:smelting','category':'food','cookingtime':200,'experience':0.35,"
          + "'ingredient':'minecraft:potato','result':{'id':'minecraft:baked_potato'}}";
  private static final String BREWING =
      "{'type':'minecraft:brewing','input':{'item':'minecraft:potion'},"
          + "'output':{'id':'minecraft:splash_potion'},'reagent':{'item':'minecraft:gunpowder'}}";
  private static final String UNKNOWN_TYPE = "{'type':'minecraft:crafting_nonsense'}";
  private static final String MISSING_TAG =
      "{'type':'minecraft:crafting_shapeless','ingredients':['#minecraft:ghost'],"
          + "'result':{'id':'minecraft:stone_slab'}}";

  private static final Map<String, List<String>> TAGS =
      Map.of("planks", List.of("oak_planks", "birch_planks"));

  private static RecipeLoadResult load(Map<String, String> recipes) {
    LoadedData data = MiniDatapack.load(recipes, TAGS);
    return RecipeLoader.load(
        MiniDatapack.recipes(data), data.registryAccess(), TestItems.catalog());
  }

  private static Identifier id(String path) {
    return Identifier.minecraft(path);
  }

  @Test
  void validRecipesLoadWithoutFindings() {
    // spec: RC-R1 (each file is a recipe of identifier namespace:path)
    Map<String, String> files = new LinkedHashMap<>();
    files.put("table", SHAPED);
    files.put("baked_potato", SMELTING);
    RecipeLoadResult result = load(files);
    assertThat(result.findings()).isEmpty();
    assertThat(result.recipes().size()).isEqualTo(2);
    assertThat(result.recipes().get(id("table"))).containsInstanceOf(ShapedRecipe.class);
    assertThat(result.recipes().get(id("baked_potato"))).containsInstanceOf(CookingRecipe.class);
  }

  @Test
  void aSubfolderIsPartOfTheRecipeIdentifier() {
    // spec: RC-R1 (brewing/ keeps its folder: minecraft:brewing/potion_to_splash)
    RecipeLoadResult result = load(Map.of("brewing/potion_to_splash", BREWING));
    assertThat(result.findings()).isEmpty();
    Recipe recipe = result.recipes().get(id("brewing/potion_to_splash")).orElseThrow();
    assertThat(recipe).isInstanceOf(BrewingRecipe.class);
    assertThat(recipe.id()).isEqualTo(id("brewing/potion_to_splash"));
    assertThat(result.recipes().get(id("potion_to_splash"))).isEmpty();
  }

  @Test
  void anEmptyRegistryGivesAnEmptySet() {
    RecipeLoadResult result = load(Map.of());
    assertThat(result.recipes().size()).isZero();
    assertThat(result.findings()).isEmpty();
  }

  @Test
  void provisionalABrokenRecipeIsReportedAndTheOthersStillLoad() {
    // spec: RC-R13 (a recipe in error is ignored, the others load)
    Map<String, String> files = new LinkedHashMap<>();
    files.put("good", SMELTING);
    files.put("bad", UNKNOWN_TYPE);
    files.put("other", SHAPED);
    RecipeLoadResult result = load(files);
    assertThat(result.recipes().size()).isEqualTo(2);
    assertThat(result.recipes().get(id("bad"))).isEmpty();
    assertThat(result.recipes().get(id("good"))).isPresent();
    assertThat(result.recipes().get(id("other"))).isPresent();
    assertThat(result.findings())
        .singleElement()
        .satisfies(
            f -> {
              assertThat(f.recipe()).isEqualTo(id("bad"));
              assertThat(f.finding().code()).isEqualTo(FindingCode.UNKNOWN_RECIPE_TYPE);
            });
  }

  @Test
  void provisionalARecipeCitingATagThatIsNotLoadedIsIgnored() {
    // spec: RC-R13 (a missing tag is an error of that recipe only)
    Map<String, String> files = new LinkedHashMap<>();
    files.put("ghostly", MISSING_TAG);
    files.put("table", SHAPED);
    RecipeLoadResult result = load(files);
    assertThat(result.recipes().get(id("ghostly"))).isEmpty();
    assertThat(result.recipes().get(id("table"))).isPresent();
    assertThat(result.findings())
        .extracting(f -> f.finding().code())
        .containsExactly(FindingCode.UNRESOLVED_TAG);
  }

  @Test
  void provisionalFindingsFollowTheRecipeIdentifierOrder() {
    // spec: RecipeLoadResult Javadoc (recipe identifier order, then the order they were made)
    Map<String, String> files = new LinkedHashMap<>();
    files.put("zzz", UNKNOWN_TYPE);
    files.put("ok", SMELTING);
    files.put("aaa", MISSING_TAG);
    files.put("mmm", UNKNOWN_TYPE);
    RecipeLoadResult result = load(files);
    assertThat(result.findings())
        .extracting(f -> f.recipe().path())
        .isSortedAccordingTo(String::compareTo);
    assertThat(result.findings().stream().map(f -> f.recipe().path()).distinct().toList())
        .containsExactly("aaa", "mmm", "zzz");
  }

  @Test
  void aRecipeWithAnErrorFindingIsNeverInTheSet() {
    // spec: RecipeLoadResult Javadoc
    Map<String, String> files = new LinkedHashMap<>();
    files.put("bad_one", UNKNOWN_TYPE);
    files.put("bad_two", MISSING_TAG);
    files.put("fine", SMELTING);
    RecipeLoadResult result = load(files);
    for (RecipeLoadFinding finding : result.findings()) {
      if (finding.finding().code().severity() == Severity.ERROR) {
        assertThat(result.recipes().get(finding.recipe())).isEmpty();
      }
    }
    assertThat(result.recipes().all()).extracting(Recipe::id).containsExactly(id("fine"));
  }

  @Test
  void provisionalTheSetIsOrderedByAscendingIdentifier() {
    // spec: RC-R54 (H-ord-a), RC-R55
    Map<String, String> files = new LinkedHashMap<>();
    files.put("zeta", SMELTING);
    files.put("alpha", SHAPED);
    files.put("brewing/x", BREWING);
    RecipeLoadResult result = load(files);
    assertThat(result.recipes().all())
        .extracting(r -> r.id().toString())
        .containsExactly("minecraft:alpha", "minecraft:brewing/x", "minecraft:zeta");
  }
}
