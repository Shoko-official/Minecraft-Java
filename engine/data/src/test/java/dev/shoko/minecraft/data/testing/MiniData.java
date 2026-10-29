package dev.shoko.minecraft.data.testing;

import dev.shoko.minecraft.data.catalog.RegistryCatalog;
import dev.shoko.minecraft.data.catalog.StaticRegistryData;
import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNull;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.loader.DatapackLoader;
import dev.shoko.minecraft.data.loader.ElementRegistryData;
import dev.shoko.minecraft.data.loader.LoadRequest;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.data.loader.PackReport;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackFileRef;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.tag.TagResolution;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A miniature of the extracted reports ({@code datapack.json}, {@code registries.json}) and the
 * calls that load packs against it. The shapes are the ones of the reference reports.
 */
public final class MiniData {

  public static final String CATALOG_JSON =
      """
      {"others":{
         "function":{"elements":true,"format":"mcfunction","stable":true,"tags":true},
         "structure":{"elements":true,"format":"structure","stable":true,"tags":false}},
       "registries":{
         "minecraft:block":{"elements":false,"stable":true,"tags":true},
         "minecraft:item":{"elements":false,"stable":true,"tags":true},
         "minecraft:loot_table":{"elements":true,"stable":true,"tags":true},
         "minecraft:villager_trade":{"elements":true,"stable":false,"tags":true},
         "minecraft:worldgen/biome":{"elements":true,"stable":false,"tags":true},
         "minecraft:dimension":{"elements":true,"stable":false,"tags":true}}}
      """;

  public static final String STATIC_JSON =
      """
      {"minecraft:block":{"default":"minecraft:air","protocol_id":0,"entries":{
         "minecraft:air":{"protocol_id":0},"minecraft:stone":{"protocol_id":1},
         "minecraft:dirt":{"protocol_id":2},"minecraft:oak_log":{"protocol_id":3},
         "minecraft:birch_log":{"protocol_id":4},"minecraft:glass":{"protocol_id":5}}},
       "minecraft:item":{"protocol_id":1,"entries":{
         "minecraft:air":{"protocol_id":0},"minecraft:stick":{"protocol_id":1}}}}
      """;

  private MiniData() {}

  public static RegistryCatalog catalog() {
    return RegistryCatalog.read(Jx.parseLenient(CATALOG_JSON)).value().orElseThrow();
  }

  public static List<StaticRegistryData> staticRegistries() {
    return StaticRegistryData.read(Jx.parseLenient(STATIC_JSON)).value().orElseThrow();
  }

  public static LoadRequest request(FeatureFlags flags, Pack... lowestFirst) {
    return new LoadRequest(Packs.stack(lowestFirst), catalog(), staticRegistries(), flags);
  }

  public static Outcome<LoadedData> load(FeatureFlags flags, Pack... lowestFirst) {
    try {
      return DatapackLoader.load(request(flags, lowestFirst));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Loads with only {@code minecraft:vanilla} active. */
  public static Outcome<LoadedData> load(Pack... lowestFirst) {
    return load(FeatureFlags.vanilla(), lowestFirst);
  }

  public static Identifier registry(String name) {
    return Identifier.parse(name);
  }

  /** The ordered identifiers of an element registry, as text. */
  public static List<String> elementIds(LoadedData data, String registry) {
    ElementRegistryData elements = data.elementRegistry(registry(registry)).orElseThrow();
    return elements.elements().keySet().stream().map(Identifier::toString).toList();
  }

  public static List<String> tagMembers(LoadedData data, String registry, String tag) {
    TagResolution tags = data.tags(registry(registry)).orElseThrow();
    return tags.members(Identifier.parse(tag)).orElseThrow().stream()
        .map(Identifier::toString)
        .toList();
  }

  /**
   * A text that captures everything observable in an outcome: diagnostics (code, file, path), the
   * elements with their written form and pack, the tags, the functions, the structures and the pack
   * reports. Two loads that must be indistinguishable give the same text.
   */
  public static String snapshot(Outcome<LoadedData> outcome) throws IOException {
    StringBuilder text = new StringBuilder();
    for (Diagnostic d : outcome.diagnostics()) {
      text.append("D ").append(d.code()).append(' ').append(d.origin()).append(' ');
      text.append(d.path()).append('\n');
    }
    LoadedData data = outcome.value().orElseThrow();
    for (ElementRegistryData registry : data.elementRegistries()) {
      for (Map.Entry<Identifier, JsonNode> e : registry.elements().entrySet()) {
        text.append("E ").append(registry.registry()).append(' ').append(e.getKey());
        text.append(' ').append(e.getValue().origin()).append(' ');
        text.append(describe(e.getValue())).append('\n');
      }
    }
    List<Identifier> registries = new ArrayList<>();
    registries.add(Identifier.parse("block"));
    registries.add(Identifier.parse("item"));
    registries.add(Identifier.parse("loot_table"));
    registries.add(Identifier.parse("villager_trade"));
    registries.add(Identifier.parse("function"));
    for (Identifier registry : registries) {
      data.tags(registry).ifPresent(tags -> appendTags(text, registry, tags));
    }
    appendFiles(text, "F ", data.functions());
    appendFiles(text, "S ", data.structures());
    for (PackReport report : data.packReports()) {
      text.append("R ").append(report.pack()).append(' ').append(report.compatibility());
      text.append(' ').append(report.experimental()).append(' ');
      text.append(report.features().flags()).append(' ');
      text.append(report.missingFlags().flags()).append('\n');
    }
    text.append("flags ").append(data.featureFlags().flags()).append('\n');
    return text.toString();
  }

  private static void appendTags(StringBuilder text, Identifier registry, TagResolution tags) {
    for (Identifier tag : tags.tags()) {
      text.append("T ").append(registry).append(" #").append(tag).append(' ');
      text.append(tags.members(tag).orElseThrow()).append('\n');
    }
    text.append("X ").append(registry).append(' ').append(tags.failed()).append('\n');
  }

  private static void appendFiles(
      StringBuilder text, String prefix, Map<Identifier, PackFileRef> files) throws IOException {
    for (Map.Entry<Identifier, PackFileRef> e : files.entrySet()) {
      text.append(prefix).append(e.getKey()).append(' ').append(e.getValue().origin());
      text.append(' ').append(Arrays.hashCode(e.getValue().read())).append('\n');
    }
  }

  /** The written form of a tree; numbers keep their literal, objects keep their member order. */
  public static String describe(JsonNode node) {
    return switch (node) {
      case JsonObject o -> {
        StringBuilder text = new StringBuilder("{");
        o.members().forEach((k, v) -> text.append(k).append('=').append(describe(v)).append(','));
        yield text.append('}').toString();
      }
      case JsonArray a -> a.elements().stream().map(MiniData::describe).toList().toString();
      case JsonString s -> '"' + s.value() + '"';
      case JsonNumber n -> n.literal();
      case JsonBoolean b -> Boolean.toString(b.value());
      case JsonNull ignored -> "null";
    };
  }
}
