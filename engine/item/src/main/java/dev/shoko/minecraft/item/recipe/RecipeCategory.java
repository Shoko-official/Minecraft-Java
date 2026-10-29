package dev.shoko.minecraft.item.recipe;

/** The {@code category} field of a recipe file (RC-R7). An absent field is {@link #MISC}. */
public enum RecipeCategory {
  /** {@code building}: 493 recipes. */
  BUILDING,
  /** {@code redstone}: 120 recipes. */
  REDSTONE,
  /** {@code equipment}: 124 recipes. */
  EQUIPMENT,
  /** {@code misc}: the default when the field is absent (provisional). */
  MISC,
  /** {@code food}: smelting, smoking and campfire recipes. */
  FOOD,
  /** {@code blocks}: smelting and blasting recipes. */
  BLOCKS
}
