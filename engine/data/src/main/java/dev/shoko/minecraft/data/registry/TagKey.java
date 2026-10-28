package dev.shoko.minecraft.data.registry;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * The name of a tag in a registry, written {@code #namespace:path} where a tag is expected (R9,
 * R23).
 *
 * @param registry the registry the tag belongs to; a tag refers to entries of its own registry only
 *     (R33)
 * @param id the identifier of the tag, without the {@code #}
 * @param <T> the type of the entries
 */
public record TagKey<T>(RegistryKey<T> registry, Identifier id) {}
