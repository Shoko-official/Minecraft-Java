package dev.shoko.minecraft.item.recipe;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.stack.ItemStack;

/**
 * {@code smelting} (73), {@code blasting} (25), {@code smoking} (9) and {@code campfire_cooking}
 * (9) recipes (RC-R25). The speed of the machine is not here: it comes from a provider evaluated by
 * {@code loot} (RC-R26).
 *
 * @param id the recipe identifier
 * @param group the recipe book group, empty when absent
 * @param category the recipe book category
 * @param type {@code SMELTING}, {@code BLASTING}, {@code SMOKING} or {@code CAMPFIRE_COOKING}
 * @param ingredient the cooked item
 * @param result the cooked stack
 * @param cookingTime the cooking time in ticks (200, or 600 on the campfire)
 * @param experience the experience per cooked result
 */
public record CookingRecipe(
    Identifier id,
    String group,
    RecipeCategory category,
    RecipeType type,
    Ingredient ingredient,
    ItemStack result,
    int cookingTime,
    float experience)
    implements Recipe {}
