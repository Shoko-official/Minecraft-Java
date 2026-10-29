package dev.shoko.minecraft.item.recipe.codec;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.recipe.BrewingRecipe;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.SmithingTransformRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTrimRecipe;
import dev.shoko.minecraft.item.recipe.StonecuttingRecipe;
import dev.shoko.minecraft.item.recipe.TransmuteRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.CountRange;
import dev.shoko.minecraft.item.recipe.ingredient.PotionMatch;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestAccess;
import dev.shoko.minecraft.item.testing.TestItems;
import org.junit.jupiter.api.Test;

// spec: RC-R3, RC-R10, RC-R12, RC-R36 to RC-R38, RC-R49f, V-R7 to V-R9: stonecutting, smithing,
// brewing and transmute files
class RecipeCodecOtherTest {

  private static String potionOf(ItemStack stack) {
    var value = stack.component(WellKnownComponents.POTION_CONTENTS).orElseThrow();
    String json =
        TestItems.componentCodec().valueToJson(WellKnownComponents.POTION_CONTENTS, value);
    JsonObject object = (JsonObject) Json.parseStrict(json);
    return ((JsonString) object.get("potion").orElseThrow()).value();
  }

  @Test
  void aStonecuttingFileKeepsIngredientAndResult() {
    // spec: RC-R38 (ingredient and result with a count)
    StonecuttingRecipe recipe =
        Recipes.recipe(
            StonecuttingRecipe.class,
            "slab_from_stone",
            "{'type':'minecraft:stonecutting','ingredient':'minecraft:stone',"
                + "'result':{'count':2,'id':'minecraft:stone_slab'}}");
    assertThat(recipe.ingredient().matches(TestItems.stack("stone"))).isTrue();
    assertThat(recipe.ingredient().matches(TestItems.stack("dirt"))).isFalse();
    assertThat(recipe.result().type()).isEqualTo(TestItems.type("stone_slab"));
    assertThat(recipe.result().count()).isEqualTo(2);
  }

  @Test
  void provisionalAStonecuttingResultWithoutCountIsOne() {
    // spec: RC-R28 (count absent: 1)
    StonecuttingRecipe recipe =
        Recipes.recipe(
            StonecuttingRecipe.class,
            "stairs",
            "{'type':'minecraft:stonecutting','ingredient':'minecraft:stone',"
                + "'result':{'id':'minecraft:stone_stairs'}}");
    assertThat(recipe.result().count()).isEqualTo(1);
  }

  @Test
  void aSmithingTransformFileKeepsTemplateBaseAdditionAndResult() {
    // spec: V-R7 (netherite upgrade: template, base tool, addition tag)
    SmithingTransformRecipe recipe =
        Recipes.recipe(
            SmithingTransformRecipe.class,
            "netherite_axe_smithing",
            "{'type':'minecraft:smithing_transform',"
                + "'addition':'#minecraft:netherite_tool_materials','base':'minecraft:diamond_axe',"
                + "'result':{'id':'minecraft:netherite_axe'},"
                + "'template':'minecraft:netherite_upgrade_smithing_template'}");
    assertThat(recipe.template().matches(TestItems.stack("netherite_upgrade_smithing_template")))
        .isTrue();
    assertThat(recipe.base().matches(TestItems.stack("diamond_axe"))).isTrue();
    assertThat(recipe.base().matches(TestItems.stack("netherite_axe"))).isFalse();
    assertThat(recipe.addition().items().tagKey().orElseThrow().id())
        .isEqualTo(TestItems.id("netherite_tool_materials"));
    assertThat(recipe.addition().matches(TestItems.stack("netherite_ingot"))).isTrue();
    assertThat(recipe.result().type()).isEqualTo(TestItems.type("netherite_axe"));
  }

  @Test
  void aSmithingTrimFileHasNoResultField() {
    // spec: RC-R12 (the 18 smithing_trim files have no result), RC-R37
    SmithingTrimRecipe recipe =
        Recipes.recipe(
            SmithingTrimRecipe.class,
            "bolt_trim",
            "{'type':'minecraft:smithing_trim','addition':'#minecraft:trim_materials',"
                + "'base':'#minecraft:trimmable_armor','pattern':'minecraft:bolt',"
                + "'template':'minecraft:bolt_armor_trim_smithing_template'}");
    assertThat(recipe.pattern()).isEqualTo(TestItems.id("bolt"));
    assertThat(recipe.template().matches(TestItems.stack("bolt_armor_trim_smithing_template")))
        .isTrue();
    assertThat(recipe.base().matches(TestItems.stack("iron_helmet"))).isTrue();
    assertThat(recipe.base().matches(TestItems.stack("stone"))).isFalse();
    assertThat(recipe.addition().matches(TestItems.stack("emerald"))).isTrue();
  }

  @Test
  void aBrewingFileKeepsInputPotionReagentAndOutput() {
    // spec: V-R8, RC-R10 (potion + awkward + blaze powder gives a potion of strength)
    BrewingRecipe recipe =
        Recipes.recipe(
            BrewingRecipe.class,
            "brewing/potion_awkward_blaze_powder",
            "{'type':'minecraft:brewing',"
                + "'input':{'item':'minecraft:potion','potion_contents':{'potions':'minecraft:awkward'}},"
                + "'output':{'components':{'minecraft:potion_contents':{'potion':'minecraft:strength'}},"
                + "'id':'minecraft:potion'},'reagent':{'item':'minecraft:blaze_powder'}}");
    assertThat(recipe.id()).isEqualTo(TestItems.id("brewing/potion_awkward_blaze_powder"));
    assertThat(recipe.input().item().matches(TestItems.stack("potion"))).isTrue();
    assertThat(recipe.input().item().matches(TestItems.stack("splash_potion"))).isFalse();
    assertThat(recipe.input().potion()).isEqualTo(new PotionMatch.Exact(TestItems.id("awkward")));
    assertThat(recipe.reagent().matches(TestItems.stack("blaze_powder"))).isTrue();
    assertThat(recipe.output().type()).isEqualTo(TestItems.type("potion"));
    assertThat(potionOf(recipe.output())).isEqualTo("minecraft:strength");
  }

  @Test
  void aBrewingInputWithoutPotionContentsPutsNoConditionOnThePotion() {
    // spec: RC-R10 (potion_contents is optional in input)
    BrewingRecipe recipe =
        Recipes.recipe(
            BrewingRecipe.class,
            "brewing/any",
            "{'type':'minecraft:brewing','input':{'item':'minecraft:glass_bottle'},"
                + "'output':{'id':'minecraft:potion'},'reagent':{'item':'minecraft:nether_wart'}}");
    assertThat(recipe.input().potion()).isInstanceOf(PotionMatch.Any.class);
  }

  @Test
  void provisionalAMissingBrewingFieldIsReported() {
    // spec: RC-R10 (all 279 recipes have input, reagent and output)
    Findings.assertFailure(
        Recipes.decode(
            "brewing/no_reagent",
            "{'type':'minecraft:brewing','input':{'item':'minecraft:potion'},"
                + "'output':{'id':'minecraft:potion'}}"),
        FindingCode.MISSING_FIELD);
  }

  @Test
  void aMapCloningFileHasADerivedResultAndARangeOfMaterials() {
    // spec: V-R9, RC-R12, RC-R49f (crafting_transmute, material_count 1 to 8)
    TransmuteRecipe recipe =
        decodeTransmute(
            "map_cloning",
            "{'type':'minecraft:crafting_transmute','add_material_count_to_result':true,"
                + "'group':'map_cloning','input':'#minecraft:clonable_maps',"
                + "'material':'minecraft:map','material_count':{'max':8,'min':1},'result':{}}");
    assertThat(recipe.result()).isInstanceOf(RecipeResult.Derived.class);
    assertThat(recipe.materialCount()).isEqualTo(new CountRange(1, 8));
    assertThat(recipe.addMaterialCountToResult()).isTrue();
    assertThat(recipe.group()).isEqualTo("map_cloning");
    assertThat(recipe.input().matches(TestItems.stack("map"))).isTrue();
    assertThat(recipe.material().matches(TestItems.stack("map"))).isTrue();
  }

  private static TransmuteRecipe decodeTransmute(String path, String json) {
    TestAccess access = TestAccess.standard().withTag("clonable_maps", "map");
    Recipe recipe = Findings.value(Recipes.decode(path, json, access));
    assertThat(recipe).isInstanceOf(TransmuteRecipe.class);
    return (TransmuteRecipe) recipe;
  }

  @Test
  void provisionalATransmuteFileWithoutCountFieldsTakesTheDefaults() {
    // spec: RC-R49f (material_count absent: 1 to 1, add_material_count_to_result absent: false)
    TransmuteRecipe recipe =
        Recipes.recipe(
            TransmuteRecipe.class,
            "black_chest",
            "{'type':'minecraft:crafting_transmute','group':'chests','input':'minecraft:chest',"
                + "'material':'minecraft:coal','result':{'id':'minecraft:chest'}}");
    assertThat(recipe.materialCount()).isEqualTo(new CountRange(1, 1));
    assertThat(recipe.addMaterialCountToResult()).isFalse();
    assertThat(recipe.result()).isInstanceOf(RecipeResult.Fixed.class);
  }

  @Test
  void provisionalASingleMaterialCountGivesARangeWithEqualBounds() {
    // spec: RC-R49f, CountRange Javadoc (a single value gives equal bounds)
    TransmuteRecipe recipe =
        Recipes.recipe(
            TransmuteRecipe.class,
            "three",
            "{'type':'minecraft:crafting_transmute','input':'minecraft:chest',"
                + "'material':'minecraft:coal','material_count':3,"
                + "'result':{'id':'minecraft:chest'}}");
    assertThat(recipe.materialCount()).isEqualTo(new CountRange(3, 3));
  }
}
