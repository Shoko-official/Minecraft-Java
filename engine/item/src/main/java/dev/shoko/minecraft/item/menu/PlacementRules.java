package dev.shoko.minecraft.item.menu;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.Objects;

/**
 * Decides whether a stack may be placed in a slot (IC-R38a to IC-R38c, provisional). The answer is:
 * never for {@link SlotRole#RESULT}; for an armor slot only when {@code equippable.slot} of the
 * stack names that slot and for no other armor slot; for {@link SlotRole#INPUT} what the {@link
 * MachineRules} say; always for the other roles. The empty stack is always accepted.
 */
public final class PlacementRules {

  private PlacementRules() {}

  private static String equipmentSlot(SlotRole role) {
    return switch (role) {
      case ARMOR_HEAD -> "head";
      case ARMOR_CHEST -> "chest";
      case ARMOR_LEGS -> "legs";
      default -> "feet";
    };
  }

  private static boolean fitsArmorSlot(ItemStack stack, SlotRole role) {
    return stack
        .component(WellKnownComponents.EQUIPPABLE)
        .map(ComponentValue::tag)
        .filter(CompoundTag.class::isInstance)
        .map(tag -> ((CompoundTag) tag).getString("slot"))
        .filter(equipmentSlot(role)::equals)
        .isPresent();
  }

  /**
   * Tells whether a stack may be placed in a slot.
   *
   * @param layout the menu layout
   * @param slot the slot index
   * @param stack the stack to place
   * @param machine what the machine inputs accept
   * @return {@code true} when the placement is allowed
   * @throws IndexOutOfBoundsException when the slot is outside the layout
   */
  public static boolean mayPlace(
      MenuLayout layout, int slot, ItemStack stack, MachineRules machine) {
    SlotRole role = layout.roles().get(Objects.checkIndex(slot, layout.slotCount()));
    if (stack.isEmpty()) {
      return true;
    }
    return switch (role) {
      case RESULT -> false;
      case INPUT -> machine.acceptsInput(layout.menuType(), slot, stack);
      case ARMOR_HEAD, ARMOR_CHEST, ARMOR_LEGS, ARMOR_FEET -> fitsArmorSlot(stack, role);
      default -> true;
    };
  }
}
