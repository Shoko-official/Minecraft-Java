package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.catalog.RegistryCatalog;
import dev.shoko.minecraft.data.catalog.StaticRegistryData;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.loader.DatapackLoader;
import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.loader.LoadRequest;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackFiles;
import dev.shoko.minecraft.data.pack.PackOrigin;
import dev.shoko.minecraft.data.pack.PackStack;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.TestItems;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads recipe files and item tags written in Java strings through the datapack loader of {@code
 * data}, as the game does, to get the {@link ElementRegistryData} that the recipe loader reads. The
 * item registry of the pack holds the fake items.
 */
final class MiniDatapack {

  private static final String CATALOG =
      """
      {"others":{
         "function":{"elements":true,"format":"mcfunction","stable":true,"tags":true},
         "structure":{"elements":true,"format":"structure","stable":true,"tags":false}},
       "registries":{
         "minecraft:item":{"elements":false,"stable":true,"tags":true},
         "minecraft:recipe":{"elements":true,"stable":true,"tags":false}}}
      """;

  private MiniDatapack() {}

  private static String staticItems() {
    List<String> entries = new ArrayList<>();
    int rawId = 0;
    for (String path : TestItems.itemPaths()) {
      entries.add("\"minecraft:" + path + "\":{\"protocol_id\":" + rawId++ + "}");
    }
    return "{\"minecraft:item\":{\"protocol_id\":0,\"entries\":{"
        + String.join(",", entries)
        + "}}}";
  }

  /**
   * Loads a pack.
   *
   * @param recipes recipe path (below {@code recipe/}, without extension) to JSON text with {@code
   *     '} for quotes
   * @param itemTags item tag path to its members, as item paths
   */
  static LoadedData load(Map<String, String> recipes, Map<String, List<String>> itemTags) {
    Map<String, byte[]> files = new LinkedHashMap<>();
    recipes.forEach(
        (path, json) ->
            files.put(
                "data/minecraft/recipe/" + path + ".json",
                json.replace('\'', '"').getBytes(StandardCharsets.UTF_8)));
    itemTags.forEach(
        (tag, members) -> {
          List<String> quoted = members.stream().map(m -> "\"minecraft:" + m + "\"").toList();
          String text = "{\"values\":[" + String.join(",", quoted) + "]}";
          files.put(
              "data/minecraft/tags/item/" + tag + ".json", text.getBytes(StandardCharsets.UTF_8));
        });
    Pack pack =
        Pack.implicit("test", PackOrigin.BUILT_IN, PackType.DATA, PackFiles.inMemory(files));
    RegistryCatalog catalog = RegistryCatalog.read(Json.parseStrict(CATALOG)).value().orElseThrow();
    List<StaticRegistryData> statics =
        StaticRegistryData.read(Json.parseStrict(staticItems())).value().orElseThrow();
    LoadRequest request =
        new LoadRequest(
            PackStack.of(PackType.DATA, List.of(pack)), catalog, statics, FeatureFlags.vanilla());
    try {
      return DatapackLoader.load(request).value().orElseThrow();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static ElementRegistryData recipes(LoadedData data) {
    return data.elementRegistry(Identifier.minecraft("recipe")).orElseThrow();
  }
}
