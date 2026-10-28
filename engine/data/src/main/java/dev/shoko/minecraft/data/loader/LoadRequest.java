package dev.shoko.minecraft.data.loader;

import dev.shoko.minecraft.data.catalog.RegistryCatalog;
import dev.shoko.minecraft.data.catalog.StaticRegistryData;
import dev.shoko.minecraft.data.pack.PackStack;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import java.util.List;

/**
 * Everything the loader needs (input of the spec): the packs in priority order, the registries they
 * can fill, the static registries, and the feature flags in effect.
 *
 * @param packs the datapack stack, vanilla first (R40)
 * @param catalog the loadable registries, read from the extracted reports
 * @param staticRegistries the static registries, read from the extracted reports; they give the
 *     universe of the tags of those registries (R33)
 * @param activeFlags the feature flags of the world (R45), reported back unchanged by the loader
 */
public record LoadRequest(
    PackStack packs,
    RegistryCatalog catalog,
    List<StaticRegistryData> staticRegistries,
    FeatureFlags activeFlags) {}
