package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.item.recipe.TransmuteRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;
import java.util.Optional;

/**
 * Recognition of a transmutation (RC-R49f): one stack of the {@code input} and as many stacks of
 * the {@code material} as the range allows, nothing else. The result takes the item of the file and
 * the modifications of the input stack; a derived result takes the item of the input, and {@code
 * add_material_count_to_result} adds the materials to its count (cloning of maps).
 */
final class TransmuteMatcher {

  private TransmuteMatcher() {}

  static Optional<ItemStack> match(TransmuteRecipe recipe, CraftingGrid grid) {
    List<ItemStack> cells = GridStacks.nonEmpty(grid);
    int materials = cells.size() - 1;
    if (materials < recipe.materialCount().min() || materials > recipe.materialCount().max()) {
      return Optional.empty();
    }
    for (int i = 0; i < cells.size(); i++) {
      boolean input = recipe.input().matches(cells.get(i));
      if (input && GridStacks.allMatch(GridStacks.without(cells, i), recipe.material())) {
        return Optional.of(result(recipe, cells.get(i), materials));
      }
    }
    return Optional.empty();
  }

  private static ItemStack result(TransmuteRecipe recipe, ItemStack input, int materials) {
    int added = recipe.addMaterialCountToResult() ? materials : 0;
    ItemStack model =
        switch (recipe.result()) {
          case RecipeResult.Fixed fixed -> fixed.stack();
          case RecipeResult.Derived _ -> input.withCount(1);
        };
    ComponentPatch overrides =
        recipe.result() instanceof RecipeResult.Fixed ? model.patch() : ComponentPatch.empty();
    int count = Math.min(model.count() + added, model.maxStackSize());
    return GridStacks.carry(input, model.type(), count, overrides);
  }
}
