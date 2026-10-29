package dev.shoko.minecraft.item.hunt;

import dev.shoko.minecraft.data.catalog.RegistryCatalog;
import dev.shoko.minecraft.data.catalog.StaticRegistryData;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.loader.DatapackLoader;
import dev.shoko.minecraft.data.loader.LoadRequest;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackDiscovery;
import dev.shoko.minecraft.data.pack.PackStack;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.item.recipe.codec.RecipeLoadResult;
import dev.shoko.minecraft.item.recipe.codec.RecipeLoader;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.ReferenceCache;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** The reference items, tags and recipes, loaded once; the tests skip when the cache is absent. */
final class RealData {

  record Loaded(ItemCatalog items, LoadedData data, RecipeLoadResult recipes) {

    ItemType type(String path) {
      return items.find(Identifier.minecraft(path)).orElseThrow();
    }

    ItemStack stack(String path, int count) {
      return ItemStack.of(type(path), count);
    }
  }

  private static Loaded cached;

  private RealData() {}

  static synchronized Loaded get() {
    if (cached == null) {
      try {
        cached = load();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    return cached;
  }

  private static Loaded load() throws IOException {
    Path reference = ReferenceCache.require();
    ItemCatalog items = Findings.value(ItemCatalog.load(ReferenceCache.itemReport()));
    RegistryCatalog catalog =
        RegistryCatalog.read(
                ReferenceCache.json(reference.resolve("generated/reports/datapack.json")))
            .value()
            .orElseThrow();
    List<StaticRegistryData> statics =
        StaticRegistryData.read(
                ReferenceCache.json(reference.resolve("generated/reports/registries.json")))
            .value()
            .orElseThrow();
    List<Pack> available =
        PackDiscovery.referencePacks(reference.resolve("resources")).value().orElseThrow();
    List<Pack> chosen = available.stream().filter(p -> p.id().equals("vanilla")).toList();
    LoadedData data =
        DatapackLoader.load(
                new LoadRequest(
                    PackStack.of(PackType.DATA, chosen), catalog, statics, FeatureFlags.vanilla()))
            .value()
            .orElseThrow();
    RecipeLoadResult result =
        RecipeLoader.load(
            data.elementRegistry(Identifier.minecraft("recipe")).orElseThrow(),
            data.registryAccess(),
            items);
    return new Loaded(items, data, result);
  }
}
