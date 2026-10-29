package dev.shoko.minecraft.item.recipe.book;

import dev.shoko.minecraft.item.recipe.BrewingRecipe;
import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeCategory;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTransformRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTrimRecipe;
import dev.shoko.minecraft.item.recipe.SpecialRecipe;
import dev.shoko.minecraft.item.recipe.StonecuttingRecipe;
import dev.shoko.minecraft.item.recipe.TransmuteRecipe;
import java.util.Optional;

/**
 * The correspondence from a recipe to its recipe book category (RC-R7, level S: the mapping from
 * type and {@code category} to the 13 categories is not in the data).
 */
public final class BookCategories {

  private BookCategories() {}

  /**
   * Returns the book category of a recipe.
   *
   * @param recipe a recipe
   * @return the category; empty for the recipes that the book does not list, the brewing recipes
   *     (RC-R53 lists them, but no category of the registry fits; provisional)
   */
  public static Optional<BookCategory> of(Recipe recipe) {
    return switch (recipe) {
      case ShapedRecipe shaped -> Optional.of(crafting(shaped.category()));
      case ShapelessRecipe shapeless -> Optional.of(crafting(shapeless.category()));
      case TransmuteRecipe transmute -> Optional.of(crafting(transmute.category()));
      case CookingRecipe cooking -> Optional.of(cooking(cooking));
      case StonecuttingRecipe _ -> Optional.of(BookCategory.STONECUTTER);
      case SmithingTransformRecipe _, SmithingTrimRecipe _ -> Optional.of(BookCategory.SMITHING);
      case SpecialRecipe _ -> Optional.of(BookCategory.CRAFTING_MISC);
      case BrewingRecipe _ -> Optional.empty();
    };
  }

  private static BookCategory crafting(RecipeCategory category) {
    return switch (category) {
      case BUILDING -> BookCategory.CRAFTING_BUILDING_BLOCKS;
      case REDSTONE -> BookCategory.CRAFTING_REDSTONE;
      case EQUIPMENT -> BookCategory.CRAFTING_EQUIPMENT;
      default -> BookCategory.CRAFTING_MISC;
    };
  }

  private static BookCategory cooking(CookingRecipe recipe) {
    boolean food = recipe.category() == RecipeCategory.FOOD;
    boolean blocks = recipe.category() == RecipeCategory.BLOCKS;
    return switch (recipe.type()) {
      case SMOKING -> BookCategory.SMOKER_FOOD;
      case CAMPFIRE_COOKING -> BookCategory.CAMPFIRE;
      case BLASTING -> blocks ? BookCategory.BLAST_FURNACE_BLOCKS : BookCategory.BLAST_FURNACE_MISC;
      default -> furnace(food, blocks);
    };
  }

  private static BookCategory furnace(boolean food, boolean blocks) {
    if (food) {
      return BookCategory.FURNACE_FOOD;
    }
    return blocks ? BookCategory.FURNACE_BLOCKS : BookCategory.FURNACE_MISC;
  }
}
