package dev.shoko.minecraft.item.testing;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonReader;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.item.registry.ItemReportEntry;
import dev.shoko.minecraft.item.registry.ItemReportSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Access to the extracted reference data of the local cache ({@code MINECRAFT_CACHE_DIR}, else
 * {@code .cache} at the repository root), for the tests tagged {@code oracle}. Nothing is copied
 * into the repository; every test skips itself when the cache is absent.
 */
public final class ReferenceCache {

  private ReferenceCache() {}

  /** The reference directory, which may not exist. */
  public static Path reference() {
    String configured = System.getenv("MINECRAFT_CACHE_DIR");
    Path cache;
    if (configured != null && !configured.isBlank()) {
      cache = Path.of(configured);
    } else {
      Path dir = Path.of("").toAbsolutePath();
      while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
        dir = dir.getParent();
      }
      cache = (dir == null ? Path.of("") : dir).resolve(".cache");
    }
    return cache.resolve("reference/26.3");
  }

  /** The reference directory, or a skipped test when the cache is absent. */
  public static Path require() {
    Path dir = reference();
    assumeTrue(
        Files.isDirectory(dir.resolve("resources/data")), () -> "reference cache absent: " + dir);
    return dir;
  }

  public static JsonNode json(Path file) {
    try {
      return JsonReader.read(
              Files.readAllBytes(file), new FileOrigin("reference", file.getFileName().toString()))
          .value()
          .orElseThrow();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The item report of the reference data, read from {@code registries.json} and the files. */
  public static ItemReportSource itemReport() {
    Path reference = require();
    Path reports = reference.resolve("generated/reports");
    JsonObject registries = (JsonObject) json(reports.resolve("registries.json"));
    List<Identifier> componentTypes = entries(registries, "minecraft:data_component_type");
    JsonObject itemRegistry = (JsonObject) registries.get("minecraft:item").orElseThrow();
    Identifier defaultItem =
        Identifier.parse(((JsonString) itemRegistry.get("default").orElseThrow()).value());
    List<ItemReportEntry> items = new ArrayList<>();
    for (Identifier id : entries(registries, "minecraft:item")) {
      Path file = reports.resolve("minecraft/components/item/" + id.path() + ".json");
      JsonObject root = (JsonObject) json(file);
      items.add(new ItemReportEntry(id, root.get("components").orElseThrow()));
    }
    return new FileReport(componentTypes, defaultItem, List.copyOf(items));
  }

  /** The entries of a registry of {@code registries.json}, ordered by protocol id. */
  private static List<Identifier> entries(JsonObject registries, String registry) {
    JsonObject node = (JsonObject) registries.get(registry).orElseThrow();
    JsonObject entries = (JsonObject) node.get("entries").orElseThrow();
    return entries.members().entrySet().stream()
        .sorted(Comparator.comparingLong(e -> protocolId(e.getValue())))
        .map(e -> Identifier.parse(e.getKey()))
        .toList();
  }

  private static long protocolId(JsonNode entry) {
    JsonNumber id = (JsonNumber) ((JsonObject) entry).get("protocol_id").orElseThrow();
    return id.longValue();
  }

  /** The files of a directory tree below a reference path, sorted. */
  public static List<Path> files(Path directory, String suffix) {
    try (Stream<Path> walk = Files.walk(directory)) {
      return walk.filter(p -> p.toString().endsWith(suffix)).sorted().toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private record FileReport(
      List<Identifier> componentTypeIds, Identifier defaultItem, List<ItemReportEntry> items)
      implements ItemReportSource {}
}
