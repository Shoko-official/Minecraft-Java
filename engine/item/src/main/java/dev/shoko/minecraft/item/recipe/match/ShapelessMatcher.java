package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.Arrays;
import java.util.List;

/**
 * Recognition of a shapeless recipe (RC-R19, RC-R20, provisional level S): the non-empty stacks of
 * the grid and the ingredients pair one to one, in any order and position. When a stack suits
 * several ingredients the recipe matches if a complete pairing exists, so the answer does not
 * depend on the order of trial.
 */
public final class ShapelessMatcher {

  private ShapelessMatcher() {}

  /**
   * Tells whether a grid satisfies a shapeless recipe.
   *
   * @param recipe the recipe
   * @param grid the grid
   * @return {@code true} when recognized; false when the number of non-empty stacks differs from
   *     the number of ingredients
   */
  public static boolean matches(ShapelessRecipe recipe, CraftingGrid grid) {
    List<ItemStack> stacks = grid.stacks().stream().filter(stack -> !stack.isEmpty()).toList();
    List<Ingredient> ingredients = recipe.ingredients();
    if (stacks.size() != ingredients.size()) {
      return false;
    }
    boolean[][] suits = new boolean[ingredients.size()][stacks.size()];
    for (int i = 0; i < ingredients.size(); i++) {
      for (int s = 0; s < stacks.size(); s++) {
        suits[i][s] = ingredients.get(i).matches(stacks.get(s));
      }
    }
    return Pairing.complete(suits);
  }

  /** A perfect matching of ingredients to stacks by augmenting paths (Kuhn). */
  private static final class Pairing {

    private final boolean[][] suits;
    private final int[] stackOwner;

    private Pairing(boolean[][] suits) {
      this.suits = suits;
      this.stackOwner = new int[suits.length == 0 ? 0 : suits[0].length];
      Arrays.fill(stackOwner, -1);
    }

    static boolean complete(boolean[][] suits) {
      Pairing pairing = new Pairing(suits);
      for (int ingredient = 0; ingredient < suits.length; ingredient++) {
        if (!pairing.assign(ingredient, new boolean[pairing.stackOwner.length])) {
          return false;
        }
      }
      return true;
    }

    private boolean assign(int ingredient, boolean[] visited) {
      for (int stack = 0; stack < stackOwner.length; stack++) {
        if (suits[ingredient][stack] && !visited[stack]) {
          visited[stack] = true;
          if (stackOwner[stack] < 0 || assign(stackOwner[stack], visited)) {
            stackOwner[stack] = ingredient;
            return true;
          }
        }
      }
      return false;
    }
  }
}
