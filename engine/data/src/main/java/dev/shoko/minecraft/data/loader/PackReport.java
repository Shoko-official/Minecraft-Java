package dev.shoko.minecraft.data.loader;

import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.pack.meta.PackCompatibility;

/**
 * What the loader learned about one pack (output of the spec). There is one report per pack of the
 * request, discarded packs included.
 *
 * @param pack the pack identifier
 * @param compatibility the result of the format check (R5)
 * @param experimental whether the pack holds a valid element file in a registry that is not stable
 *     (R17); it has no effect on loading, and is {@code false} for a discarded pack
 * @param features the flags the pack requires (R44, R66 b)
 * @param missingFlags the flags of {@code features} that are not in the active flags of the request
 *     (R66 c); the pack is discarded from the stack, with a {@code PACK_FLAGS_INACTIVE} error, if
 *     and only if this set is not empty
 */
public record PackReport(
    String pack,
    PackCompatibility compatibility,
    boolean experimental,
    FeatureFlags features,
    FeatureFlags missingFlags) {}
