package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.assembly.LootData;
import java.util.Optional;

/** The grindstone (EN-R44a, probe PE4). A pure function: no random draw. */
public final class GrindstoneCalculator {

  private GrindstoneCalculator() {}

  /**
   * Removes the enchantments that are not curses ({@code binding_curse} and {@code vanishing_curse}
   * stay), resets the work penalty to 0, keeps the name, and returns the experience that the
   * removed levels give. With two items, they are merged first as the grindstone does (open point
   * of the spec).
   *
   * @param first the top slot, possibly empty
   * @param second the bottom slot, possibly empty
   * @param data the loaded data
   * @return the output, empty when both slots are empty or the grindstone cannot take the items
   */
  public static Optional<GrindResult> grind(ItemStack first, ItemStack second, LootData data) {
    return Skeleton.throwNotImplemented();
  }
}
