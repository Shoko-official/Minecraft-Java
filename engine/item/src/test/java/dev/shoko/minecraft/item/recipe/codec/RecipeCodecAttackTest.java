package dev.shoko.minecraft.item.recipe.codec;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonReader;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.Finding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.recipe.BrewingRecipe;
import dev.shoko.minecraft.item.recipe.CookingRecipe;
import dev.shoko.minecraft.item.recipe.Recipe;
import dev.shoko.minecraft.item.recipe.ShapedRecipe;
import dev.shoko.minecraft.item.recipe.ShapelessRecipe;
import dev.shoko.minecraft.item.recipe.SmithingTransformRecipe;
import dev.shoko.minecraft.item.recipe.StonecuttingRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.recipe.ingredient.ShapedPattern;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.Recipes;
import dev.shoko.minecraft.item.testing.TestAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: RC-R1, RC-R9, RC-R13 to RC-R15, RC-R24 (adversarial): seeded mutation of valid recipe files
// of every serializer. A decoding is a typed result or a failure with a code, never an exception.
class RecipeCodecAttackTest {

  private static final Identifier ID = Identifier.minecraft("fuzz");
  private static final int MUTANTS_PER_FILE = 120;

  private static List<String> corpus() {
    List<String> files = new ArrayList<>(ordinary());
    files.addAll(specials());
    return files;
  }

  private static List<String> ordinary() {
    return List.of(
        "{'type':'minecraft:crafting_shaped','category':'building','group':'g',"
            + "'key':{'#':'#minecraft:planks','X':['minecraft:stone','minecraft:dirt']},"
            + "'pattern':['#X ','X# '],'result':{'count':3,'id':'minecraft:stone_slab'},"
            + "'show_notification':false}",
        "{'type':'minecraft:crafting_shapeless','group':'g','ingredients':['#minecraft:logs',"
            + "['minecraft:stone','minecraft:dirt'],'minecraft:stick'],"
            + "'result':{'count':4,'id':'minecraft:oak_planks'}}",
        "{'type':'minecraft:smelting','category':'food','cookingtime':200,'experience':0.35,"
            + "'ingredient':'minecraft:potato','result':{'id':'minecraft:baked_potato'}}",
        "{'type':'minecraft:blasting','category':'blocks','cookingtime':200,'experience':0.7,"
            + "'ingredient':['minecraft:stone','minecraft:dirt'],"
            + "'result':{'id':'minecraft:iron_ingot'}}",
        "{'type':'minecraft:smoking','cookingtime':200,'experience':0.35,"
            + "'ingredient':'minecraft:potato','result':{'id':'minecraft:baked_potato'}}",
        "{'type':'minecraft:campfire_cooking','cookingtime':600,'experience':0.35,"
            + "'ingredient':'minecraft:potato','result':{'id':'minecraft:baked_potato'}}",
        "{'type':'minecraft:stonecutting','ingredient':'minecraft:stone',"
            + "'result':{'count':2,'id':'minecraft:stone_slab'}}",
        "{'type':'minecraft:smithing_transform','addition':'#minecraft:netherite_tool_materials',"
            + "'base':'minecraft:diamond_axe','result':{'id':'minecraft:netherite_axe'},"
            + "'template':'minecraft:netherite_upgrade_smithing_template'}",
        "{'type':'minecraft:smithing_trim','addition':'#minecraft:trim_materials',"
            + "'base':'#minecraft:trimmable_armor','pattern':'minecraft:bolt',"
            + "'template':'minecraft:bolt_armor_trim_smithing_template'}",
        "{'type':'minecraft:brewing','input':{'item':'minecraft:potion',"
            + "'potion_contents':{'potions':'minecraft:awkward'}},"
            + "'output':{'components':{'minecraft:potion_contents':{'potion':'minecraft:strength'}},"
            + "'id':'minecraft:potion'},'reagent':{'item':'minecraft:blaze_powder'}}",
        "{'type':'minecraft:crafting_transmute','add_material_count_to_result':true,"
            + "'group':'t','input':'#minecraft:logs','material':'minecraft:map',"
            + "'material_count':{'max':8,'min':1},'result':{}}");
  }

  private static List<String> specials() {
    return List.of(
        "{'type':'minecraft:crafting_special_bannerduplicate','banner':'minecraft:white_banner',"
            + "'result':{'id':'minecraft:white_banner'}}",
        "{'type':'minecraft:crafting_special_bookcloning','material':'#minecraft:logs',"
            + "'result':{'id':'minecraft:paper'},'source':'minecraft:paper'}",
        "{'type':'minecraft:crafting_special_firework_rocket','fuel':'minecraft:gunpowder',"
            + "'result':{'count':3,'id':'minecraft:stick'},'shell':'minecraft:paper',"
            + "'star':'minecraft:diamond'}",
        "{'type':'minecraft:crafting_special_firework_star','dye':'#minecraft:planks',"
            + "'fuel':'minecraft:gunpowder','result':{'id':'minecraft:diamond'},"
            + "'shapes':{'burst':'minecraft:stick','creeper':'#minecraft:logs',"
            + "'large_ball':'minecraft:coal','star':'minecraft:gold_ingot'},"
            + "'trail':'minecraft:diamond','twinkle':'minecraft:redstone'}",
        "{'type':'minecraft:crafting_special_firework_star_fade','dye':'#minecraft:planks',"
            + "'result':{'id':'minecraft:diamond'},'target':'minecraft:diamond'}",
        "{'type':'minecraft:crafting_special_mapextending','map':'minecraft:map',"
            + "'material':'minecraft:paper','result':{}}",
        "{'type':'minecraft:crafting_special_repairitem'}",
        "{'type':'minecraft:crafting_special_shielddecoration','banner':'minecraft:white_banner',"
            + "'result':{'id':'minecraft:stone'},'target':'minecraft:stone'}",
        "{'type':'minecraft:crafting_decorated_pot','back':'minecraft:stone',"
            + "'front':'minecraft:stone','left':'minecraft:stone','result':{'id':'minecraft:chest'},"
            + "'right':'minecraft:stone'}",
        "{'type':'minecraft:crafting_dye','dye':'#minecraft:planks','group':'d',"
            + "'result':{'id':'minecraft:iron_helmet'},'target':'minecraft:iron_helmet'}",
        "{'type':'minecraft:crafting_imbue','material':'minecraft:stone',"
            + "'result':{'count':8,'id':'minecraft:stone'},'source':'minecraft:lingering_potion'}");
  }

  /** Mutants of the whole corpus that the JSON reader accepts, from a fixed seed. */
  private static List<JsonNode> mutants(long seed) {
    Random random = new Random(seed);
    List<JsonNode> accepted = new ArrayList<>();
    for (String file : corpus()) {
      JsonNode original = Json.parse(file);
      for (int i = 0; i < MUTANTS_PER_FILE; i++) {
        String text = JsonMutator.mutate(original, random);
        JsonReader.read(text, Json.ORIGIN).value().ifPresent(accepted::add);
      }
    }
    return accepted;
  }

  private static Decoding<Recipe> decode(JsonNode node, TestAccess access) {
    try {
      return Recipes.codec().decode(ID, node, access);
    } catch (UnsupportedOperationException notYet) {
      throw notYet;
    } catch (RuntimeException e) {
      throw new AssertionError("untyped exception on " + describe(node), e);
    }
  }

  private static String describe(JsonNode node) {
    return node.path() + " of a mutant";
  }

  private static boolean hasError(List<Finding> findings) {
    return findings.stream().anyMatch(f -> f.code().severity() == Severity.ERROR);
  }

  @Test
  @Timeout(60)
  void everyMutantGivesATypedResultWhoseSeverityMatchesItsKind() {
    // spec: Decoding Javadoc (a failure has an ERROR finding, a success has none)
    TestAccess access = TestAccess.standard();
    int successes = 0;
    int failures = 0;
    for (JsonNode mutant : mutants(0xA77AC4L)) {
      Decoding<Recipe> decoding = decode(mutant, access);
      switch (decoding) {
        case Decoding.Success<Recipe> success -> {
          successes++;
          assertThat(hasError(success.findings())).isFalse();
        }
        case Decoding.Failure<Recipe> failure -> {
          failures++;
          assertThat(hasError(failure.findings())).isTrue();
        }
      }
    }
    assertThat(successes).as("mutants that still decode").isPositive();
    assertThat(failures).as("mutants that are refused").isPositive();
  }

  @Test
  @Timeout(60)
  void decodingTheSameMutantTwiceGivesTheSameOutcome() {
    // spec: RC-R1 (a recipe is a function of its file)
    TestAccess access = TestAccess.standard();
    for (JsonNode mutant : mutants(0xDE7E12L)) {
      Decoding<Recipe> first = decode(mutant, access);
      Decoding<Recipe> second = decode(mutant, access);
      assertThat(second.getClass()).isEqualTo(first.getClass());
      assertThat(second.findings()).isEqualTo(first.findings());
      if (first instanceof Decoding.Success<Recipe> a
          && second instanceof Decoding.Success<Recipe> b) {
        assertThat(RecipeDescriber.describe(b.value()))
            .isEqualTo(RecipeDescriber.describe(a.value()));
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalEveryAcceptedRecipeRespectsTheShapeAndStackLimits() {
    // spec: RC-R14 (pattern 1 to 3 by 1 to 3, a cell filled), RC-R24 (count within the stack size)
    TestAccess access = TestAccess.standard();
    for (JsonNode mutant : mutants(0x5AFE17L)) {
      if (decode(mutant, access) instanceof Decoding.Success<Recipe> success) {
        assertSane(success.value());
      }
    }
  }

  private static void assertSane(Recipe recipe) {
    switch (recipe) {
      case ShapedRecipe shaped -> {
        assertPattern(shaped.pattern());
        assertResult(shaped.result());
      }
      case ShapelessRecipe shapeless -> {
        assertThat(shapeless.ingredients()).isNotEmpty().hasSizeLessThanOrEqualTo(9);
        assertResult(shapeless.result());
      }
      case CookingRecipe cooking -> assertStack(cooking.result());
      case StonecuttingRecipe cutting -> assertStack(cutting.result());
      case SmithingTransformRecipe transform -> assertStack(transform.result());
      case BrewingRecipe brewing -> assertStack(brewing.output());
      default -> assertThat(recipe.id()).isEqualTo(ID);
    }
  }

  private static void assertPattern(ShapedPattern pattern) {
    assertThat(pattern.width()).isBetween(1, 3);
    assertThat(pattern.height()).isBetween(1, 3);
    assertThat(pattern.cells()).hasSize(pattern.width() * pattern.height());
    assertThat(pattern.cells().stream().anyMatch(cell -> !cell.isNone())).isTrue();
  }

  private static void assertResult(RecipeResult result) {
    if (result instanceof RecipeResult.Fixed fixed) {
      assertStack(fixed.stack());
    }
  }

  private static void assertStack(ItemStack stack) {
    assertThat(stack.count()).isLessThanOrEqualTo(stack.maxStackSize());
  }

  @Test
  @Timeout(60)
  void provisionalRandomPatternsAreJudgedLikeAnIndependentModel() {
    // spec: RC-R14, RC-R15 (shape first, then keys; a pattern of spaces is refused)
    Random random = new Random(0x9A77E2L);
    String alphabet = "#XY  ";
    TestAccess access = TestAccess.standard();
    for (int i = 0; i < 600; i++) {
      List<String> rows = new ArrayList<>();
      int rowCount = random.nextInt(6);
      int width = random.nextInt(5);
      for (int r = 0; r < rowCount; r++) {
        int length = random.nextInt(8) == 0 ? random.nextInt(5) : width;
        StringBuilder row = new StringBuilder();
        for (int c = 0; c < length; c++) {
          row.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        rows.add(row.toString());
      }
      boolean hasHash = random.nextBoolean();
      boolean hasX = random.nextBoolean();
      String json = shaped(rows, hasHash, hasX);
      Decoding<Recipe> decoding = decode(Json.parseStrict(json), access);
      assertAgainstModel(rows, hasHash, hasX, decoding, json);
    }
  }

  private static String shaped(List<String> rows, boolean hasHash, boolean hasX) {
    List<String> quoted = rows.stream().map(row -> "\"" + row + "\"").toList();
    List<String> keys = new ArrayList<>();
    if (hasHash) {
      keys.add("\"#\":\"minecraft:stone\"");
    }
    if (hasX) {
      keys.add("\"X\":\"minecraft:dirt\"");
    }
    return "{\"type\":\"minecraft:crafting_shaped\",\"key\":{"
        + String.join(",", keys)
        + "},\"pattern\":["
        + String.join(",", quoted)
        + "],\"result\":{\"id\":\"minecraft:chest\"}}";
  }

  private static void assertAgainstModel(
      List<String> rows, boolean hasHash, boolean hasX, Decoding<Recipe> decoding, String json) {
    int width = rows.isEmpty() ? 0 : rows.getFirst().length();
    boolean ragged = rows.stream().anyMatch(row -> row.length() != width);
    boolean badShape = rows.isEmpty() || rows.size() > 3 || ragged || width < 1 || width > 3;
    String all = String.join("", rows);
    List<FindingCode> codes = decoding.findings().stream().map(Finding::code).toList();
    if (badShape) {
      assertThat(codes).as(json).contains(FindingCode.PATTERN_SHAPE);
      return;
    }
    boolean usesHash = all.indexOf('#') >= 0;
    boolean usesX = all.indexOf('X') >= 0;
    boolean usesY = all.indexOf('Y') >= 0;
    boolean unused = (hasHash && !usesHash) || (hasX && !usesX);
    boolean missing = (usesHash && !hasHash) || (usesX && !hasX) || usesY;
    if (unused) {
      assertThat(codes).as(json).contains(FindingCode.PATTERN_KEY_UNUSED);
    }
    if (missing) {
      assertThat(codes).as(json).contains(FindingCode.PATTERN_KEY_MISSING);
    }
    boolean allSpaces = all.isBlank();
    if (unused || missing || allSpaces) {
      assertThat(decoding).as(json).isInstanceOf(Decoding.Failure.class);
    } else {
      assertThat(decoding).as(json).isInstanceOf(Decoding.Success.class);
    }
  }
}
