package dev.shoko.minecraft.loot.eval;

import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.LootFinding;
import java.util.List;

/**
 * The content of an unpacked container (LT-R68).
 *
 * @param slots one stack per slot, the empty stack for a free slot; the size of the container
 * @param findings the runtime observations of the draw
 */
public record UnpackResult(List<ItemStack> slots, List<LootFinding> findings) {}
