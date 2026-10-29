package dev.shoko.minecraft.item.recipe.codec;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.catalog.RegistryCatalog;
import dev.shoko.minecraft.data.catalog.StaticRegistryData;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.loader.DatapackLoader;
import dev.shoko.minecraft.data.loader.LoadRequest;
import dev.shoko.minecraft.data.loader.LoadedData;
import dev.shoko.minecraft.data.pack.Pack;
import dev.shoko.minecraft.data.pack.PackDiscovery;
import dev.shoko.minecraft.data.pack.PackStack;
import dev.shoko.minecraft.data.pack.meta.FeatureFlags;
import dev.shoko.minecraft.data.pack.meta.PackType;
import dev.shoko.minecraft.item.recipe.BrewingRecipe;
import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.RecipeCategory;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTransformRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTrimRecipe;
import dev.shoko.minecraft.item.recipe.SpecialKind;
import dev.shoko.minecraft.item.recipe.SpecialRecipe;
import dev.shoko.minecraft.item.recipe.StonecuttingRecipe;
import dev.shoko.minecraft.item.recipe.TransmuteRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.CountRange;
import dev.shoko.minecraft.item.recipe.ingredient.Ingredient;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.ReferenceCache;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The recipes of the reference data (2042 files, 22 serializers) through {@code RecipeLoader}:
 * counts of item-recipes.md RC-R2, RC-R3, RC-R14, RC-R25, RC-R31, RC-R39 and vectors V-R1 to V-R9.
 * The files are read from the local cache and never copied; every test is skipped when it is
 * absent.
 */
@Tag("oracle")
class RecipesOracleTest {

  private record Loaded(LoadedData data, RecipeLoadResult result) {

    RecipeSet recipes() {
      return result.recipes();
    }

    Recipe recipe(String path) {
      return recipes().get(Identifier.minecraft(path)).orElseThrow();
    }
  }

  private static Loaded load() throws IOException {
    Path reference = ReferenceCache.require();
    ItemCatalog items = Findings.value(ItemCatalog.load(ReferenceCache.itemReport()));
    RegistryCatalog catalog =
        RegistryCatalog.read(
                ReferenceCache.json(reference.resolve("generated/reports/datapack.json")))
            .value()
            .orElseThrow();
    List<StaticRegistryData> statics =
        StaticRegistryData.read(
                ReferenceCache.json(reference.resolve("generated/reports/registries.json")))
            .value()
            .orElseThrow();
    List<Pack> available =
        PackDiscovery.referencePacks(reference.resolve("resources")).value().orElseThrow();
    List<Pack> chosen = available.stream().filter(p -> p.id().equals("vanilla")).toList();
    LoadedData data =
        DatapackLoader.load(
                new LoadRequest(
                    PackStack.of(PackType.DATA, chosen), catalog, statics, FeatureFlags.vanilla()))
            .value()
            .orElseThrow();
    RecipeLoadResult result =
        RecipeLoader.load(
            data.elementRegistry(Identifier.minecraft("recipe")).orElseThrow(),
            data.registryAccess(),
            items);
    return new Loaded(data, result);
  }

  private static long count(RecipeSet set, Class<? extends Recipe> type) {
    return set.all().stream().filter(type::isInstance).count();
  }

  private static long special(RecipeSet set, SpecialKind kind) {
    return set.all().stream().filter(r -> r instanceof SpecialRecipe s && s.kind() == kind).count();
  }

  private static ItemStack fixed(RecipeResult result) {
    assertThat(result).isInstanceOf(RecipeResult.Fixed.class);
    return ((RecipeResult.Fixed) result).stack();
  }

  @Test
  void everyRecipeFileLoadsWithoutAFinding() throws IOException {
    // spec: RC-R2 (2042 recipes), RC-R3
    Loaded loaded = load();
    assertThat(loaded.result().findings()).isEmpty();
    assertThat(loaded.recipes().size()).isEqualTo(2042);
  }

  @Test
  void theSerializerCountsMatchTheTable() throws IOException {
    // spec: RC-R3 (827 shaped, 375 shapeless, 351 stonecutting, 279 brewing, 73 smelting, ...)
    RecipeSet set = load().recipes();
    assertThat(count(set, ShapedRecipe.class)).isEqualTo(827);
    assertThat(count(set, ShapelessRecipe.class)).isEqualTo(375);
    assertThat(count(set, StonecuttingRecipe.class)).isEqualTo(351);
    assertThat(count(set, BrewingRecipe.class)).isEqualTo(279);
    assertThat(count(set, TransmuteRecipe.class)).isEqualTo(33);
    assertThat(count(set, SmithingTrimRecipe.class)).isEqualTo(18);
    assertThat(count(set, SmithingTransformRecipe.class)).isEqualTo(12);
    assertThat(count(set, CookingRecipe.class)).isEqualTo(73 + 25 + 9 + 9);
    assertThat(count(set, SpecialRecipe.class)).isEqualTo(16 + 6 + 8 + 1);
  }

  @Test
  void theSpecialKindsMatchTheTable() throws IOException {
    // spec: RC-R3 (bannerduplicate 16, dye 6, one each for the others)
    RecipeSet set = load().recipes();
    assertThat(special(set, SpecialKind.BANNER_DUPLICATE)).isEqualTo(16);
    assertThat(special(set, SpecialKind.DYE)).isEqualTo(6);
    for (SpecialKind kind : SpecialKind.values()) {
      if (kind != SpecialKind.BANNER_DUPLICATE && kind != SpecialKind.DYE) {
        assertThat(special(set, kind)).as("%s", kind).isEqualTo(1);
      }
    }
  }

  @Test
  void provisionalTheMachinesGroupTheSerializers() throws IOException {
    // spec: RC-R2, RC-R4 (crafting 1266, smelting 73, blasting 25, smoking 9, campfire 9,
    // stonecutting 351, smithing 30, brewing 279)
    RecipeSet set = load().recipes();
    Map<RecipeType, Integer> expected = new EnumMap<>(RecipeType.class);
    expected.put(RecipeType.CRAFTING, 827 + 375 + 33 + 31);
    expected.put(RecipeType.SMELTING, 73);
    expected.put(RecipeType.BLASTING, 25);
    expected.put(RecipeType.SMOKING, 9);
    expected.put(RecipeType.CAMPFIRE_COOKING, 9);
    expected.put(RecipeType.STONECUTTING, 351);
    expected.put(RecipeType.SMITHING, 30);
    expected.put(RecipeType.BREWING, 279);
    for (RecipeType type : RecipeType.values()) {
      assertThat(set.ofType(type)).as("%s", type).hasSize(expected.get(type));
    }
  }

  @Test
  void theShapedDimensionsFollowTheDistribution() throws IOException {
    // spec: RC-R14 (height x width: 3x3 347, 2x3 161, 1x3 121, 2x2 83, 1x2 35, 3x2 30, 2x1 29,
    // 3x1 21)
    Map<String, Integer> dimensions = new TreeMap<>();
    for (Recipe recipe : load().recipes().all()) {
      if (recipe instanceof ShapedRecipe shaped) {
        dimensions.merge(
            shaped.pattern().height() + "x" + shaped.pattern().width(), 1, Integer::sum);
        assertThat(shaped.pattern().cells().stream().anyMatch(cell -> !cell.isNone())).isTrue();
      }
    }
    assertThat(dimensions)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "3x3", 347, "2x3", 161, "1x3", 121, "2x2", 83, "1x2", 35, "3x2", 30, "2x1", 29,
                "3x1", 21));
  }

  @Test
  void theBoatDoorAndTableVectorsAreRead() throws IOException {
    // spec: V-R2, V-R3, V-R4
    Loaded loaded = load();
    ShapedRecipe boat = (ShapedRecipe) loaded.recipe("acacia_boat");
    assertThat(boat.group()).isEqualTo("boat");
    assertThat(boat.pattern().width()).isEqualTo(3);
    assertThat(boat.pattern().height()).isEqualTo(2);
    assertThat(RecipeDescriber.ingredient(boat.pattern().cell(0, 0)))
        .isEqualTo("[minecraft:acacia_planks]");
    assertThat(boat.pattern().cell(1, 0).isNone()).isTrue();
    assertThat(fixed(boat.result()).type().id()).isEqualTo(Identifier.minecraft("acacia_boat"));
    assertThat(fixed(boat.result()).count()).isEqualTo(1);
    ShapedRecipe door = (ShapedRecipe) loaded.recipe("acacia_door");
    assertThat(door.category()).isEqualTo(RecipeCategory.REDSTONE);
    assertThat(door.group()).isEqualTo("wooden_door");
    assertThat(fixed(door.result()).count()).isEqualTo(3);
    ShapedRecipe table = (ShapedRecipe) loaded.recipe("crafting_table");
    assertThat(table.showNotification()).isFalse();
    assertThat(RecipeDescriber.ingredient(table.pattern().cell(1, 1)))
        .isEqualTo("#minecraft:planks");
    assertThat(table.pattern().width()).isEqualTo(2);
    assertThat(table.pattern().height()).isEqualTo(2);
  }

  @Test
  void thePlanksAndPotatoVectorsAreRead() throws IOException {
    // spec: V-R5, V-R6
    Loaded loaded = load();
    ShapelessRecipe planks = (ShapelessRecipe) loaded.recipe("acacia_planks");
    assertThat(planks.ingredients()).hasSize(1);
    assertThat(RecipeDescriber.ingredient(planks.ingredients().getFirst()))
        .isEqualTo("#minecraft:acacia_logs");
    assertThat(fixed(planks.result()).count()).isEqualTo(4);
    CookingRecipe baked = (CookingRecipe) loaded.recipe("baked_potato");
    assertThat(baked.type()).isEqualTo(RecipeType.SMELTING);
    assertThat(baked.cookingTime()).isEqualTo(200);
    assertThat(baked.experience()).isEqualTo(0.35f);
    CookingRecipe smoked = (CookingRecipe) loaded.recipe("baked_potato_from_smoking");
    assertThat(smoked.type()).isEqualTo(RecipeType.SMOKING);
    assertThat(smoked.cookingTime()).isEqualTo(200);
    CookingRecipe campfire = (CookingRecipe) loaded.recipe("baked_potato_from_campfire_cooking");
    assertThat(campfire.type()).isEqualTo(RecipeType.CAMPFIRE_COOKING);
    assertThat(campfire.cookingTime()).isEqualTo(600);
  }

  @Test
  void theSmithingBrewingAndMapCloningVectorsAreRead() throws IOException {
    // spec: V-R7, V-R8, V-R9
    Loaded loaded = load();
    SmithingTransformRecipe axe = (SmithingTransformRecipe) loaded.recipe("netherite_axe_smithing");
    assertThat(RecipeDescriber.ingredient(axe.template()))
        .isEqualTo("[minecraft:netherite_upgrade_smithing_template]");
    assertThat(RecipeDescriber.ingredient(axe.base())).isEqualTo("[minecraft:diamond_axe]");
    assertThat(RecipeDescriber.ingredient(axe.addition()))
        .isEqualTo("#minecraft:netherite_tool_materials");
    BrewingRecipe brewing = (BrewingRecipe) loaded.recipe("brewing/potion_awkward_blaze_powder");
    assertThat(RecipeDescriber.ingredient(brewing.input().item())).isEqualTo("[minecraft:potion]");
    assertThat(RecipeDescriber.ingredient(brewing.reagent())).isEqualTo("[minecraft:blaze_powder]");
    assertThat(brewing.output().type().id()).isEqualTo(Identifier.minecraft("potion"));
    TransmuteRecipe cloning = (TransmuteRecipe) loaded.recipe("map_cloning");
    assertThat(RecipeDescriber.ingredient(cloning.input())).isEqualTo("#minecraft:clonable_maps");
    assertThat(RecipeDescriber.ingredient(cloning.material())).isEqualTo("[minecraft:map]");
    assertThat(cloning.materialCount()).isEqualTo(new CountRange(1, 8));
    assertThat(cloning.addMaterialCountToResult()).isTrue();
    assertThat(cloning.result()).isInstanceOf(RecipeResult.Derived.class);
  }

  @Test
  void theBrewingRecipesFollowTheDistribution() throws IOException {
    // spec: RC-R39 (inputs: potion 108, splash_potion 108, lingering_potion 63; outputs:
    // lingering_potion 108, splash_potion 108, potion 63)
    Map<String, Integer> inputs = new TreeMap<>();
    Map<String, Integer> outputs = new TreeMap<>();
    for (Recipe recipe : load().recipes().all()) {
      if (recipe instanceof BrewingRecipe brewing) {
        inputs.merge(RecipeDescriber.ingredient(brewing.input().item()), 1, Integer::sum);
        outputs.merge(brewing.output().type().id().path(), 1, Integer::sum);
      }
    }
    assertThat(inputs)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "[minecraft:potion]", 108,
                "[minecraft:splash_potion]", 108,
                "[minecraft:lingering_potion]", 63));
    assertThat(outputs)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of("lingering_potion", 108, "splash_potion", 108, "potion", 63));
  }

  @Test
  void cookingTimesAreTwoHundredExceptOnTheCampfire() throws IOException {
    // spec: RC-R25 (200 for 107 recipes of furnace, blast furnace and smoker, 600 for 9 campfire)
    for (Recipe recipe : load().recipes().all()) {
      if (recipe instanceof CookingRecipe cooking) {
        int expected = cooking.type() == RecipeType.CAMPFIRE_COOKING ? 600 : 200;
        assertThat(cooking.cookingTime()).as("%s", cooking.id()).isEqualTo(expected);
        assertThat(cooking.experience()).as("%s", cooking.id()).isGreaterThanOrEqualTo(0f);
      }
    }
  }

  @Test
  void noItemIsCookedByTwoRecipesOfOneMachine() throws IOException {
    // spec: RC-R31 (0 shared items for smelting, blasting, smoking and campfire_cooking, tags
    // expanded)
    Loaded loaded = load();
    Map<RecipeType, Set<Identifier>> seen = new EnumMap<>(RecipeType.class);
    List<String> shared = new ArrayList<>();
    for (Recipe recipe : loaded.recipes().all()) {
      if (recipe instanceof CookingRecipe cooking) {
        Set<Identifier> items = seen.computeIfAbsent(cooking.type(), t -> new HashSet<>());
        for (Identifier id : accepted(cooking.ingredient(), loaded.data())) {
          if (!items.add(id)) {
            shared.add(cooking.type() + " " + id + " in " + cooking.id());
          }
        }
      }
    }
    assertThat(shared).isEmpty();
  }

  private static Set<Identifier> accepted(Ingredient ingredient, LoadedData data) {
    return RecipeDescriber.expand(ingredient, data.registryAccess());
  }

  @Test
  void noResultExceedsItsStackSize() throws IOException {
    // spec: RC-R24 (counts seen: 1, 2, 3, 4, 6, 8, 9, 16)
    Set<Integer> counts = new HashSet<>();
    for (Recipe recipe : load().recipes().all()) {
      ItemStack stack = resultOf(recipe);
      if (stack != null) {
        assertThat(stack.count()).as("%s", recipe.id()).isBetween(1, stack.maxStackSize());
        counts.add(stack.count());
      }
    }
    assertThat(counts).contains(1, 2, 3, 4, 6, 8, 9, 16);
  }

  private static ItemStack resultOf(Recipe recipe) {
    return switch (recipe) {
      case ShapedRecipe r -> r.result() instanceof RecipeResult.Fixed f ? f.stack() : null;
      case ShapelessRecipe r -> r.result() instanceof RecipeResult.Fixed f ? f.stack() : null;
      case TransmuteRecipe r -> r.result() instanceof RecipeResult.Fixed f ? f.stack() : null;
      case SpecialRecipe r -> r.result() instanceof RecipeResult.Fixed f ? f.stack() : null;
      case CookingRecipe r -> r.result();
      case StonecuttingRecipe r -> r.result();
      case SmithingTransformRecipe r -> r.result();
      case BrewingRecipe r -> r.output();
      case SmithingTrimRecipe ignored -> null;
    };
  }

  @Test
  void theResultsOfTheTwoDerivedRecipesAreDerived() throws IOException {
    // spec: RC-R12 (map_cloning and map_extending have {}; repairitem and the 18 trims no result)
    Loaded loaded = load();
    assertThat(((SpecialRecipe) loaded.recipe("map_extending")).result())
        .isInstanceOf(RecipeResult.Derived.class);
    assertThat(((SpecialRecipe) loaded.recipe("repair_item")).parts()).isEmpty();
  }
}
