package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.data.catalog.RegistryCatalog;
import dev.shoko.minecraft.data.catalog.StaticRegistryData;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.loader.DatapackLoader;
import dev.shoko.minecraft.data.loader.LoadRequest;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackFiles;
import dev.shoko.minecraft.data.pack.PackOrigin;
import dev.shoko.minecraft.data.pack.PackStack;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.pack.meta.PackType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads files written in Java strings through the datapack loader of {@code data}, as the game
 * does, to get the {@link LoadedData} that {@code LootDataLoader} reads. The static registries hold
 * the made-up items, blocks, entity types and damage types; the tags are the ones of {@link
 * EnchantFixtures#tags()}.
 */
public final class MiniDatapack {

  private static final String CATALOG =
      """
      {"others":{},
       "registries":{
         "minecraft:item":{"elements":false,"stable":true,"tags":true},
         "minecraft:block":{"elements":false,"stable":true,"tags":true},
         "minecraft:entity_type":{"elements":false,"stable":true,"tags":true},
         "minecraft:damage_type":{"elements":false,"stable":true,"tags":true},
         "minecraft:enchantment":{"elements":true,"stable":true,"tags":true},
         "minecraft:enchantment_provider":{"elements":true,"stable":true,"tags":false},
         "minecraft:loot_table":{"elements":true,"stable":true,"tags":false},
         "minecraft:predicate":{"elements":true,"stable":true,"tags":false},
         "minecraft:context_int_provider":{"elements":true,"stable":true,"tags":false},
         "minecraft:context_float_provider":{"elements":true,"stable":true,"tags":false}}}
      """;

  private static final List<String> STATIC = List.of("item", "block", "entity_type", "damage_type");

  private MiniDatapack() {}

  private static String staticRegistries(TestAccess access) {
    List<String> registries = new ArrayList<>();
    int protocolId = 0;
    for (String name : STATIC) {
      List<String> entries = new ArrayList<>();
      int rawId = 0;
      for (Identifier id : access.ids(name)) {
        entries.add("\"" + id + "\":{\"protocol_id\":" + rawId++ + "}");
      }
      registries.add(
          "\"minecraft:"
              + name
              + "\":{\"protocol_id\":"
              + protocolId++
              + ",\"entries\":{"
              + String.join(",", entries)
              + "}}");
    }
    return "{" + String.join(",", registries) + "}";
  }

  private static void addTags(Map<String, byte[]> files, TestAccess access) {
    List<String> registries = new ArrayList<>(STATIC);
    registries.add("enchantment");
    for (String registry : registries) {
      access
          .tagsOf(registry)
          .forEach(
              (tag, members) -> {
                List<String> quoted = members.stream().map(m -> "\"" + m + "\"").toList();
                String text = "{\"values\":[" + String.join(",", quoted) + "]}";
                files.put(
                    "data/minecraft/tags/" + registry + "/" + tag.path() + ".json",
                    text.getBytes(StandardCharsets.UTF_8));
              });
    }
  }

  /**
   * Loads a pack.
   *
   * @param elements path below {@code data/minecraft/} without the extension, such as {@code
   *     loot_table/blocks/stone}, to JSON text with a single quote for each double quote
   */
  public static LoadedData load(Map<String, String> elements) {
    TestAccess access = EnchantFixtures.tags();
    Map<String, byte[]> files = new LinkedHashMap<>();
    addTags(files, access);
    elements.forEach(
        (path, json) ->
            files.put(
                "data/minecraft/" + path + ".json",
                json.replace('\'', '"').getBytes(StandardCharsets.UTF_8)));
    Pack pack =
        Pack.implicit("test", PackOrigin.BUILT_IN, PackType.DATA, PackFiles.inMemory(files));
    RegistryCatalog catalog = RegistryCatalog.read(Json.parseStrict(CATALOG)).value().orElseThrow();
    List<StaticRegistryData> statics =
        StaticRegistryData.read(Json.parseStrict(staticRegistries(access))).value().orElseThrow();
    LoadRequest request =
        new LoadRequest(
            PackStack.of(PackType.DATA, List.of(pack)), catalog, statics, FeatureFlags.vanilla());
    try {
      return DatapackLoader.load(request).value().orElseThrow();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The enchantment files of the made-up universe, keyed for {@link #load}. */
  public static Map<String, String> enchantmentFiles() {
    Map<String, String> files = new LinkedHashMap<>();
    EnchantFixtures.definitions().forEach((name, json) -> files.put("enchantment/" + name, json));
    return files;
  }
}
