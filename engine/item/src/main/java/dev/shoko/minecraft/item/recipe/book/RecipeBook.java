package dev.shoko.minecraft.item.recipe.book;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import java.util.Collection;
import java.util.SortedSet;

/**
 * The recipes a player has unlocked (RC-R51, RC-R52).
 *
 * @param unlocked the identifiers of the recipes, in ascending order, unmodifiable
 */
public record RecipeBook(SortedSet<Identifier> unlocked) {

  /**
   * Returns a book with nothing unlocked.
   *
   * @return the empty book
   */
  public static RecipeBook empty() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Unlocks recipes, as an advancement reward or {@code /recipe give <recipe>} does.
   *
   * @param recipes the recipes; an identifier already unlocked changes nothing
   * @return the new book
   */
  public RecipeBook give(Collection<Identifier> recipes) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Unlocks every recipe of a set, as {@code /recipe give <players> *} does.
   *
   * @param recipes the loaded recipes
   * @return the new book
   */
  public RecipeBook giveAll(RecipeSet recipes) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Locks recipes, as {@code /recipe take} does.
   *
   * @param recipes the recipes; an identifier not unlocked changes nothing
   * @return the new book
   */
  public RecipeBook take(Collection<Identifier> recipes) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether a recipe is unlocked.
   *
   * @param recipe a recipe identifier
   * @return {@code true} when unlocked
   */
  public boolean contains(Identifier recipe) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether a recipe may be crafted (RC-R52, provisional).
   *
   * @param recipe a recipe identifier
   * @param limitedCrafting the value of the {@code limited_crafting} game rule
   * @return {@code true} when the rule is off, or when the rule is on and the recipe is unlocked
   */
  public boolean mayCraft(Identifier recipe, boolean limitedCrafting) {
    return Skeleton.throwNotImplemented();
  }
}
