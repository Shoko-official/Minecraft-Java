package dev.shoko.minecraft.tools.oracle.nbt;

import java.util.Map;

/**
 * Root of an NBT document: its name and its entries in file order. Values follow the mapping
 * described in {@link NbtReader}.
 */
public record NamedCompound(String name, Map<String, Object> value) {}
