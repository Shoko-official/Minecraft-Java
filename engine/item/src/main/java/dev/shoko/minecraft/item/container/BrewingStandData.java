package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.List;

/**
 * The stored state of a brewing stand (IC-R31, RC-R35).
 *
 * @param slots the five slots: 0 to 2 bottles, 3 ingredient, 4 fuel
 * @param brewTime {@code BrewTime}, a Short
 * @param fuel {@code Fuel}, a Byte from 0 to 20
 */
public record BrewingStandData(List<ItemStack> slots, int brewTime, int fuel) {

  /**
   * Copies the slots.
   *
   * @param slots the five slots
   * @param brewTime {@code BrewTime}
   * @param fuel {@code Fuel}
   */
  public BrewingStandData {
    slots = List.copyOf(slots);
  }
}
