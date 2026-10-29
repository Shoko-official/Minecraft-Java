package dev.shoko.minecraft.item.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.testing.Recipes;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import org.junit.jupiter.api.Test;

// spec: RC-R2, RC-R54, RC-R55: the set of loaded recipes and its resolution order
class RecipeSetTest {

  private static final String SHAPELESS =
      "{'type':'minecraft:crafting_shapeless','ingredients':['minecraft:stone'],"
          + "'result':{'id':'minecraft:stone_slab'}}";
  private static final String CUTTING =
      "{'type':'minecraft:stonecutting','ingredient':'minecraft:stone',"
          + "'result':{'id':'minecraft:stone_stairs'}}";
  private static final String SMELTING =
      "{'type':'minecraft:smelting','cookingtime':200,'experience':0.1,"
          + "'ingredient':'minecraft:potato','result':{'id':'minecraft:baked_potato'}}";

  private static Recipe recipe(String path, String json) {
    return Recipes.recipe(Recipe.class, path, json);
  }

  private static SequencedMap<Identifier, Recipe> mapOf(Recipe... recipes) {
    SequencedMap<Identifier, Recipe> map = new LinkedHashMap<>();
    for (Recipe recipe : recipes) {
      map.put(recipe.id(), recipe);
    }
    return map;
  }

  private static List<Recipe> mixed() {
    return List.of(
        recipe("zeta", SHAPELESS),
        recipe("alpha", CUTTING),
        recipe("mid", SMELTING),
        recipe("brewing/x", SHAPELESS),
        recipe("beta", CUTTING));
  }

  @Test
  void aSetKnowsItsSizeAndFindsRecipesByIdentifier() {
    // spec: RC-R1 (identifier is namespace:path)
    List<Recipe> recipes = mixed();
    RecipeSet set = RecipeSet.of(mapOf(recipes.toArray(new Recipe[0])));
    assertThat(set.size()).isEqualTo(5);
    assertThat(set.get(Identifier.minecraft("mid"))).contains(recipes.get(2));
    assertThat(set.get(Identifier.minecraft("brewing/x"))).contains(recipes.get(3));
    assertThat(set.get(Identifier.minecraft("absent"))).isEmpty();
  }

  @Test
  void anEmptyMapGivesAnEmptySet() {
    RecipeSet set = RecipeSet.of(new LinkedHashMap<>());
    assertThat(set.size()).isZero();
    assertThat(set.all()).isEmpty();
    assertThat(set.ofType(RecipeType.CRAFTING)).isEmpty();
  }

  @Test
  void aKeyThatDiffersFromTheRecipeIdentifierIsRefused() {
    Recipe recipe = recipe("alpha", CUTTING);
    SequencedMap<Identifier, Recipe> map = new LinkedHashMap<>();
    map.put(Identifier.minecraft("other"), recipe);
    assertThatThrownBy(() -> RecipeSet.of(map)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void provisionalRecipesAreOrderedByAscendingIdentifierWhateverTheMapOrder() {
    // spec: RC-R54 (hypothesis H-ord-a: ascending identifier)
    List<Recipe> recipes = mixed();
    Comparator<Recipe> byId = Comparator.comparing(Recipe::id);
    List<Recipe> expected = recipes.stream().sorted(byId).toList();
    RecipeSet forward = RecipeSet.of(mapOf(recipes.toArray(new Recipe[0])));
    RecipeSet backward = RecipeSet.of(mapOf(recipes.reversed().toArray(new Recipe[0])));
    assertThat(forward.all()).containsExactlyElementsOf(expected);
    assertThat(backward.all()).containsExactlyElementsOf(expected);
  }

  @Test
  void provisionalOfTypeKeepsTheResolutionOrderOfTheMachine() {
    // spec: RC-R54, RC-R4
    RecipeSet set = RecipeSet.of(mapOf(mixed().toArray(new Recipe[0])));
    assertThat(set.ofType(RecipeType.CRAFTING))
        .extracting(r -> r.id().path())
        .containsExactly("brewing/x", "zeta");
    assertThat(set.ofType(RecipeType.STONECUTTING))
        .extracting(r -> r.id().path())
        .containsExactly("alpha", "beta");
    assertThat(set.ofType(RecipeType.SMELTING))
        .extracting(r -> r.id().path())
        .containsExactly("mid");
    assertThat(set.ofType(RecipeType.BREWING)).isEmpty();
  }

  @Test
  void theListsReturnedCannotBeModified() {
    RecipeSet set = RecipeSet.of(mapOf(mixed().toArray(new Recipe[0])));
    List<Recipe> all = set.all();
    List<Recipe> crafting = set.ofType(RecipeType.CRAFTING);
    Recipe extra = recipe("extra", CUTTING);
    assertThatThrownBy(() -> all.add(extra)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> crafting.add(extra)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void aSetIsNotAffectedByLaterChangesToTheMapItWasBuiltFrom() {
    // spec: RecipeSet is immutable once built
    SequencedMap<Identifier, Recipe> map = mapOf(mixed().toArray(new Recipe[0]));
    RecipeSet set = RecipeSet.of(map);
    map.clear();
    assertThat(set.size()).isEqualTo(5);
    assertThat(set.all()).hasSize(5);
  }

  @Test
  void provisionalTwoSetsBuiltFromTheSameRecipesInAnyOrderAreIndistinguishable() {
    // spec: RC-R55 (a replaced recipe keeps the place of the original: the place depends on the
    // identifier only)
    List<Recipe> recipes = mixed();
    RecipeSet first = RecipeSet.of(mapOf(recipes.toArray(new Recipe[0])));
    RecipeSet second = RecipeSet.of(mapOf(recipes.reversed().toArray(new Recipe[0])));
    assertThat(second.all()).isEqualTo(first.all());
    assertThat(second.ofType(RecipeType.CRAFTING)).isEqualTo(first.ofType(RecipeType.CRAFTING));
  }
}
