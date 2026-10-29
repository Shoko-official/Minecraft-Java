package dev.shoko.minecraft.loot.eval;

import dev.shoko.minecraft.loot.table.LootEntry;

/**
 * One choosable entry of a roll, after expansion and condition filtering (LT-R20 to LT-R22).
 *
 * @param entry a simple entry: {@code empty}, {@code item}, {@code loot_table}, {@code dynamic},
 *     {@code slots}, or a child produced by the expansion of a tag
 * @param effectiveWeight the weight after quality and luck, at least 1 (LT-R25)
 */
public record Candidate(LootEntry entry, int effectiveWeight) {}
