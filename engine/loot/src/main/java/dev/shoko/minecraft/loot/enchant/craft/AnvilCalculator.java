package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.loot.assembly.LootData;

/**
 * The cost and result of the anvil (EN-R41 to EN-R43, probe PE2, vector V-E13). A pure function: no
 * random draw, the menu and the slots are in {@code game}.
 */
public final class AnvilCalculator {

  private AnvilCalculator() {}

  /**
   * Computes the anvil output. Repair with a material: a unit restores 25 percent of {@code
   * max_damage} rounded down for one level (EN-R42a). Merge of two items: the remaining
   * durabilities plus 12 percent of {@code max_damage}, capped (EN-R42b). Enchantments of the
   * sacrifice that the target supports (EN-R43c): added at their level unless one of the target's
   * excludes them, then the loss costs one level (EN-R43a); already present: the higher level wins,
   * equal levels give +1 up to {@code max_level}, a lower level does nothing (EN-R43b); the cost of
   * each is the resulting level times {@code anvil_cost}, halved with a floor of 1 for a book
   * (EN-R43d, provisional). The output {@code repair_cost} is {@code 2 * max(costs) + 1} (EN-R41b);
   * a rename alone keeps it. The name is limited to 50 characters (EN-R42c).
   *
   * @param input the two slots and the name field
   * @param creative whether the player is in creative mode, which lifts the limit of 39 levels
   * @param data the loaded data
   * @return the output with its cost, or the reason there is none
   */
  public static AnvilResult combine(AnvilInput input, boolean creative, LootData data) {
    return Skeleton.throwNotImplemented();
  }
}
