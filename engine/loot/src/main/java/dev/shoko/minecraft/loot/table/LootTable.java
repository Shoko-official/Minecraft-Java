package dev.shoko.minecraft.loot.table;

import dev.shoko.minecraft.loot.context.LootContextType;
import dev.shoko.minecraft.loot.modifier.ItemModifier;
import dev.shoko.minecraft.loot.random.SequenceRef;
import java.util.List;

/**
 * A decoded loot table (LT-R1, LT-R2). The identifier is the key it is stored under in {@link
 * LootTables}; an inline table (LT-R10) has none.
 *
 * @param type the context type, {@code minecraft:generic} when the file has no {@code type}
 *     (provisional)
 * @param pools the pools in file order, empty for the 44 vanilla tables without pools (LT-R55)
 * @param modifiers the modifiers applied last to all the stacks (LT-R19d, LT-R48), in file order
 * @param randomSequence the sequence of the table (LT-R64)
 */
public record LootTable(
    LootContextType type,
    List<LootPool> pools,
    List<ItemModifier> modifiers,
    SequenceRef randomSequence) {}
