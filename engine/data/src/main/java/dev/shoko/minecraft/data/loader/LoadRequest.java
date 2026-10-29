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
 * @param packs the datapack stack, vanilla first, then the active packs in the order recorded by
 *     the world, the last having the highest priority (R40)
 * @param catalog the loadable registries, read from the extracted reports
 * @param staticRegistries the static registries, read from the extracted reports; they give the
 *     universe of the tags of those registries (R33)
 * @param activeFlags the active feature flags, an input computed and owned by the caller (R45, R66
 *     a): the loader never changes it and only compares it with the flags each pack requires. It
 *     always contains {@code minecraft:vanilla}: that is an invariant of the caller, not checked
 *     (R66 a, d). A pack whose required flags are not all in it is discarded from the load with a
 *     {@code PACK_FLAGS_INACTIVE} error, and the other packs load normally (R66 c)
 */
public record LoadRequest(
    PackStack packs,
    RegistryCatalog catalog,
    List<StaticRegistryData> staticRegistries,
    FeatureFlags activeFlags) {}
