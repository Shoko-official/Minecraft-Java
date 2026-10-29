package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.LootContext;

/** Repair by experience, the {@code repair_with_xp} effect of {@code mending} (EN-R44b). */
public final class XpRepair {

  private XpRepair() {}

  /**
   * Repairs an item with picked-up experience: each point repairs {@code 2 * factor} durability
   * points, where the factor is the value of the {@code minecraft:repair_with_xp} component
   * evaluated over the item (start 1, EN-R19c); the formula is provisional (probe PE4).
   *
   * @param stack an item in an active slot, possibly with {@code mending}
   * @param experience the points of experience picked up
   * @param context the context, for the definitions and the conditions
   * @return the repaired item and the experience not used; unchanged when the item has no such
   *     effect or no damage
   */
  public static RepairResult repair(ItemStack stack, int experience, LootContext context) {
    return Skeleton.throwNotImplemented();
  }
}
