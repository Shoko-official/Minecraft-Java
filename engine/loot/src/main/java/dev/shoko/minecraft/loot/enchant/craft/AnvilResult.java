package dev.shoko.minecraft.loot.enchant.craft;

import dev.shoko.minecraft.item.stack.ItemStack;

/** The outcome of an anvil operation (EN-R41 to EN-R43). */
public sealed interface AnvilResult permits AnvilResult.Output, AnvilResult.Refused {

  /**
   * An output the player can take.
   *
   * @param result the output stack, with the new {@code repair_cost} (EN-R41b)
   * @param levelCost the cost in experience levels (EN-R41c)
   * @param materialUsed the units of the sacrifice stack consumed: the repair material count, or
   *     the whole sacrifice for a merge
   */
  record Output(ItemStack result, int levelCost, int materialUsed) implements AnvilResult {}

  /**
   * No output.
   *
   * @param reason why
   */
  record Refused(AnvilRefusal reason) implements AnvilResult {}
}
