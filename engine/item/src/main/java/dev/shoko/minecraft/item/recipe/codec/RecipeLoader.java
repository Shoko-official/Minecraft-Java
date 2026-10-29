package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.registry.ItemCatalog;

/**
 * Turns the {@code recipe} elements found by the datapack loader into a {@link
 * dev.shoko.minecraft.item.recipe.RecipeSet} (RC-R1, RC-R5, RC-R55). The winner of a path between
 * packs is already chosen by {@code data}; this decodes the winners only.
 */
public final class RecipeLoader {

  private RecipeLoader() {}

  /**
   * Decodes every recipe, never stopping at the first problem (RC-R13).
   *
   * @param recipes the elements of the recipe registry in the loaded data
   * @param access the loaded registries and tags
   * @param items the loaded items
   * @return the recipes that decoded and the problems of the others
   */
  public static RecipeLoadResult load(
      ElementRegistryData recipes, RegistryAccess access, ItemCatalog items) {
    return Skeleton.throwNotImplemented();
  }
}
