package dev.shoko.minecraft.loot.assembly;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootFinding;

/**
 * A problem met while loading one element of a registry.
 *
 * @param registry the registry, such as {@code minecraft:loot_table}
 * @param element the identifier of the element
 * @param finding what went wrong, with the JSON path inside the element
 */
public record LootLoadFinding(Identifier registry, Identifier element, LootFinding finding) {}
