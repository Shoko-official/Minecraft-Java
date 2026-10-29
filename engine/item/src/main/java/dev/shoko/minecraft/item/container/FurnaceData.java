package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/**
 * The stored state of a furnace, blast furnace or smoker (IC-R31, IC-R33, RC-R30). This is data:
 * how it evolves each tick belongs to {@code game}.
 *
 * @param slots the three slots: 0 ingredient, 1 fuel, 2 output
 * @param litTimeRemaining {@code lit_time_remaining}, a Short
 * @param litTotalTime {@code lit_total_time}, a Short
 * @param cookingTimeSpent {@code cooking_time_spent}, a Short
 * @param cookingTotalTime {@code cooking_total_time}, a Short
 * @param recipesUsed {@code RecipesUsed}: recipe identifier to a count, in stored order
 */
public record FurnaceData(
    List<ItemStack> slots,
    int litTimeRemaining,
    int litTotalTime,
    int cookingTimeSpent,
    int cookingTotalTime,
    SequencedMap<Identifier, Integer> recipesUsed) {

  /**
   * Copies the slots and the recipes used.
   *
   * @param slots the three slots
   * @param litTimeRemaining {@code lit_time_remaining}
   * @param litTotalTime {@code lit_total_time}
   * @param cookingTimeSpent {@code cooking_time_spent}
   * @param cookingTotalTime {@code cooking_total_time}
   * @param recipesUsed {@code RecipesUsed}, in stored order
   */
  public FurnaceData {
    slots = List.copyOf(slots);
    recipesUsed = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(recipesUsed));
  }
}
