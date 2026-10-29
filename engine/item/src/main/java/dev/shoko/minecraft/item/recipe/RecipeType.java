package dev.shoko.minecraft.item.recipe;

/** The 8 entries of the {@code recipe_type} registry: one per machine (RC-R2, RC-R4). */
public enum RecipeType {
  /** Crafting table and player grid: shaped, shapeless, transmute and special recipes. */
  CRAFTING,
  /** Furnace: {@code smelting}. */
  SMELTING,
  /** Blast furnace: {@code blasting}. */
  BLASTING,
  /** Smoker: {@code smoking}. */
  SMOKING,
  /** Campfire: {@code campfire_cooking}. */
  CAMPFIRE_COOKING,
  /** Stonecutter: {@code stonecutting}. */
  STONECUTTING,
  /** Smithing table: {@code smithing_transform} and {@code smithing_trim}. */
  SMITHING,
  /** Brewing stand: {@code brewing}. */
  BREWING
}
