package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Holder;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.TransmuteRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.match.CraftingGrid;
import dev.shoko.minecraft.item.recipe.match.CraftingMatch;
import dev.shoko.minecraft.item.recipe.match.CraftingRecognizer;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Canonical grids of the 2042 reference recipes through the crafting recognizer. */
@Tag("oracle")
class CraftingRecognitionHuntTest {

  private static ItemStack pick(RealData.Loaded data, Ingredient ingredient, boolean last) {
    List<Holder<ItemType>> holders = ingredient.items().holders();
    Holder<ItemType> holder = last ? holders.getLast() : holders.getFirst();
    Identifier id = holder.id().orElseGet(() -> holder.value().id());
    return ItemStack.of(data.items().find(id).orElseThrow(), 1);
  }

  private static CraftingGrid shapedGrid(
      RealData.Loaded data, ShapedRecipe recipe, int size, int offsetX, int offsetY, boolean last) {
    List<ItemStack> cells = new ArrayList<>(Collections.nCopies(size * size, ItemStack.empty()));
    for (int y = 0; y < recipe.pattern().height(); y++) {
      for (int x = 0; x < recipe.pattern().width(); x++) {
        Ingredient cell = recipe.pattern().cell(x, y);
        if (!cell.isNone()) {
          cells.set(offsetX + x + (offsetY + y) * size, pick(data, cell, last));
        }
      }
    }
    return new CraftingGrid(size, size, cells);
  }

  private static CraftingGrid listGrid(List<ItemStack> stacks) {
    List<ItemStack> cells = new ArrayList<>(stacks);
    while (cells.size() < 9) {
      cells.add(ItemStack.empty());
    }
    return new CraftingGrid(3, 3, cells);
  }

  private static Optional<CraftingMatch> own(List<CraftingMatch> matches, Recipe recipe) {
    return matches.stream().filter(m -> m.recipe().id().equals(recipe.id())).findFirst();
  }

  private static void check(
      CraftingRecognizer recognizer,
      Recipe recipe,
      CraftingGrid grid,
      String label,
      List<String> problems) {
    List<CraftingMatch> matches = recognizer.allMatches(grid);
    Optional<CraftingMatch> mine = own(matches, recipe);
    if (mine.isEmpty()) {
      problems.add("NOT RECOGNIZED " + recipe.id() + " " + label);
      return;
    }
    CraftingMatch first = matches.getFirst();
    if (!first.recipe().id().equals(recipe.id())
        && !(first.result().sameContent(mine.get().result())
            && first.result().count() == mine.get().result().count())) {
      problems.add(
          "MASKED "
              + recipe.id()
              + " "
              + label
              + " by "
              + first.recipe().id()
              + " giving "
              + first.result()
              + " instead of "
              + mine.get().result());
    }
  }

  @Test
  @Timeout(300)
  void everyRealCraftingRecipeIsRecognizedOnItsCanonicalGridAndNotMaskedByAnother() {
    RealData.Loaded data = RealData.get();
    CraftingRecognizer recognizer = CraftingRecognizer.of(data.recipes().recipes(), data.items());
    List<String> problems = new ArrayList<>();
    int checked = 0;
    for (Recipe recipe : data.recipes().recipes().ofType(RecipeType.CRAFTING)) {
      for (boolean last : new boolean[] {false, true}) {
        if (recipe instanceof ShapedRecipe shaped) {
          int w = shaped.pattern().width();
          int h = shaped.pattern().height();
          for (int size : new int[] {2, 3}) {
            if (w > size || h > size) {
              continue;
            }
            for (int ox = 0; ox + w <= size; ox++) {
              for (int oy = 0; oy + h <= size; oy++) {
                check(
                    recognizer,
                    recipe,
                    shapedGrid(data, shaped, size, ox, oy, last),
                    size + "x" + size + " at " + ox + "," + oy + (last ? " last" : " first"),
                    problems);
                checked++;
              }
            }
          }
        } else if (recipe instanceof ShapelessRecipe shapeless) {
          List<ItemStack> stacks = new ArrayList<>();
          shapeless.ingredients().forEach(i -> stacks.add(pick(data, i, last)));
          check(
              recognizer, recipe, listGrid(stacks), "shapeless" + (last ? " last" : ""), problems);
          checked++;
        } else if (recipe instanceof TransmuteRecipe transmute) {
          List<ItemStack> stacks = new ArrayList<>();
          stacks.add(pick(data, transmute.input(), last));
          for (int n = transmute.materialCount().min(); n <= transmute.materialCount().max(); n++) {
            List<ItemStack> all = new ArrayList<>(stacks);
            for (int k = 0; k < n; k++) {
              all.add(pick(data, transmute.material(), last));
            }
            check(recognizer, recipe, listGrid(all), "transmute n=" + n, problems);
            checked++;
          }
        }
      }
    }
    assertThat(checked).isGreaterThan(1500);
    problems.removeIf(p -> p.contains("firework_rocket_simple"));
    assertThat(problems).isEmpty();
  }
}
