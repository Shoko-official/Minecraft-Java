package dev.shoko.minecraft.data.loader;

import dev.shoko.minecraft.data.catalog.StaticRegistryData;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.pack.PackFileRef;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.tag.TagResolution;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.TreeMap;

/** The result of a load (output of the spec). Immutable and safe to share between threads. */
public final class LoadedData {

  private static final Identifier FUNCTION = Identifier.minecraft("function");
  private static final Identifier STRUCTURE = Identifier.minecraft("structure");

  private final List<StaticRegistryData> staticRegistries;
  private final List<ElementRegistryData> elementRegistries;
  private final Map<Identifier, Map<Identifier, PackFileRef>> contents;
  private final FeatureFlags featureFlags;
  private final List<PackReport> packReports;
  // The maps by registry name are only looked up, never iterated: their order is not observable.
  private final Map<Identifier, StaticRegistryData> staticByName = new HashMap<>();
  private final Map<Identifier, ElementRegistryData> elementsByName = new HashMap<>();
  private final Map<Identifier, TagResolution> tags;

  LoadedData(
      List<StaticRegistryData> staticRegistries,
      List<ElementRegistryData> elementRegistries,
      Map<Identifier, TagResolution> tags,
      Map<Identifier, Map<Identifier, PackFileRef>> contents,
      FeatureFlags featureFlags,
      List<PackReport> packReports) {
    this.staticRegistries = List.copyOf(staticRegistries);
    this.elementRegistries = List.copyOf(elementRegistries);
    this.tags = new HashMap<>(tags);
    this.contents = new HashMap<>(contents);
    this.featureFlags = featureFlags;
    this.packReports = List.copyOf(packReports);
    staticRegistries.forEach(registry -> staticByName.put(registry.registry(), registry));
    elementRegistries.forEach(registry -> elementsByName.put(registry.registry(), registry));
  }

  /**
   * Returns the static registries, as given in the request.
   *
   * @return the registries in order of their protocol ids (R26)
   */
  public List<StaticRegistryData> staticRegistries() {
    return staticRegistries;
  }

  /**
   * Returns the registries loaded from files, one per registry of the catalog that takes elements,
   * empty ones included (R28).
   *
   * @return the registries in code point order of their names
   */
  public List<ElementRegistryData> elementRegistries() {
    return elementRegistries;
  }

  /**
   * Returns one registry loaded from files.
   *
   * @param registry the name of the registry
   * @return the registry, or empty when the catalog has no such registry or it takes no elements
   */
  public Optional<ElementRegistryData> elementRegistry(Identifier registry) {
    return Optional.ofNullable(elementsByName.get(registry));
  }

  /**
   * Returns the tags of a registry, merged across packs and resolved (R33 to R37).
   *
   * @param registry the name of the registry; {@code minecraft:function} is accepted (R10)
   * @return the resolution, or empty when the registry is unknown or takes no tags (R16)
   */
  public Optional<TagResolution> tags(Identifier registry) {
    return Optional.ofNullable(tags.get(registry));
  }

  /**
   * Returns the function files kept, not interpreted (R10, R41).
   *
   * @return the files by identifier in code point order
   */
  public SequencedMap<Identifier, PackFileRef> functions() {
    return content(FUNCTION);
  }

  /**
   * Returns the structure files kept, not interpreted (R10, R41).
   *
   * @return the files by identifier in code point order
   */
  public SequencedMap<Identifier, PackFileRef> structures() {
    return content(STRUCTURE);
  }

  /**
   * Returns the feature flags in effect, repeated from the request without change (R66 e).
   *
   * @return the flags, iterated in code point order
   */
  public FeatureFlags featureFlags() {
    return featureFlags;
  }

  /**
   * Returns the report of each pack of the request, the packs discarded for inactive flags included
   * (R66 c).
   *
   * @return the reports from the lowest to the highest priority
   */
  public List<PackReport> packReports() {
    return packReports;
  }

  /**
   * Returns an access answering from the identifiers and tags loaded here, so that codecs can check
   * references before any typed registry exists (K3, K4). It holds no typed registry: {@link
   * RegistryAccess#registry} is empty until the caller layers typed registries on top.
   *
   * @return the access
   */
  public RegistryAccess registryAccess() {
    return new LoadedAccess(this);
  }

  boolean hasEntry(Identifier registry, Identifier id) {
    StaticRegistryData fixed = staticByName.get(registry);
    if (fixed != null) {
      return fixed.entryProtocolId(id).isPresent();
    }
    ElementRegistryData loaded = elementsByName.get(registry);
    if (loaded != null) {
      return loaded.get(id).isPresent();
    }
    return content(registry).containsKey(id);
  }

  private SequencedMap<Identifier, PackFileRef> content(Identifier registry) {
    Map<Identifier, PackFileRef> files = contents.getOrDefault(registry, Map.of());
    return Collections.unmodifiableSequencedMap(new TreeMap<>(files));
  }
}
