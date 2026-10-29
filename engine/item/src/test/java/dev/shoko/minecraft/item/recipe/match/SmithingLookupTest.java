package dev.shoko.minecraft.item.recipe.match;

import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.rid;
import static dev.shoko.minecraft.item.recipe.match.MatchFixtures.set;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.SmithingTransformRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTrimRecipe;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestItems;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R36, RC-R37 (smithing lookups, provisional level S)
class SmithingLookupTest {

  private static RecipeSet recipes() {
    return set(
        new SmithingTransformRecipe(
            rid("netherite_axe_smithing"),
            Recipes.items("netherite_upgrade_smithing_template"),
            Recipes.items("diamond_axe"),
            Recipes.tag("netherite_tool_materials"),
            TestItems.stack("netherite_axe")),
        new SmithingTrimRecipe(
            rid("bolt_trim"),
            Recipes.items("bolt_armor_trim_smithing_template"),
            Recipes.tag("trimmable_armor"),
            Recipes.tag("trim_materials"),
            rid("bolt")));
  }

  private static SmithingLookup lookup() {
    return SmithingLookup.of(recipes(), TestItems.catalog());
  }

  private static ItemStack wornAndEnchantedAxe() {
    return TestItems.stack("diamond_axe", 1, "damage", "100")
        .withComponent(
            TestItems.id("enchantments"),
            TestItems.value("enchantments", "{'minecraft:sharpness':2}"));
  }

  private static JsonObject trimJson(ItemStack result) {
    ComponentValue trim = result.component(WellKnownComponents.TRIM).orElseThrow();
    String text = TestItems.componentCodec().valueToJson(WellKnownComponents.TRIM, trim);
    JsonNode node = Json.parseStrict(text);
    assertThat(node).isInstanceOf(JsonObject.class);
    return (JsonObject) node;
  }

  private static String field(JsonObject object, String name) {
    return ((JsonString) object.get(name).orElseThrow()).value();
  }

  @Test
  void provisionalATransformationReplacesTheItemAndKeepsAllComponentsOfTheBase() {
    // spec: RC-R36 (the result takes the components of the base, wear included)
    ItemStack base = wornAndEnchantedAxe();
    SmithingMatch match =
        lookup()
            .find(
                TestItems.stack("netherite_upgrade_smithing_template"),
                base,
                TestItems.stack("netherite_ingot"))
            .orElseThrow();
    assertThat(match.recipe().id()).isEqualTo(rid("netherite_axe_smithing"));
    ItemStack result = match.result();
    assertThat(result.type()).isEqualTo(TestItems.type("netherite_axe"));
    assertThat(result.count()).isEqualTo(1);
    assertThat(result.component(WellKnownComponents.DAMAGE))
        .isEqualTo(base.component(WellKnownComponents.DAMAGE));
    assertThat(result.component(WellKnownComponents.ENCHANTMENTS))
        .isEqualTo(base.component(WellKnownComponents.ENCHANTMENTS));
  }

  @Test
  void provisionalATransformationNeedsItsTemplateBaseAndAddition() {
    // spec: RC-R36
    SmithingLookup lookup = lookup();
    ItemStack template = TestItems.stack("netherite_upgrade_smithing_template");
    ItemStack axe = TestItems.stack("diamond_axe");
    ItemStack ingot = TestItems.stack("netherite_ingot");
    assertThat(lookup.find(template, axe, ingot)).isPresent();
    assertThat(lookup.find(TestItems.stack("paper"), axe, ingot)).as("template").isEmpty();
    assertThat(lookup.find(template, TestItems.stack("stone"), ingot)).as("base").isEmpty();
    assertThat(lookup.find(template, axe, TestItems.stack("diamond"))).as("addition").isEmpty();
  }

  @Test
  void provisionalAnEmptySlotGivesNoRecipe() {
    // spec: RC-R36, RC-R37
    SmithingLookup lookup = lookup();
    ItemStack template = TestItems.stack("netherite_upgrade_smithing_template");
    ItemStack axe = TestItems.stack("diamond_axe");
    ItemStack ingot = TestItems.stack("netherite_ingot");
    assertThat(lookup.find(ItemStack.empty(), axe, ingot)).isEmpty();
    assertThat(lookup.find(template, ItemStack.empty(), ingot)).isEmpty();
    assertThat(lookup.find(template, axe, ItemStack.empty())).isEmpty();
  }

  @Test
  void provisionalATrimSetsTheTrimComponentOnTheBase() {
    // spec: RC-R37 (the material comes from provides_trim_material, the pattern from the recipe;
    // the fields material and pattern are the shape of IC-R9b, itself provisional)
    ItemStack helmet = TestItems.stack("iron_helmet");
    SmithingMatch match =
        lookup()
            .find(
                TestItems.stack("bolt_armor_trim_smithing_template"),
                helmet,
                TestItems.stack("emerald"))
            .orElseThrow();
    assertThat(match.recipe().id()).isEqualTo(rid("bolt_trim"));
    ItemStack result = match.result();
    assertThat(result.type()).isEqualTo(TestItems.type("iron_helmet"));
    assertThat(result.count()).isEqualTo(1);
    JsonObject trim = trimJson(result);
    assertThat(field(trim, "material")).isEqualTo("minecraft:emerald");
    assertThat(field(trim, "pattern")).isEqualTo("minecraft:bolt");
  }

  @Test
  void provisionalATrimKeepsTheOtherComponentsOfTheBase() {
    // spec: RC-R37 (the result is the base with the trim component)
    ItemStack helmet =
        TestItems.stack("iron_helmet", 1, "enchantments", "{'minecraft:sharpness':1}");
    ItemStack result =
        lookup()
            .find(
                TestItems.stack("bolt_armor_trim_smithing_template"),
                helmet,
                TestItems.stack("emerald"))
            .orElseThrow()
            .result();
    assertThat(result.component(WellKnownComponents.ENCHANTMENTS))
        .isEqualTo(helmet.component(WellKnownComponents.ENCHANTMENTS));
    assertThat(result.component(WellKnownComponents.EQUIPPABLE))
        .isEqualTo(helmet.component(WellKnownComponents.EQUIPPABLE));
  }

  @Test
  void provisionalATrimNeedsATrimmableBaseAndATrimMaterial() {
    // spec: RC-R37 (base in trimmable_armor, addition in trim_materials)
    SmithingLookup lookup = lookup();
    ItemStack template = TestItems.stack("bolt_armor_trim_smithing_template");
    ItemStack helmet = TestItems.stack("iron_helmet");
    ItemStack emerald = TestItems.stack("emerald");
    assertThat(lookup.find(template, helmet, emerald)).isPresent();
    assertThat(lookup.find(template, TestItems.stack("stone"), emerald)).as("base").isEmpty();
    assertThat(lookup.find(template, helmet, TestItems.stack("diamond"))).as("addition").isEmpty();
    assertThat(lookup.find(TestItems.stack("paper"), helmet, emerald)).as("template").isEmpty();
  }

  @Test
  void provisionalATransformTemplateDoesNotTrim() {
    // spec: RC-R36, RC-R37 (each recipe has its own template)
    assertThat(
            lookup()
                .find(
                    TestItems.stack("netherite_upgrade_smithing_template"),
                    TestItems.stack("iron_helmet"),
                    TestItems.stack("emerald")))
        .isEmpty();
  }
}
