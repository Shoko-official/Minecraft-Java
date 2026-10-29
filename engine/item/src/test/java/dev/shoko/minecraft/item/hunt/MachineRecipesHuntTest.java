package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Holder;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.recipe.BrewingRecipe;
import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.SmithingTransformRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTrimRecipe;
import dev.shoko.minecraft.item.recipe.StonecuttingRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.PotionMatch;
import dev.shoko.minecraft.item.recipe.match.BrewingLookup;
import dev.shoko.minecraft.item.recipe.match.CookingLookup;
import dev.shoko.minecraft.item.recipe.match.SmithingLookup;
import dev.shoko.minecraft.item.recipe.match.SmithingMatch;
import dev.shoko.minecraft.item.recipe.match.StonecutterLookup;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("oracle")
class MachineRecipesHuntTest {

  private static ItemStack pick(RealData.Loaded data, Ingredient ingredient, boolean last) {
    List<Holder<ItemType>> holders = ingredient.items().holders();
    Holder<ItemType> holder = last ? holders.getLast() : holders.getFirst();
    Identifier id = holder.id().orElseGet(() -> holder.value().id());
    return ItemStack.of(data.items().find(id).orElseThrow(), 1);
  }

  @Test
  void everyCookingRecipeIsTheOneFoundForEachItemOfItsIngredient() {
    RealData.Loaded data = RealData.get();
    CookingLookup lookup = CookingLookup.of(data.recipes().recipes());
    List<String> problems = new ArrayList<>();
    for (RecipeType machine :
        List.of(
            RecipeType.SMELTING,
            RecipeType.BLASTING,
            RecipeType.SMOKING,
            RecipeType.CAMPFIRE_COOKING)) {
      for (Recipe recipe : data.recipes().recipes().ofType(machine)) {
        CookingRecipe cooking = (CookingRecipe) recipe;
        for (Holder<ItemType> holder : cooking.ingredient().items().holders()) {
          Identifier id = holder.id().orElseGet(() -> holder.value().id());
          ItemStack input = ItemStack.of(data.items().find(id).orElseThrow(), 1);
          Optional<CookingRecipe> found = lookup.find(machine, input);
          if (found.isEmpty() || !found.get().id().equals(recipe.id())) {
            problems.add(machine + " " + id + " -> " + found.map(Recipe::id));
          }
          ItemStack renamed =
              input.withComponent(
                  Identifier.minecraft("custom_name"), new ComponentValue(new StringTag("x")));
          if (lookup.find(machine, renamed).isEmpty()) {
            problems.add("components matter: " + machine + " " + id);
          }
        }
      }
    }
    assertThat(problems).isEmpty();
  }

  @Test
  void everySmithingRecipeIsFoundOnItsCanonicalTriple() {
    RealData.Loaded data = RealData.get();
    SmithingLookup lookup = SmithingLookup.of(data.recipes().recipes(), data.items());
    List<String> problems = new ArrayList<>();
    for (Recipe recipe : data.recipes().recipes().ofType(RecipeType.SMITHING)) {
      Ingredient template;
      Ingredient base;
      Ingredient addition;
      if (recipe instanceof SmithingTransformRecipe t) {
        template = t.template();
        base = t.base();
        addition = t.addition();
      } else {
        SmithingTrimRecipe t = (SmithingTrimRecipe) recipe;
        template = t.template();
        base = t.base();
        addition = t.addition();
      }
      for (boolean last : new boolean[] {false, true}) {
        Optional<SmithingMatch> match =
            lookup.find(
                pick(data, template, last), pick(data, base, last), pick(data, addition, last));
        if (match.isEmpty() || !match.get().recipe().id().equals(recipe.id())) {
          problems.add(recipe.id() + " last=" + last + " -> " + match.map(m -> m.recipe().id()));
        }
      }
    }
    assertThat(problems).isEmpty();
  }

  @Test
  void everyStonecuttingRecipeIsOfferedForItsIngredient() {
    RealData.Loaded data = RealData.get();
    StonecutterLookup lookup = StonecutterLookup.of(data.recipes().recipes());
    List<String> problems = new ArrayList<>();
    for (Recipe recipe : data.recipes().recipes().ofType(RecipeType.STONECUTTING)) {
      StonecuttingRecipe cutting = (StonecuttingRecipe) recipe;
      List<StonecuttingRecipe> offered = lookup.recipesFor(pick(data, cutting.ingredient(), false));
      if (offered.stream().noneMatch(r -> r.id().equals(recipe.id()))) {
        problems.add(recipe.id().toString());
      }
    }
    assertThat(problems).isEmpty();
  }

  @Test
  void everyBrewingRecipeTurnsItsInputIntoItsOutputWithoutKeepingTheInputPotion() {
    RealData.Loaded data = RealData.get();
    BrewingLookup lookup = BrewingLookup.of(data.recipes().recipes());
    List<String> problems = new ArrayList<>();
    Identifier potionContents = WellKnownComponents.POTION_CONTENTS;
    for (Recipe recipe : data.recipes().recipes().ofType(RecipeType.BREWING)) {
      BrewingRecipe brewing = (BrewingRecipe) recipe;
      ItemStack bottle = pick(data, brewing.input().item(), false);
      if (brewing.input().potion() instanceof PotionMatch.Exact exact) {
        LinkedHashMap<String, dev.shoko.minecraft.core.nbt.NbtTag> contents = new LinkedHashMap<>();
        contents.put("potion", new StringTag(exact.potion().toString()));
        bottle = bottle.withComponent(potionContents, new ComponentValue(CompoundTag.of(contents)));
      }
      ItemStack reagent = pick(data, brewing.reagent(), false);
      Optional<BrewingRecipe> found = lookup.find(bottle, reagent);
      if (found.isEmpty() || !found.get().id().equals(recipe.id())) {
        problems.add("find " + recipe.id() + " -> " + found.map(Recipe::id));
        continue;
      }
      ItemStack out = lookup.brew(brewing, bottle);
      if (!out.type().id().equals(brewing.output().type().id())) {
        problems.add("type " + recipe.id());
      }
      if (!out.component(potionContents).equals(brewing.output().component(potionContents))) {
        problems.add("contents " + recipe.id() + " " + out + " vs " + brewing.output());
      }
    }
    assertThat(problems).isEmpty();
  }
}
