package dev.shoko.minecraft.item.recipe.ingredient;

import dev.shoko.minecraft.data.id.Identifier;

/** The condition a brewing input puts on {@code potion_contents} (RC-R10). */
public sealed interface PotionMatch permits PotionMatch.Any, PotionMatch.Exact {

  /** No condition: the potion contents are not looked at. */
  record Any() implements PotionMatch {}

  /**
   * The {@code potion_contents.potions} of the bottle must be this potion.
   *
   * @param potion the potion identifier, such as {@code minecraft:awkward}
   */
  record Exact(Identifier potion) implements PotionMatch {}
}
