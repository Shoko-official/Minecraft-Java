package dev.shoko.minecraft.item.menu;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.ArrayList;
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
   * Copies the roles.
   *
   * @param menuType the {@code menu} registry entry
   * @param roles the role of each slot
   */
  public MenuLayout {
    roles = List.copyOf(roles);
  }

  /**
   * Returns the total number of slots.
   *
   * @return the size of {@link #roles()}
   */
  public int slotCount() {
    return roles.size();
  }

  /**
   * Returns the runs of slots with a role, in ascending order.
   *
   * @param role a role
   * @return the maximal runs of consecutive indices with that role, empty when there is none
   */
  public List<SlotRange> ranges(SlotRole role) {
    List<SlotRange> ranges = new ArrayList<>();
    int start = -1;
    for (int slot = 0; slot <= roles.size(); slot++) {
      boolean inRun = slot < roles.size() && roles.get(slot) == role;
      if (inRun && start < 0) {
        start = slot;
      } else if (!inRun && start >= 0) {
        ranges.add(new SlotRange(start, slot));
        start = -1;
      }
    }
    return List.copyOf(ranges);
  }

  /**
   * Tells whether the menu shows the player inventory after its own slots (IC-R37a).
   *
   * @return {@code false} for the lectern only
   */
  public boolean includesPlayerInventory() {
    return roles.contains(SlotRole.PLAYER_MAIN);
  }
}
