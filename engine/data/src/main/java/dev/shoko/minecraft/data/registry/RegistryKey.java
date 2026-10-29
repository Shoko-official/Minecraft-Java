package dev.shoko.minecraft.data.registry;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * The name of a registry, such as {@code minecraft:block} or {@code minecraft:worldgen/biome}. The
 * type parameter ties the key to the type of the entries; it is not checked at run time.
 *
 * @param name the identifier of the registry; its path is the folder of the registry (R8)
 * @param <T> the type of the entries
 */
@SuppressWarnings("UnusedTypeParameter") // phantom type: ties a key to its entries at compile time
public record RegistryKey<T>(Identifier name) {}
