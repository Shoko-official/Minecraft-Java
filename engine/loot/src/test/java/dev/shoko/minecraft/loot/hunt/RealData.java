package dev.shoko.minecraft.loot.hunt;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.codec.RecipeLoader;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.loot.assembly.LootData;
import dev.shoko.minecraft.loot.assembly.LootDataLoader;
import dev.shoko.minecraft.loot.assembly.LootLoadResult;
import dev.shoko.minecraft.loot.testing.ReferenceCache;

/** Loads the extracted vanilla data once for the hunt tests. */
final class RealData {

  private static LootLoadResult result;
  private static ItemCatalog items;

  private RealData() {}

  static synchronized LootLoadResult result() {
    if (result == null) {
      LoadedData loaded = ReferenceCache.vanilla();
      items = ReferenceCache.items();
      RecipeSet recipes =
          RecipeLoader.load(
                  loaded.elementRegistry(Identifier.minecraft("recipe")).orElseThrow(),
                  loaded.registryAccess(),
                  items)
              .recipes();
      result = LootDataLoader.load(loaded, items, recipes);
    }
    return result;
  }

  static LootData data() {
    return result().data();
  }

  static ItemCatalog items() {
    result();
    return items;
  }
}
