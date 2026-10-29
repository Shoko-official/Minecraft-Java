package dev.shoko.minecraft.item.recipe.ingredient;

/**
 * The bottle a brewing recipe transforms (RC-R10).
 *
 * @param item the accepted bottle items
 * @param potion the condition on the potion contents
 */
public record BrewingInput(Ingredient item, PotionMatch potion) {}
