package dev.shoko.minecraft.item.recipe.ingredient;

/**
 * An inclusive range of counts, such as the {@code material_count} of a transmutation (RC-R49f,
 * V-R9: 1 to 8). A single value gives a range with equal bounds (provisional).
 *
 * @param min the smallest count
 * @param max the largest count
 */
public record CountRange(int min, int max) {}
