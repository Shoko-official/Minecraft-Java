package dev.shoko.minecraft.item.menu;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;

/**
 * The slots of one menu type, in index order (IC-R37a to IC-R37d). The total count is the count of
 * own slots plus 36, or the count of own slots for the lectern.
 *
 * @param menuType the {@code menu} registry entry, such as {@code minecraft:furnace}
 * @param roles the role of each slot, the index being the position in the list, unmodifiable
 */
public record MenuLayout(Identifier menuType, List<SlotRole> roles) {

  /**
   * Returns the total number of slots.
   *
   * @return the size of {@link #roles()}
   */
  public int slotCount() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the runs of slots with a role, in ascending order.
   *
   * @param role a role
   * @return the maximal runs of consecutive indices with that role, empty when there is none
   */
  public List<SlotRange> ranges(SlotRole role) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether the menu shows the player inventory after its own slots (IC-R37a).
   *
   * @return {@code false} for the lectern only
   */
  public boolean includesPlayerInventory() {
    return Skeleton.throwNotImplemented();
  }
}
