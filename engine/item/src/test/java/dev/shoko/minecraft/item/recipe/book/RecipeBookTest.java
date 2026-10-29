package dev.shoko.minecraft.item.recipe.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.testing.Recipes;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

// spec: RC-R51, RC-R52: the recipes a player has unlocked
class RecipeBookTest {

  private static final Identifier A = Identifier.minecraft("a");
  private static final Identifier B = Identifier.minecraft("b");
  private static final Identifier C = Identifier.minecraft("brewing/c");

  private static RecipeSet recipes() {
    String json =
        "{'type':'minecraft:stonecutting','ingredient':'minecraft:stone',"
            + "'result':{'id':'minecraft:stone_slab'}}";
    return Recipes.set("z", json, "a", json, "brewing/c", json);
  }

  @Test
  void aNewBookHasNothingUnlocked() {
    // spec: RC-R51
    RecipeBook book = RecipeBook.empty();
    assertThat(book.unlocked()).isEmpty();
    assertThat(book.contains(A)).isFalse();
  }

  @Test
  void provisionalGivingRecipesUnlocksThem() {
    // spec: RC-R51 (an advancement reward or /recipe give <recipe>)
    RecipeBook book = RecipeBook.empty().give(List.of(A, B));
    assertThat(book.contains(A)).isTrue();
    assertThat(book.contains(B)).isTrue();
    assertThat(book.contains(C)).isFalse();
    assertThat(book.unlocked()).containsExactly(A, B);
  }

  @Test
  void provisionalGivingARecipeAlreadyUnlockedChangesNothing() {
    // spec: RecipeBook#give Javadoc
    RecipeBook once = RecipeBook.empty().give(List.of(A));
    RecipeBook twice = once.give(List.of(A, A));
    assertThat(twice).isEqualTo(once);
    assertThat(twice.unlocked()).containsExactly(A);
  }

  @Test
  void provisionalUnlockedRecipesAreInAscendingOrderWhateverTheGivingOrder() {
    // spec: RecipeBook Javadoc (ascending order)
    RecipeBook book = RecipeBook.empty().give(List.of(C, B)).give(List.of(A));
    assertThat(book.unlocked()).containsExactly(A, B, C);
    assertThat(RecipeBook.empty().give(List.of(A, B, C))).isEqualTo(book);
  }

  @Test
  void provisionalTakingRecipesLocksThemAndIgnoresTheOnesNotUnlocked() {
    // spec: RC-R51 (/recipe take)
    RecipeBook book = RecipeBook.empty().give(List.of(A, B));
    RecipeBook taken = book.take(List.of(A, C));
    assertThat(taken.contains(A)).isFalse();
    assertThat(taken.contains(B)).isTrue();
    assertThat(taken.unlocked()).containsExactly(B);
    assertThat(book.take(List.of(C))).isEqualTo(book);
  }

  @Test
  void provisionalABookIsNotChangedByTheOperationsThatDeriveOthersFromIt() {
    // spec: RecipeBook is immutable
    RecipeBook book = RecipeBook.empty().give(List.of(A));
    RecipeBook more = book.give(List.of(B));
    RecipeBook less = book.take(List.of(A));
    assertThat(book.unlocked()).containsExactly(A);
    assertThat(more.unlocked()).containsExactly(A, B);
    assertThat(less.unlocked()).isEmpty();
  }

  @Test
  void theUnlockedSetCannotBeModified() {
    // spec: RecipeBook Javadoc (unmodifiable)
    RecipeBook book = RecipeBook.empty().give(List.of(A));
    Set<Identifier> unlocked = book.unlocked();
    assertThatThrownBy(() -> unlocked.add(B)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> unlocked.remove(A)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void provisionalGivingEveryRecipeOfASetUnlocksThemAll() {
    // spec: RC-R51 (/recipe give <players> *)
    RecipeBook book = RecipeBook.empty().giveAll(recipes());
    assertThat(book.unlocked()).containsExactly(A, C, Identifier.minecraft("z"));
    assertThat(book.contains(Identifier.minecraft("z"))).isTrue();
  }

  @Test
  void provisionalGivingEveryRecipeKeepsWhatWasAlreadyUnlocked() {
    // spec: RC-R51
    Identifier extra = Identifier.minecraft("not_in_the_set");
    RecipeBook book = RecipeBook.empty().give(List.of(extra)).giveAll(recipes());
    assertThat(book.contains(extra)).isTrue();
    assertThat(book.unlocked()).hasSize(4);
  }

  @Test
  void provisionalWithoutLimitedCraftingEveryRecipeMayBeCrafted() {
    // spec: RC-R52 (limited_crafting false by default: no restriction)
    RecipeBook book = RecipeBook.empty().give(List.of(A));
    assertThat(book.mayCraft(A, false)).isTrue();
    assertThat(book.mayCraft(B, false)).isTrue();
    assertThat(RecipeBook.empty().mayCraft(B, false)).isTrue();
  }

  @Test
  void provisionalWithLimitedCraftingOnlyUnlockedRecipesMayBeCrafted() {
    // spec: RC-R52
    RecipeBook book = RecipeBook.empty().give(List.of(A));
    assertThat(book.mayCraft(A, true)).isTrue();
    assertThat(book.mayCraft(B, true)).isFalse();
    assertThat(RecipeBook.empty().mayCraft(A, true)).isFalse();
    assertThat(book.take(List.of(A)).mayCraft(A, true)).isFalse();
  }
}
