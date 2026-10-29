package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.SmithingTransformRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTrimRecipe;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * Finds the smithing recipe of a template, a base and an addition (RC-R36, RC-R37, provisional).
 * The trim material comes from the {@code provides_trim_material} component of the addition.
 */
public final class SmithingLookup {

  private static final Identifier TRIM_MATERIAL = Identifier.minecraft("provides_trim_material");

  private final RecipeSet recipes;

  private SmithingLookup(RecipeSet recipes) {
    this.recipes = recipes;
  }

  /**
   * Creates a lookup.
   *
   * @param recipes the loaded recipes
   * @param items the loaded items; the results are built from the stacks and the recipes, so the
   *     catalog is not consulted
   * @return the lookup
   */
  public static SmithingLookup of(RecipeSet recipes, ItemCatalog items) {
    return new SmithingLookup(recipes);
  }

  /**
   * Finds the recipe for three input stacks.
   *
   * @param template the stack in the template slot
   * @param base the stack in the base slot
   * @param addition the stack in the addition slot
   * @return the first matching recipe in resolution order with its result, empty when none
   */
  public Optional<SmithingMatch> find(ItemStack template, ItemStack base, ItemStack addition) {
    if (template.isEmpty() || base.isEmpty() || addition.isEmpty()) {
      return Optional.empty();
    }
    for (Recipe recipe : recipes.ofType(RecipeType.SMITHING)) {
      Optional<ItemStack> result =
          switch (recipe) {
            case SmithingTransformRecipe transform ->
                transform(transform, template, base, addition);
            case SmithingTrimRecipe trim -> trim(trim, template, base, addition);
            default -> Optional.empty();
          };
      if (result.isPresent()) {
        return Optional.of(new SmithingMatch(recipe, result.get()));
      }
    }
    return Optional.empty();
  }

  /** RC-R36: the result item replaces the base item, every modification of the base is kept. */
  private static Optional<ItemStack> transform(
      SmithingTransformRecipe recipe, ItemStack template, ItemStack base, ItemStack addition) {
    boolean fits =
        recipe.template().matches(template)
            && recipe.base().matches(base)
            && recipe.addition().matches(addition);
    if (!fits) {
      return Optional.empty();
    }
    ItemStack file = recipe.result();
    return Optional.of(GridStacks.carry(base, file.type(), 1, file.patch()));
  }

  /** RC-R37: the base gets the {@code trim} component of the material and the pattern. */
  private static Optional<ItemStack> trim(
      SmithingTrimRecipe recipe, ItemStack template, ItemStack base, ItemStack addition) {
    boolean fits =
        recipe.template().matches(template)
            && recipe.base().matches(base)
            && recipe.addition().matches(addition);
    Optional<NbtTag> material = GridStacks.tag(addition, TRIM_MATERIAL);
    if (!fits || material.isEmpty()) {
      return Optional.empty();
    }
    SequencedMap<String, NbtTag> trim = new LinkedHashMap<>();
    trim.put("material", material.get());
    trim.put("pattern", new StringTag(recipe.pattern().toString()));
    return Optional.of(
        base.withCount(1)
            .withComponent(WellKnownComponents.TRIM, new ComponentValue(CompoundTag.of(trim))));
  }
}
