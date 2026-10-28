package dev.shoko.minecraft.data.loader;

import dev.shoko.minecraft.data.catalog.StaticRegistryData;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.pack.PackFileRef;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.tag.TagResolution;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;

/** The result of a load (output of the spec). Immutable and safe to share between threads. */
public final class LoadedData {

  private LoadedData() {}

  /**
   * Returns the static registries, as given in the request.
   *
   * @return the registries in order of their protocol ids (R26)
   */
  public List<StaticRegistryData> staticRegistries() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the registries loaded from files, one per registry of the catalog that takes elements,
   * empty ones included (R28).
   *
   * @return the registries in code point order of their names
   */
  public List<ElementRegistryData> elementRegistries() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns one registry loaded from files.
   *
   * @param registry the name of the registry
   * @return the registry, or empty when the catalog has no such registry or it takes no elements
   */
  public Optional<ElementRegistryData> elementRegistry(Identifier registry) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the tags of a registry, merged across packs and resolved (R33 to R37).
   *
   * @param registry the name of the registry; {@code minecraft:function} is accepted (R10)
   * @return the resolution, or empty when the registry is unknown or takes no tags (R16)
   */
  public Optional<TagResolution> tags(Identifier registry) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the function files kept, not interpreted (R10, R41).
   *
   * @return the files by identifier in code point order
   */
  public SequencedMap<Identifier, PackFileRef> functions() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the structure files kept, not interpreted (R10, R41).
   *
   * @return the files by identifier in code point order
   */
  public SequencedMap<Identifier, PackFileRef> structures() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the feature flags in effect, as given in the request.
   *
   * @return the flags
   */
  public FeatureFlags featureFlags() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the report of each pack.
   *
   * @return the reports from the lowest to the highest priority
   */
  public List<PackReport> packReports() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns an access answering from the identifiers and tags loaded here, so that codecs can check
   * references before any typed registry exists (K3, K4). It holds no typed registry: {@link
   * RegistryAccess#registry} is empty until the caller layers typed registries on top.
   *
   * @return the access
   */
  public RegistryAccess registryAccess() {
    return Skeleton.throwNotImplemented();
  }
}
