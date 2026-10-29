package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentPatch;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.recipe.SpecialRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SequencedMap;
import java.util.stream.IntStream;

/**
 * Recognition of the special crafting recipes whose result depends on the content of the stacks
 * (RC-R43 to RC-R49e, provisional level S). The firework and dye kinds are in {@link
 * ContentSpecials}; this class holds the repair, the layouts of the 3x3 grid, and the banners and
 * books.
 */
final class SpecialMatcher {

  private static final Identifier BANNER_PATTERNS = Identifier.minecraft("banner_patterns");
  private static final Identifier BASE_COLOR = Identifier.minecraft("base_color");
  private static final Identifier POT_DECORATIONS = Identifier.minecraft("pot_decorations");
  private static final Identifier BOOK = Identifier.minecraft("written_book_content");
  private static final int MAX_BANNER_PATTERNS = 6;
  private static final int MAX_BOOK_GENERATION = 2;
  private static final int REPAIR_BONUS_PERCENT = 5;
  private static final int CENTER = 4;
  private static final int SIDE = 3;

  private SpecialMatcher() {}

  static Optional<ItemStack> match(SpecialRecipe recipe, CraftingGrid grid) {
    List<ItemStack> cells = GridStacks.nonEmpty(grid);
    return switch (recipe.kind()) {
      case REPAIR_ITEM -> repair(cells);
      case IMBUE -> imbue(recipe, grid);
      case MAP_EXTENDING ->
          ringCenter(recipe, grid, "map", "material").map(map -> resultOrCopy(recipe, map));
      case DECORATED_POT -> pot(recipe, grid);
      case BANNER_DUPLICATE -> bannerDuplicate(recipe, cells);
      case BOOK_CLONING -> bookCloning(recipe, cells);
      case SHIELD_DECORATION -> shieldDecoration(recipe, cells);
      default -> ContentSpecials.match(recipe, cells);
    };
  }

  static Ingredient part(SpecialRecipe recipe, String name) {
    return recipe.parts().getOrDefault(name, Ingredient.none());
  }

  /** The result of the file with the modifications of a model, or a copy of the model. */
  static ItemStack resultOrCopy(SpecialRecipe recipe, ItemStack model) {
    if (recipe.result() instanceof RecipeResult.Fixed fixed) {
      ItemStack file = fixed.stack();
      return GridStacks.carry(model, file.type(), file.count(), file.patch());
    }
    return model.withCount(1);
  }

  /** RC-R49a: two worn items of one kind give one item with the remaining wear added up. */
  private static Optional<ItemStack> repair(List<ItemStack> cells) {
    if (cells.size() != 2) {
      return Optional.empty();
    }
    ItemStack first = cells.get(0);
    ItemStack second = cells.get(1);
    boolean sameKind = first.type().id().equals(second.type().id());
    if (!sameKind || !first.isDamageable() || !second.isDamageable()) {
      return Optional.empty();
    }
    int max = first.maxDamage();
    long left = (long) (max - first.damage()) + (max - second.damage());
    long repaired = Math.min(max, left + (long) max * REPAIR_BONUS_PERCENT / 100);
    ItemStack fresh = ItemStack.of(first.type(), 1);
    int damage = (int) Math.max(0, max - repaired);
    IntTag worn = new IntTag(damage);
    return Optional.of(
        damage == 0
            ? fresh
            : fresh.withComponent(WellKnownComponents.DAMAGE, new ComponentValue(worn)));
  }

  /** The center stack of a 3x3 grid whose eight other cells hold the second ingredient. */
  private static Optional<ItemStack> ringCenter(
      SpecialRecipe recipe, CraftingGrid grid, String center, String around) {
    if (grid.width() != SIDE || grid.height() != SIDE) {
      return Optional.empty();
    }
    ItemStack middle = grid.stacks().get(CENTER);
    if (!part(recipe, center).matches(middle)) {
      return Optional.empty();
    }
    for (int i = 0; i < grid.stacks().size(); i++) {
      if (i != CENTER && !part(recipe, around).matches(grid.stacks().get(i))) {
        return Optional.empty();
      }
    }
    return Optional.of(middle);
  }

  /** RC-R49e: eight stacks around a potion give the stack of the file with the potion. */
  private static Optional<ItemStack> imbue(SpecialRecipe recipe, CraftingGrid grid) {
    if (!(recipe.result() instanceof RecipeResult.Fixed fixed)) {
      return Optional.empty();
    }
    return ringCenter(recipe, grid, "source", "material")
        .map(
            potion ->
                potion
                    .component(WellKnownComponents.POTION_CONTENTS)
                    .map(
                        contents ->
                            fixed
                                .stack()
                                .withComponent(WellKnownComponents.POTION_CONTENTS, contents))
                    .orElse(fixed.stack()));
  }

  /** RC-R49c: four items in a cross around the empty center. */
  private static Optional<ItemStack> pot(SpecialRecipe recipe, CraftingGrid grid) {
    if (grid.width() != SIDE
        || grid.height() != SIDE
        || GridStacks.nonEmpty(grid).size() != 4
        || !(recipe.result() instanceof RecipeResult.Fixed fixed)) {
      return Optional.empty();
    }
    SequencedMap<String, ItemStack> sides = new LinkedHashMap<>();
    sides.put("back", grid.get(1, 0));
    sides.put("front", grid.get(1, 2));
    sides.put("left", grid.get(0, 1));
    sides.put("right", grid.get(2, 1));
    SequencedMap<String, NbtTag> decorations = new LinkedHashMap<>();
    for (var side : sides.entrySet()) {
      if (!part(recipe, side.getKey()).matches(side.getValue())) {
        return Optional.empty();
      }
      decorations.put(side.getKey(), itemOf(side.getValue()));
    }
    return Optional.of(
        fixed
            .stack()
            .withComponent(POT_DECORATIONS, new ComponentValue(CompoundTag.of(decorations))));
  }

  private static NbtTag itemOf(ItemStack stack) {
    SequencedMap<String, NbtTag> item = new LinkedHashMap<>();
    item.put("id", new StringTag(stack.type().id().toString()));
    return CompoundTag.of(item);
  }

  /** RC-R43: a patterned banner and a blank one of the same color give a copy of the first. */
  private static Optional<ItemStack> bannerDuplicate(SpecialRecipe recipe, List<ItemStack> cells) {
    if (cells.size() != 2 || !GridStacks.allMatch(cells, part(recipe, "banner"))) {
      return Optional.empty();
    }
    int first = GridStacks.listSize(cells.get(0), BANNER_PATTERNS);
    int second = GridStacks.listSize(cells.get(1), BANNER_PATTERNS);
    boolean sameColor = cells.get(0).type().id().equals(cells.get(1).type().id());
    boolean oneBlank = (first == 0) != (second == 0);
    if (!sameColor || !oneBlank || Math.max(first, second) > MAX_BANNER_PATTERNS) {
      return Optional.empty();
    }
    return Optional.of((first > 0 ? cells.get(0) : cells.get(1)).withCount(1));
  }

  /** RC-R44: one signed book and n blank ones give n copies one generation further. */
  private static Optional<ItemStack> bookCloning(SpecialRecipe recipe, List<ItemStack> cells) {
    OptionalInt source = bookSource(recipe, cells);
    if (source.isEmpty()) {
      return Optional.empty();
    }
    ItemStack book = cells.get(source.getAsInt());
    int copies = GridStacks.without(cells, source.getAsInt()).size();
    int generation = GridStacks.intField(book, BOOK, "generation", 0);
    return Optional.of(copyOfBook(recipe, book, copies, generation + 1));
  }

  /** The cell of the book to clone among cells that may include empty ones, if the cells fit. */
  private static OptionalInt bookSource(SpecialRecipe recipe, List<ItemStack> cells) {
    for (int i = 0; i < cells.size(); i++) {
      ItemStack source = cells.get(i);
      if (source.isEmpty()) {
        continue;
      }
      List<ItemStack> blanks =
          GridStacks.without(cells, i).stream().filter(s -> !s.isEmpty()).toList();
      int generation = GridStacks.intField(source, BOOK, "generation", 0);
      boolean fits =
          part(recipe, "source").matches(source)
              && generation < MAX_BOOK_GENERATION
              && !blanks.isEmpty()
              && GridStacks.allMatch(blanks, part(recipe, "material"));
      if (fits) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  /**
   * The cell whose stack the recipe gives back (RC-R43: the banner to copy, RC-R44: the signed
   * book), if the kind gives one back.
   *
   * @param recipe the recipe that matched the grid
   * @param grid the grid
   * @return the index of the cell in row-major order
   */
  static OptionalInt keptCell(SpecialRecipe recipe, CraftingGrid grid) {
    return switch (recipe.kind()) {
      case BOOK_CLONING -> bookSource(recipe, grid.stacks());
      case BANNER_DUPLICATE ->
          IntStream.range(0, grid.stacks().size())
              .filter(i -> GridStacks.listSize(grid.stacks().get(i), BANNER_PATTERNS) > 0)
              .findFirst();
      default -> OptionalInt.empty();
    };
  }

  private static ItemStack copyOfBook(
      SpecialRecipe recipe, ItemStack source, int copies, int generation) {
    SequencedMap<String, NbtTag> content =
        new LinkedHashMap<>(GridStacks.compound(source, BOOK).entries());
    content.put("generation", new IntTag(generation));
    ItemType type =
        recipe.result() instanceof RecipeResult.Fixed fixed ? fixed.stack().type() : source.type();
    return GridStacks.carry(source, type, copies, ComponentPatch.empty())
        .withComponent(BOOK, new ComponentValue(CompoundTag.of(content)));
  }

  /** RC-R49b: a shield and a banner give the shield with the color and patterns of the banner. */
  private static Optional<ItemStack> shieldDecoration(SpecialRecipe recipe, List<ItemStack> cells) {
    if (cells.size() != 2) {
      return Optional.empty();
    }
    for (int i = 0; i < 2; i++) {
      ItemStack shield = cells.get(i);
      ItemStack banner = cells.get(1 - i);
      String color = bannerColor(banner.type());
      boolean fits =
          part(recipe, "target").matches(shield)
              && part(recipe, "banner").matches(banner)
              && !color.isEmpty()
              && shield.component(BASE_COLOR).isEmpty();
      if (fits) {
        ComponentValue patterns =
            banner.component(BANNER_PATTERNS).orElse(new ComponentValue(ListTag.empty()));
        return Optional.of(
            shield
                .withCount(1)
                .withComponent(BASE_COLOR, new ComponentValue(new StringTag(color)))
                .withComponent(BANNER_PATTERNS, patterns));
      }
    }
    return Optional.empty();
  }

  /** The color of a banner item, from its identifier ({@code white_banner} is white). */
  private static String bannerColor(ItemType banner) {
    String path = banner.id().path();
    return path.endsWith("_banner") ? path.substring(0, path.length() - "_banner".length()) : "";
  }
}
