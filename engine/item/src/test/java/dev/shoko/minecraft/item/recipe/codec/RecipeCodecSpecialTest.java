package dev.shoko.minecraft.item.recipe.codec;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.recipe.RecipeType;
import dev.shoko.minecraft.item.recipe.SpecialKind;
import dev.shoko.minecraft.item.recipe.SpecialRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.testing.Recipes;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

// spec: RC-R3, RC-R12, RC-R43 to RC-R49e: the eleven serializers whose result depends on content.
// The ingredients are made-up items of the fake report: only the shape of each file matters here.
class RecipeCodecSpecialTest {

  /** One serializer: its name, the body of a file, the kind, the part keys and the result form. */
  private record Case(
      String serializer, String body, SpecialKind kind, List<String> parts, boolean derived) {}

  private static List<Case> cases() {
    return Stream.concat(firstCases().stream(), lastCases().stream()).toList();
  }

  private static List<Case> firstCases() {
    return List.of(
        new Case(
            "crafting_special_bannerduplicate",
            "'banner':'minecraft:white_banner','result':{'id':'minecraft:white_banner'}",
            SpecialKind.BANNER_DUPLICATE,
            List.of("banner"),
            false),
        new Case(
            "crafting_special_bookcloning",
            "'material':'#minecraft:logs','result':{'id':'minecraft:paper'},"
                + "'source':'minecraft:paper'",
            SpecialKind.BOOK_CLONING,
            List.of("source", "material"),
            false),
        new Case(
            "crafting_special_firework_rocket",
            "'fuel':'minecraft:gunpowder','result':{'count':3,'id':'minecraft:stick'},"
                + "'shell':'minecraft:paper','star':'minecraft:diamond'",
            SpecialKind.FIREWORK_ROCKET,
            List.of("shell", "fuel", "star"),
            false),
        new Case(
            "crafting_special_firework_star",
            "'dye':'#minecraft:planks','fuel':'minecraft:gunpowder',"
                + "'result':{'id':'minecraft:diamond'},"
                + "'shapes':{'burst':'minecraft:stick','creeper':'#minecraft:logs',"
                + "'large_ball':'minecraft:coal','star':'minecraft:gold_ingot'},"
                + "'trail':'minecraft:diamond','twinkle':'minecraft:redstone'",
            SpecialKind.FIREWORK_STAR,
            List.of(
                "fuel",
                "dye",
                "trail",
                "twinkle",
                "shapes.burst",
                "shapes.creeper",
                "shapes.large_ball",
                "shapes.star"),
            false),
        new Case(
            "crafting_special_firework_star_fade",
            "'dye':'#minecraft:planks','result':{'id':'minecraft:diamond'},"
                + "'target':'minecraft:diamond'",
            SpecialKind.FIREWORK_STAR_FADE,
            List.of("target", "dye"),
            false),
        new Case(
            "crafting_special_mapextending",
            "'map':'minecraft:map','material':'minecraft:paper','result':{}",
            SpecialKind.MAP_EXTENDING,
            List.of("map", "material"),
            true));
  }

  private static List<Case> lastCases() {
    return List.of(
        new Case("crafting_special_repairitem", "", SpecialKind.REPAIR_ITEM, List.of(), true),
        new Case(
            "crafting_special_shielddecoration",
            "'banner':'minecraft:white_banner','result':{'id':'minecraft:stone'},"
                + "'target':'minecraft:stone'",
            SpecialKind.SHIELD_DECORATION,
            List.of("target", "banner"),
            false),
        new Case(
            "crafting_decorated_pot",
            "'back':'minecraft:stone','front':'minecraft:stone','left':'minecraft:stone',"
                + "'result':{'id':'minecraft:chest'},'right':'minecraft:stone'",
            SpecialKind.DECORATED_POT,
            List.of("back", "front", "left", "right"),
            false),
        new Case(
            "crafting_dye",
            "'dye':'#minecraft:planks','group':'dyed_armor',"
                + "'result':{'id':'minecraft:iron_helmet'},'target':'minecraft:iron_helmet'",
            SpecialKind.DYE,
            List.of("target", "dye"),
            false),
        new Case(
            "crafting_imbue",
            "'material':'minecraft:stone','result':{'count':8,'id':'minecraft:stone'},"
                + "'source':'minecraft:lingering_potion'",
            SpecialKind.IMBUE,
            List.of("source", "material"),
            false));
  }

  private static String json(Case c) {
    String body = c.body().isEmpty() ? "" : "," + c.body();
    return "{'type':'minecraft:" + c.serializer() + "'" + body + "}";
  }

  private static SpecialRecipe decode(Case c) {
    return Recipes.recipe(SpecialRecipe.class, "special", json(c));
  }

  @Test
  void eachOfTheElevenSerializersGivesItsKind() {
    // spec: RC-R3 (22 serializers, 11 of them special), RC-R43 to RC-R49e
    assertThat(cases()).hasSize(11);
    for (Case c : cases()) {
      assertThat(decode(c).kind()).as(c.serializer()).isEqualTo(c.kind());
    }
  }

  @Test
  void aSpecialRecipeWithoutResultOrWithAnEmptyResultIsDerived() {
    // spec: RC-R12 (map_extending has {} and repairitem has no result: derived from the grid)
    for (Case c : cases()) {
      RecipeResult result = decode(c).result();
      assertThat(result instanceof RecipeResult.Derived).as(c.serializer()).isEqualTo(c.derived());
    }
  }

  @Test
  void aFixedResultOfASpecialFileKeepsItsCount() {
    // spec: RC-R11a, RC-R45 (firework rocket result x3), RC-R49e (arrow x8)
    SpecialRecipe rocket = decode(cases().get(2));
    SpecialRecipe imbue = decode(cases().get(10));
    assertThat(((RecipeResult.Fixed) rocket.result()).stack().count()).isEqualTo(3);
    assertThat(((RecipeResult.Fixed) imbue.result()).stack().count()).isEqualTo(8);
  }

  @Test
  void provisionalASpecialRecipeRunsOnTheCraftingMachine() {
    // spec: RC-R4
    for (Case c : cases()) {
      assertThat(decode(c).type()).as(c.serializer()).isEqualTo(RecipeType.CRAFTING);
    }
  }

  @Test
  void provisionalThePartsOfEachKindFollowTheKindJavadocOfTheSkeleton() {
    // spec: SpecialKind Javadoc (a design contract of the skeleton, not a rule of the spec: the
    // keys are the field names of the file, nested objects flattened with a dot)
    for (Case c : cases()) {
      assertThat(decode(c).parts().keySet())
          .as(c.serializer())
          .containsExactlyInAnyOrderElementsOf(c.parts());
    }
  }

  @Test
  void provisionalTheGroupOfADyeFileIsKept() {
    // spec: RC-R3 (crafting_dye has a group), RC-R6
    SpecialRecipe dye = decode(cases().get(9));
    assertThat(dye.group()).isEqualTo("dyed_armor");
    assertThat(decode(cases().get(0)).group()).isEmpty();
  }
}
