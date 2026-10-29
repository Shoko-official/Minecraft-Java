package dev.shoko.minecraft.loot.eval;

import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootFinding;
import java.util.List;

/**
 * The outcome of drawing a table (LT-R55, LT-R56).
 *
 * @param stacks the produced stacks in order, none empty, none above its maximum stack size
 *     (LT-R54), unmodifiable
 * @param findings the runtime observations in the order they were made, such as a missing nested
 *     table or a {@code dynamic} source without emitter, unmodifiable
 */
public record LootResult(List<ItemStack> stacks, List<LootFinding> findings) {}
