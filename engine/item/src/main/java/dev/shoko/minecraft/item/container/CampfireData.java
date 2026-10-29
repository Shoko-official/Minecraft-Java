package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;

/**
 * The stored state of a campfire (IC-R31, RC-R30).
 *
 * @param slots the four cooking slots
 * @param cookingTimes {@code CookingTimes}, an Int_Array of 4
 * @param cookingTotalTimes {@code CookingTotalTimes}, an Int_Array of 4
 */
public record CampfireData(
    List<ItemStack> slots, List<Integer> cookingTimes, List<Integer> cookingTotalTimes) {}
