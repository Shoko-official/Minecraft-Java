package dev.shoko.minecraft.item.recipe.codec;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestAccess;
import org.junit.jupiter.api.Test;

// spec: RC-R1, RC-R9, RC-R13, RC-R15, RC-R24: recipes that must be refused, by finding code
class RecipeCodecErrorsTest {

  private static String shaped(String key, String pattern, String result) {
    return "{'type':'minecraft:crafting_shaped','key':"
        + key
        + ",'pattern':"
        + pattern
        + ",'result':"
        + result
        + "}";
  }

  private static String shapeless(String ingredients) {
    return "{'type':'minecraft:crafting_shapeless','ingredients':"
        + ingredients
        + ",'result':{'id':'minecraft:stone_slab'}}";
  }

  private static Decoding<Recipe> shapedWith(String key, String pattern) {
    return Recipes.decode("r", shaped(key, pattern, "{'id':'minecraft:chest'}"));
  }

  @Test
  void aTypeOutsideTheTwentyTwoSerializersIsUnknown() {
    // spec: RC-R1
    Findings.assertFailure(
        Recipes.decode(
            "r", "{'type':'minecraft:crafting_nonsense','result':{'id':'minecraft:stone'}}"),
        FindingCode.UNKNOWN_RECIPE_TYPE);
  }

  @Test
  void aFailureNeverCarriesAValueAndNamesAnErrorFinding() {
    // spec: Decoding Javadoc (a failure has at least one finding of severity ERROR)
    Decoding<Recipe> decoding = Recipes.decode("r", "{'type':'minecraft:unknown_thing'}");
    assertThat(decoding).isInstanceOf(Decoding.Failure.class);
    assertThat(decoding.findings()).anyMatch(f -> f.code().severity() == Severity.ERROR);
  }

  @Test
  void provisionalAMissingMandatoryFieldIsReported() {
    // spec: RC-R3 (result, key and pattern are in every shaped file)
    Findings.assertFailure(
        Recipes.decode(
            "r",
            "{'type':'minecraft:crafting_shaped','key':{'#':'minecraft:stone'},'pattern':['#']}"),
        FindingCode.MISSING_FIELD);
    Findings.assertFailure(
        Recipes.decode("r", "{'type':'minecraft:stonecutting','result':{'id':'minecraft:stone'}}"),
        FindingCode.MISSING_FIELD);
  }

  @Test
  void provisionalAnEmptyIngredientListIsRefused() {
    // spec: RC-R9 (an empty list is refused)
    Findings.assertFailure(Recipes.decode("r", shapeless("[[]]")), FindingCode.EMPTY_INGREDIENT);
    Findings.assertFailure(shapedWith("{'#':[]}", "['#']"), FindingCode.EMPTY_INGREDIENT);
    Findings.assertFailure(
        Recipes.decode(
            "r",
            "{'type':'minecraft:smelting','cookingtime':200,'experience':0.1,'ingredient':[],"
                + "'result':{'id':'minecraft:baked_potato'}}"),
        FindingCode.EMPTY_INGREDIENT);
  }

  @Test
  void anItemOutsideTheRegistryIsUnresolved() {
    // spec: RC-R9 (identifiers of the item registry)
    Findings.assertFailure(
        Recipes.decode("r", shapeless("['minecraft:no_such_item']")), FindingCode.UNRESOLVED_ITEM);
    Findings.assertFailure(
        shapedWith("{'#':'minecraft:no_such_item'}", "['#']"), FindingCode.UNRESOLVED_ITEM);
    Findings.assertFailure(
        Recipes.decode(
            "r",
            "{'type':'minecraft:stonecutting','ingredient':'minecraft:stone',"
                + "'result':{'id':'minecraft:no_such_item'}}"),
        FindingCode.UNRESOLVED_ITEM);
  }

  @Test
  void provisionalAListWithOneUnknownItemIsUnresolved() {
    // spec: RC-R9
    Findings.assertFailure(
        Recipes.decode("r", shapeless("[['minecraft:stone','minecraft:no_such_item']]")),
        FindingCode.UNRESOLVED_ITEM);
  }

  @Test
  void provisionalATagThatIsNotLoadedRejectsTheRecipe() {
    // spec: RC-R13 (an absent tag is a load error of that recipe)
    Findings.assertFailure(
        Recipes.decode("r", shapeless("['#minecraft:no_such_tag']")), FindingCode.UNRESOLVED_TAG);
    Findings.assertFailure(
        shapedWith("{'#':'#minecraft:no_such_tag'}", "['#']"), FindingCode.UNRESOLVED_TAG);
  }

  @Test
  void provisionalATagKnownToTheAccessIsAccepted() {
    // spec: RC-R13 (the tag exists once it is loaded)
    TestAccess access = TestAccess.standard().withTag("late_tag", "stone");
    Findings.value(Recipes.decode("r", shapeless("['#minecraft:late_tag']"), access));
    Findings.assertFailure(
        Recipes.decode("r", shapeless("['#minecraft:late_tag']"), TestAccess.standard()),
        FindingCode.UNRESOLVED_TAG);
  }

  @Test
  void provisionalAPatternThatIsEmptyTooTallTooWideOrRaggedIsRefused() {
    // spec: RC-R14 (1 to 3 rows, rows of equal length from 1 to 3)
    String key = "{'#':'minecraft:stone'}";
    Findings.assertFailure(shapedWith(key, "[]"), FindingCode.PATTERN_SHAPE);
    Findings.assertFailure(shapedWith(key, "['#','#','#','#']"), FindingCode.PATTERN_SHAPE);
    Findings.assertFailure(shapedWith(key, "['####']"), FindingCode.PATTERN_SHAPE);
    Findings.assertFailure(shapedWith(key, "['##','#']"), FindingCode.PATTERN_SHAPE);
  }

  @Test
  void provisionalAKeyNoPatternCharacterUsesIsRefused() {
    // spec: RC-R15
    Findings.assertFailure(
        shapedWith("{'#':'minecraft:stone','X':'minecraft:dirt'}", "['##']"),
        FindingCode.PATTERN_KEY_UNUSED);
  }

  @Test
  void provisionalAPatternCharacterWithoutKeyIsRefused() {
    // spec: RC-R15 (any character other than the space is a key)
    Findings.assertFailure(
        shapedWith("{'#':'minecraft:stone'}", "['#Y']"), FindingCode.PATTERN_KEY_MISSING);
  }

  @Test
  void provisionalAPatternOfSpacesOnlyIsRefused() {
    // spec: RC-R15 (a pattern whose cells are all empty is refused)
    Findings.assertAnyFailure(shapedWith("{}", "['  ','  ']"));
  }

  @Test
  void provisionalAShapedResultMustBeAFixedStack() {
    // spec: ShapedRecipe Javadoc (a fixed stack for this serializer), RC-R12 (only two recipes
    // have the empty result)
    Findings.assertAnyFailure(
        Recipes.decode("r", shaped("{'#':'minecraft:stone'}", "['#']", "{}")));
  }

  @Test
  void provisionalAResultAboveTheStackSizeIsRefused() {
    // spec: RC-R24 (stone stacks to 64, a pickaxe to 1)
    Findings.assertFailure(
        Recipes.decode(
            "r", shaped("{'#':'minecraft:stone'}", "['#']", "{'id':'minecraft:stone','count':65}")),
        FindingCode.RESULT_EXCEEDS_STACK);
    Findings.assertFailure(
        Recipes.decode(
            "r",
            shaped(
                "{'#':'minecraft:stone'}", "['#']", "{'id':'minecraft:stone_pickaxe','count':2}")),
        FindingCode.RESULT_EXCEEDS_STACK);
    Findings.assertFailure(
        Recipes.decode(
            "r",
            "{'type':'minecraft:stonecutting','ingredient':'minecraft:stone',"
                + "'result':{'id':'minecraft:ender_pearl','count':17}}"),
        FindingCode.RESULT_EXCEEDS_STACK);
  }

  @Test
  void provisionalAResultAtTheStackSizeIsAccepted() {
    // spec: RC-R24
    Findings.value(
        Recipes.decode(
            "r",
            shaped("{'#':'minecraft:stone'}", "['#']", "{'id':'minecraft:stone','count':64}")));
    Findings.value(
        Recipes.decode(
            "r",
            shaped(
                "{'#':'minecraft:stone'}", "['#']", "{'id':'minecraft:ender_pearl','count':16}")));
    Findings.value(
        Recipes.decode(
            "r",
            shaped(
                "{'#':'minecraft:stone'}", "['#']", "{'id':'minecraft:stone_pickaxe','count':1}")));
  }

  @Test
  void aRootThatIsNotAnObjectIsAFailureNotAnException() {
    // spec: RC-R1
    Findings.assertAnyFailure(Recipes.decode("r", "[1,2]"));
    Findings.assertAnyFailure(Recipes.decode("r", "'text'"));
    Findings.assertAnyFailure(Recipes.decode("r", "{}"));
  }
}
