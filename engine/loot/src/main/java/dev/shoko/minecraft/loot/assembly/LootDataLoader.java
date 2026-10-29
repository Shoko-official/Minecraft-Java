package dev.shoko.minecraft.loot.assembly;

import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.registry.ItemCatalog;

/**
 * Decodes the registries owned by {@code loot} from the datapack loader's output: {@code
 * loot_table}, {@code predicate}, {@code context_int_provider}, {@code context_float_provider},
 * {@code enchantment} and {@code enchantment_provider}. The winner of a path between packs is
 * already chosen by {@code data}; this decodes the winners only.
 */
public final class LootDataLoader {

  private LootDataLoader() {}

  /**
   * Loads the module data, never stopping at the first problem.
   *
   * @param loaded the datapack loader's output
   * @param items the loaded items
   * @param recipes the loaded recipes
   * @return the data and the findings; a table whose named predicate or nested table does not exist
   *     is absent with an {@code UNRESOLVED_REFERENCE} finding (LT-R61b), a table that references
   *     itself is absent with {@code SELF_REFERENCE} (LT-R23)
   */
  public static LootLoadResult load(LoadedData loaded, ItemCatalog items, RecipeSet recipes) {
    return Skeleton.throwNotImplemented();
  }
}
