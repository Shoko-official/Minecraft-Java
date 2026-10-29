package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.component.ComponentCodec;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.recipe.codec.RecipeCodec;
import dev.shoko.minecraft.item.recipe.match.SmithingLookup;
import dev.shoko.minecraft.item.recipe.match.SmithingMatch;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// spec: item-recipes RC-R18, RC-R37 and item-components-inventory IC-R12, IC-R22: boundaries that
// the mutation review found unpinned (M56, M65, M69), and the levels of IC-R22 (defect B5).
class RecipeGapsHuntTest {

  private static final String TRIM =
      "{'type':'minecraft:smithing_trim','template':'minecraft:bolt_armor_trim_smithing_template',"
          + "'base':'#minecraft:trimmable_armor','addition':'#minecraft:trim_materials',"
          + "'pattern':'minecraft:bolt'}";

  private static Optional<SmithingMatch> trim(ItemStack addition) {
    RecipeSet set = HuntFixtures.recipes("bolt_trim", TRIM);
    return SmithingLookup.of(set, HuntFixtures.catalog())
        .find(
            HuntFixtures.stack("bolt_armor_trim_smithing_template"),
            HuntFixtures.stack("iron_helmet"),
            addition);
  }

  @Test
  void provisionalATrimMaterialIsTheAdditionThatProvidesOne() {
    // spec: RC-R37 (the emerald provides the material emerald)
    SmithingMatch match = trim(HuntFixtures.stack("emerald")).orElseThrow();
    assertThat(match.result().type().id().path()).isEqualTo("iron_helmet");
    assertThat(match.result().component(WellKnownComponents.TRIM)).isPresent();
  }

  @Test
  void provisionalAnAdditionThatProvidesNoTrimMaterialGivesNoTrim() {
    // spec: RC-R37 (M56: the iron ingot is in the tag of the fixtures but has no
    // provides_trim_material)
    assertThat(trim(HuntFixtures.stack("iron_ingot"))).isEmpty();
  }

  private static String shapeless(int ingredients) {
    String list = String.join(",", Collections.nCopies(ingredients, "'minecraft:stone'"));
    return "{'type':'minecraft:crafting_shapeless','ingredients':["
        + list
        + "],'result':{'id':'minecraft:chest'}}";
  }

  @Test
  void provisionalAShapelessRecipeTakesAtMostNineIngredients() {
    // spec: RC-R18 (M69: sizes 1 to 9, the grid)
    assertThat(HuntFixtures.decode("nine", shapeless(9))).isPresent();
  }

  @Test
  void provisionalAShapelessRecipeOfTenIngredientsIsRefused() {
    // spec: RC-R18 (M69)
    Decoding<?> ten =
        RecipeCodec.of(HuntFixtures.catalog())
            .decode(Identifier.minecraft("ten"), Json.parse(shapeless(10)), HuntFixtures.access());
    Findings.assertAnyFailure(ten);
  }

  private static Decoding<ComponentValue> component(String id, String json) {
    ComponentCodec codec = TestItems.componentCodec();
    return codec.valueFromJson(TestItems.id(id), Json.parse(json));
  }

  @Test
  void provisionalAnEquippableWithoutItsSlotIsRefused() {
    // spec: IC-R12 (M65: slot is in all 84 prototypes that carry equippable, so it is required)
    Findings.assertFailure(
        component("equippable", "{'asset_id':'minecraft:iron'}"), FindingCode.MISSING_FIELD);
    Findings.value(component("equippable", "{'slot':'head'}"));
  }

  @Test
  void provisionalEnchantmentLevelsGoFromOneTo255InBothComponents() {
    // spec: IC-R22 (defect B5: the codec accepted 256 while the typed view refuses it)
    for (String id : new String[] {"enchantments", "stored_enchantments"}) {
      Findings.value(component(id, "{'minecraft:sharpness':255}"));
      Findings.value(component(id, "{'minecraft:sharpness':1}"));
      Findings.assertFailure(component(id, "{'minecraft:sharpness':256}"), FindingCode.VALUE_RANGE);
      Findings.assertFailure(
          component(id, "{'minecraft:sharpness':2147483647}"), FindingCode.VALUE_RANGE);
    }
  }

  @Test
  void provisionalTwoSpellingsOfOneEnchantmentAreNotMergedInSilence() {
    // spec: IC-R22 (defect D4, stored_enchantments as well)
    ComponentValue value = null;
    Decoding<ComponentValue> decoded =
        component("stored_enchantments", "{'sharpness':1,'minecraft:sharpness':5}");
    if (decoded instanceof Decoding.Success<ComponentValue> ok) {
      value = ok.value();
    }
    assertThat(value).isNull();
  }
}
