package dev.shoko.minecraft.data.loader;

import dev.shoko.minecraft.data.catalog.RegistryDescriptor;
import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.pack.PackFileRef;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.tag.TagResolution;
import dev.shoko.minecraft.data.tag.TagResolver;
import dev.shoko.minecraft.data.tag.TagSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.TreeMap;

/** Merges and resolves the tags of every registry that takes some, once the files are read. */
final class TagLoading {

  private final List<Diagnostic> diagnostics = new ArrayList<>();
  private final Map<Identifier, TagResolution> resolutions = new TreeMap<>();

  TagLoading(
      LoadRequest request,
      Map<Identifier, Map<Identifier, JsonNode>> elements,
      Map<Identifier, Map<Identifier, PackFileRef>> contents,
      Map<Identifier, Map<Identifier, List<TagSource>>> tagLayers) {
    for (RegistryDescriptor registry : request.catalog().registries()) {
      if (!registry.tags()) {
        continue;
      }
      Set<Identifier> universe = new HashSet<>();
      switch (registry.kind()) {
        case ELEMENTS -> universe.addAll(elements.get(registry.name()).keySet());
        case CONTENT -> universe.addAll(contents.get(registry.name()).keySet());
        case STATIC ->
            request.staticRegistries().stream()
                .filter(fixed -> fixed.registry().equals(registry.name()))
                .forEach(fixed -> universe.addAll(fixed.ids()));
      }
      SequencedMap<Identifier, List<TagSource>> layers = new LinkedHashMap<>();
      layers.putAll(tagLayers.getOrDefault(registry.name(), Map.of()));
      Outcome<TagResolution> resolved =
          TagResolver.resolve(new RegistryKey<Object>(registry.name()), layers, universe);
      diagnostics.addAll(resolved.diagnostics());
      resolved.value().ifPresent(value -> resolutions.put(registry.name(), value));
    }
  }

  List<Diagnostic> diagnostics() {
    return diagnostics;
  }

  Map<Identifier, TagResolution> resolutions() {
    return resolutions;
  }
}
