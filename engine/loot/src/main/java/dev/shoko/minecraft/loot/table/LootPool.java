package dev.shoko.minecraft.loot.table;

import dev.shoko.minecraft.loot.modifier.ItemModifier;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.List;

/**
 * A pool of a table (LT-R5a, LT-R5b, LT-R6).
 *
 * @param conditions the pool conditions, all must hold (LT-R19a); empty when the key is absent
 * @param rolls the number of rolls: a constant or a provider (LT-R6)
 * @param bonusRolls the extra rolls per point of luck, the constant 0 when absent (LT-R5b,
 *     provisional)
 * @param entries the entries in file order
 * @param modifiers the modifiers applied to each stack the pool produces (LT-R19d)
 */
public record LootPool(
    List<LootCondition> conditions,
    IntProvider rolls,
    FloatProvider bonusRolls,
    List<LootEntry> entries,
    List<ItemModifier> modifiers) {}
