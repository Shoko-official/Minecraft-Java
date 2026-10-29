package dev.shoko.minecraft.loot.assembly;

import java.util.List;

/**
 * What loading produced. Loading never stops at the first problem: an element that fails is absent
 * from the data and its findings are listed.
 *
 * @param data the loaded data, without the elements that failed
 * @param findings the problems in registry order (providers and predicates, enchantments, tables),
 *     then element identifier order, then the order they were made
 */
public record LootLoadResult(LootData data, List<LootLoadFinding> findings) {}
