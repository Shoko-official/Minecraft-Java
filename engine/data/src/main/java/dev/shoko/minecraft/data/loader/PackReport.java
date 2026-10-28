package dev.shoko.minecraft.data.loader;

import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.pack.meta.PackCompatibility;

/**
 * What the loader learned about one pack (output of the spec).
 *
 * @param pack the pack identifier
 * @param compatibility the result of the format check (R5)
 * @param experimental whether the pack holds a valid element file in a registry that is not stable
 *     (R17); it has no effect on loading
 * @param features the flags the pack requires (R44)
 */
public record PackReport(
    String pack, PackCompatibility compatibility, boolean experimental, FeatureFlags features) {}
