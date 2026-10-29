package dev.shoko.minecraft.item.recipe.book;

import dev.shoko.minecraft.item.recipe.Recipe;
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
    return Skeleton.throwNotImplemented();
  }
}
