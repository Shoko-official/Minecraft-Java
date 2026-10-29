package dev.shoko.minecraft.item.recipe.match;

import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.cooking;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.set;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.shaped;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.shapeless;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Grids;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: item-recipes RC-R16 to RC-R22, RC-R54 (adversarial): fixed seeds, reference models written
// from the rules (offsets, left-right mirror, complete pairing), and no failure mode but a boolean.
class MatchAttackTest {

  private static final List<String> POOL =
      List.of("oak_planks", "birch_planks", "stick", "stone", "diamond", "dirt");

  private record ShapedDef(String id, List<String> rows, Map<Character, List<String>> allowed) {

    int width() {
      return rows.getFirst().length();
    }

    int height() {
      return rows.size();
    }

    ShapedRecipe recipe() {
      Map<Character, Ingredient> key = new HashMap<>();
      allowed.forEach((c, items) -> key.put(c, Recipes.items(items.toArray(new String[0]))));
      return shaped(id, TestItems.stack("chest"), key, rows.toArray(new String[0]));
    }
  }

  private record ShapelessDef(String id, List<List<String>> ingredients) {

    ShapelessRecipe recipe() {
      Ingredient[] all = new Ingredient[ingredients.size()];
      for (int i = 0; i < all.length; i++) {
        all[i] = Recipes.items(ingredients.get(i).toArray(new String[0]));
      }
      return shapeless(id, TestItems.stack("chest"), all);
    }
  }

  private static List<ShapedDef> shapedDefs() {
    List<String> planks = List.of("oak_planks", "birch_planks");
    return List.of(
        new ShapedDef("shaped_square", List.of("##", "##"), Map.of('#', planks)),
        new ShapedDef("shaped_line", List.of("###"), Map.of('#', List.of("stick"))),
        new ShapedDef("shaped_ell", List.of("##", "# "), Map.of('#', List.of("stone"))),
        new ShapedDef("shaped_boat", List.of("# #", "###"), Map.of('#', List.of("oak_planks"))),
        new ShapedDef("shaped_single", List.of("#"), Map.of('#', List.of("diamond"))),
        new ShapedDef(
            "shaped_pair",
            List.of("S", "D"),
            Map.of('S', List.of("stick"), 'D', List.of("diamond"))),
        new ShapedDef(
            "shaped_skew", List.of("S ", "DD"), Map.of('S', List.of("stick"), 'D', planks)));
  }

  private static List<ShapelessDef> shapelessDefs() {
    return List.of(
        new ShapelessDef(
            "shapeless_mix",
            List.of(
                List.of("oak_planks"),
                List.of("oak_planks", "birch_planks"),
                List.of("stick"),
                List.of("stone"))),
        new ShapelessDef(
            "shapeless_sticks", List.of(List.of("stick"), List.of("stick"), List.of("stone"))),
        new ShapelessDef("shapeless_diamond", List.of(List.of("diamond"))));
  }

  private static String item(String cell) {
    int colon = cell.indexOf(':');
    return colon < 0 ? cell : cell.substring(0, colon);
  }

  private static String withCount(Random random, String item) {
    return random.nextInt(4) == 0 ? item + ":" + (2 + random.nextInt(3)) : item;
  }

  private static CraftingGrid gridOf(int width, int height, List<String> cells) {
    return Grids.of(width, height, cells.toArray(new String[0]));
  }

  /** The rules RC-R16 and RC-R17 written as a search over offsets and the left-right mirror. */
  private static boolean shapedModel(ShapedDef def, int gw, int gh, List<String> cells) {
    for (boolean mirror : new boolean[] {false, true}) {
      for (int oy = 0; oy + def.height() <= gh; oy++) {
        for (int ox = 0; ox + def.width() <= gw; ox++) {
          if (shapedFits(def, gw, gh, cells, new Placement(ox, oy, mirror))) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private record Placement(int ox, int oy, boolean mirror) {}

  private static boolean shapedFits(
      ShapedDef def, int gw, int gh, List<String> cells, Placement placement) {
    int ox = placement.ox();
    int oy = placement.oy();
    boolean mirror = placement.mirror();
    for (int y = 0; y < gh; y++) {
      for (int x = 0; x < gw; x++) {
        String cell = item(cells.get(x + y * gw));
        boolean inside = x >= ox && x < ox + def.width() && y >= oy && y < oy + def.height();
        char expected = ' ';
        if (inside) {
          int px = mirror ? def.width() - 1 - (x - ox) : x - ox;
          expected = def.rows().get(y - oy).charAt(px);
        }
        boolean ok =
            expected == ' ' ? cell.equals("-") : def.allowed().get(expected).contains(cell);
        if (!ok) {
          return false;
        }
      }
    }
    return true;
  }

  /** RC-R19 and RC-R20: a complete one-to-one pairing between stacks and ingredients. */
  private static boolean shapelessModel(ShapelessDef def, List<String> cells) {
    List<String> stacks =
        cells.stream().map(MatchAttackTest::item).filter(c -> !c.equals("-")).toList();
    return stacks.size() == def.ingredients().size()
        && pair(def.ingredients(), stacks, 0, new boolean[stacks.size()]);
  }

  private static boolean pair(
      List<List<String>> ingredients, List<String> stacks, int index, boolean[] used) {
    if (index == ingredients.size()) {
      return true;
    }
    for (int s = 0; s < stacks.size(); s++) {
      if (!used[s] && ingredients.get(index).contains(stacks.get(s))) {
        used[s] = true;
        if (pair(ingredients, stacks, index + 1, used)) {
          return true;
        }
        used[s] = false;
      }
    }
    return false;
  }

  private static List<String> shapedGrid(Random random, ShapedDef def, int gw, int gh) {
    List<String> cells = new ArrayList<>(Collections.nCopies(gw * gh, "-"));
    if (def.width() <= gw && def.height() <= gh && random.nextInt(4) != 0) {
      int ox = random.nextInt(gw - def.width() + 1);
      int oy = random.nextInt(gh - def.height() + 1);
      boolean mirror = random.nextBoolean();
      for (int py = 0; py < def.height(); py++) {
        for (int px = 0; px < def.width(); px++) {
          char c = def.rows().get(py).charAt(px);
          if (c != ' ') {
            List<String> options = def.allowed().get(c);
            int x = ox + (mirror ? def.width() - 1 - px : px);
            cells.set(
                x + (oy + py) * gw, withCount(random, options.get(random.nextInt(options.size()))));
          }
        }
      }
      if (random.nextInt(3) == 0) {
        cells.set(
            random.nextInt(cells.size()),
            random.nextBoolean() ? "-" : POOL.get(random.nextInt(POOL.size())));
      }
    } else {
      for (int i = 0; i < cells.size(); i++) {
        if (random.nextInt(3) == 0) {
          cells.set(i, withCount(random, POOL.get(random.nextInt(POOL.size()))));
        }
      }
    }
    return cells;
  }

  private static List<String> shapelessGrid(Random random, ShapelessDef def, int capacity) {
    List<String> cells = new ArrayList<>(Collections.nCopies(capacity, "-"));
    List<Integer> positions = new ArrayList<>();
    for (int i = 0; i < capacity; i++) {
      positions.add(i);
    }
    Collections.shuffle(positions, random);
    int placed = 0;
    for (List<String> options : def.ingredients()) {
      if (placed < capacity) {
        cells.set(
            positions.get(placed++),
            withCount(random, options.get(random.nextInt(options.size()))));
      }
    }
    if (random.nextBoolean()) {
      int target = random.nextInt(capacity);
      cells.set(target, random.nextInt(3) == 0 ? "-" : POOL.get(random.nextInt(POOL.size())));
    }
    return cells;
  }

  @Test
  @Timeout(60)
  void provisionalShapedMatcherAgreesWithTheReferenceModel() {
    // spec: RC-R16, RC-R17 (offsets, left-right mirror, empty cells outside the pattern)
    Random random = new Random(0x5AA9EDL);
    List<ShapedDef> defs = shapedDefs();
    Map<String, ShapedRecipe> recipes = new HashMap<>();
    defs.forEach(def -> recipes.put(def.id(), def.recipe()));
    int positives = 0;
    int negatives = 0;
    for (int i = 0; i < 3000; i++) {
      int gw = 2 + random.nextInt(2);
      int gh = 2 + random.nextInt(2);
      List<String> cells = shapedGrid(random, defs.get(random.nextInt(defs.size())), gw, gh);
      CraftingGrid grid = gridOf(gw, gh, cells);
      for (ShapedDef def : defs) {
        boolean expected = shapedModel(def, gw, gh, cells);
        assertThat(ShapedMatcher.matches(recipes.get(def.id()), grid))
            .as("%s on %dx%d %s", def.id(), gw, gh, cells)
            .isEqualTo(expected);
        positives += expected ? 1 : 0;
        negatives += expected ? 0 : 1;
      }
    }
    assertThat(positives).isGreaterThan(100);
    assertThat(negatives).isGreaterThan(100);
  }

  @Test
  @Timeout(60)
  void provisionalShapelessMatcherAgreesWithTheModelAndIgnoresPermutations() {
    // spec: RC-R19, RC-R20 (complete pairing, independent of order and position)
    Random random = new Random(0x51BE55L);
    List<ShapelessDef> defs = shapelessDefs();
    Map<String, ShapelessRecipe> recipes = new HashMap<>();
    defs.forEach(def -> recipes.put(def.id(), def.recipe()));
    int positives = 0;
    for (int i = 0; i < 3000; i++) {
      int side = 2 + random.nextInt(2);
      List<String> cells =
          shapelessGrid(random, defs.get(random.nextInt(defs.size())), side * side);
      List<String> shuffled = new ArrayList<>(cells);
      Collections.shuffle(shuffled, random);
      for (ShapelessDef def : defs) {
        boolean expected = shapelessModel(def, cells);
        ShapelessRecipe recipe = recipes.get(def.id());
        assertThat(ShapelessMatcher.matches(recipe, gridOf(side, side, cells)))
            .as("%s on %s", def.id(), cells)
            .isEqualTo(expected);
        assertThat(ShapelessMatcher.matches(recipe, gridOf(side, side, shuffled)))
            .as("%s on shuffled %s", def.id(), shuffled)
            .isEqualTo(expected);
        positives += expected ? 1 : 0;
      }
    }
    assertThat(positives).isGreaterThan(100);
  }

  @Test
  @Timeout(60)
  void provisionalRecognizerAgreesWithTheModelsAndNeverThrows() {
    // spec: RC-R54 (H-ord-a), skeleton Javadoc of CraftingRecognizer (recognize is the head of
    // allMatches, cooking recipes are not candidates)
    Random random = new Random(0xFACADEL);
    List<ShapedDef> shapedDefs = shapedDefs();
    List<ShapelessDef> shapelessDefs = shapelessDefs();
    List<Recipe> recipes = new ArrayList<>();
    shapedDefs.forEach(def -> recipes.add(def.recipe()));
    shapelessDefs.forEach(def -> recipes.add(def.recipe()));
    recipes.add(
        cooking(
            "cook_stone", RecipeType.SMELTING, Recipes.items("stone"), TestItems.stack("dirt")));
    RecipeSet set = set(recipes.toArray(new Recipe[0]));
    CraftingRecognizer recognizer = CraftingRecognizer.of(set, TestItems.catalog());
    int found = 0;
    for (int i = 0; i < 2000; i++) {
      int side = 2 + random.nextInt(2);
      List<String> cells =
          random.nextBoolean()
              ? shapedGrid(random, shapedDefs.get(random.nextInt(shapedDefs.size())), side, side)
              : shapelessGrid(
                  random, shapelessDefs.get(random.nextInt(shapelessDefs.size())), side * side);
      List<String> expectedIds = new ArrayList<>();
      shapedDefs.stream()
          .filter(def -> shapedModel(def, side, side, cells))
          .forEach(def -> expectedIds.add(def.id()));
      shapelessDefs.stream()
          .filter(def -> shapelessModel(def, cells))
          .forEach(def -> expectedIds.add(def.id()));
      Collections.sort(expectedIds);
      CraftingGrid grid = gridOf(side, side, cells);
      List<CraftingMatch> all = recognizer.allMatches(grid);
      assertThat(all.stream().map(m -> m.recipe().id().path()).toList())
          .as("%s", cells)
          .isEqualTo(expectedIds);
      assertThat(all).allSatisfy(m -> assertThat(m.result().isEmpty()).isFalse());
      assertThat(recognizer.recognize(grid)).isEqualTo(all.stream().findFirst());
      found += all.isEmpty() ? 0 : 1;
    }
    assertThat(found).isGreaterThan(100);
  }

  @Test
  @Timeout(60)
  void provisionalConsumptionRemovesOneUnitPerNonEmptyCell() {
    // spec: RC-R22 (one unit from every non empty cell, remainder from use_remainder)
    Random random = new Random(0xC0FFEEL);
    Map<String, String> remainders =
        Map.of("milk_bucket", "bucket", "mushroom_stew", "bowl", "potion", "glass_bottle");
    List<String> items =
        List.of("stone", "dirt", "stick", "coal", "milk_bucket", "mushroom_stew", "potion", "-");
    CraftingConsumer consumer = CraftingConsumer.of(TestItems.catalog());
    CraftingMatch match =
        new CraftingMatch(
            shapeless("dummy", TestItems.stack("chest"), Recipes.items("stone")),
            TestItems.stack("chest"));
    for (int i = 0; i < 1500; i++) {
      int side = 2 + random.nextInt(2);
      List<String> cells = new ArrayList<>();
      for (int c = 0; c < side * side; c++) {
        String item = items.get(random.nextInt(items.size()));
        boolean single = remainders.containsKey(item) || item.equals("-");
        cells.add(single ? item : item + ":" + (1 + random.nextInt(5)));
      }
      CraftingGrid grid = gridOf(side, side, cells);
      Consumption consumption = consumer.consume(grid, match);
      assertThat(consumption.grid()).hasSize(cells.size());
      assertThat(consumption.remainders()).hasSize(cells.size());
      int removed = 0;
      for (int c = 0; c < cells.size(); c++) {
        ItemStack before = grid.stacks().get(c);
        ItemStack after = consumption.grid().get(c);
        removed += before.count() - after.count();
        String remainder = Cells.text(consumption.remainders().get(c));
        String expectedRemainder =
            remainders.containsKey(item(cells.get(c)))
                ? remainders.get(item(cells.get(c))) + ":1"
                : "-";
        assertThat(remainder).as("remainder of %s", cells.get(c)).isEqualTo(expectedRemainder);
        assertThat(after.count()).isEqualTo(Math.max(0, before.count() - 1));
      }
      assertThat(removed).isEqualTo(cells.stream().filter(c -> !c.equals("-")).count());
    }
  }
}
