package dev.shoko.minecraft.item.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R28 (JSON form of a stack in data files), V-I5. The observed
// shapes are level D; the default count, the `!` key and the refusals are level S.
class StackCodecJsonTest {

  private static StackCodec codec() {
    return TestItems.stackCodec();
  }

  private static Decoding<ItemStack> read(String relaxedJson) {
    return codec().fromJson(Json.parse(relaxedJson));
  }

  @Test
  void aStackWithItemAndCountIsRead() {
    // spec: IC-R28 (acacia_door result: {count: 3, id}, D)
    assertThat(Findings.value(read("{'count':3,'id':'minecraft:stone'}")))
        .isEqualTo(TestItems.stack("stone", 3));
  }

  @Test
  void provisionalAMissingCountIsOne() {
    // spec: IC-R28 (1294 results without count mean 1; the default is S)
    assertThat(Findings.value(read("{'id':'minecraft:stone'}")))
        .isEqualTo(TestItems.stack("stone", 1));
  }

  @Test
  void aBareItemObjectIsAOneUnitStackWithThePrototype() {
    // spec: IC-R28, V-I5 (decorated pot sherds: {id})
    ItemStack stack = Findings.value(read("{'id':'minecraft:cobblestone'}"));
    assertThat(stack.type()).isEqualTo(TestItems.type("cobblestone"));
    assertThat(stack.patch().isEmpty()).isTrue();
  }

  @Test
  void provisionalComponentsOfTheFileCoverThePrototype() {
    // spec: IC-R28, IC-R11b
    ItemStack stack =
        Findings.value(
            read("{'id':'minecraft:stone','count':2,'components':{'minecraft:rarity':'epic'}}"));
    assertThat(stack).isEqualTo(StackSupport.withRarity("stone", 2, "epic"));
  }

  @Test
  void provisionalThePotionOfABrewingOutputIsRead() {
    // spec: IC-R28, RC-R10 (output of the brewing recipes, D shape; count absent is S)
    ItemStack stack =
        Findings.value(
            read(
                "{'components':{'minecraft:potion_contents':{'potion':'minecraft:strength'}},"
                    + "'id':'minecraft:potion'}"));
    assertThat(stack.type()).isEqualTo(TestItems.type("potion"));
    assertThat(stack.count()).isEqualTo(1);
    assertThat(stack.component(TestItems.id("potion_contents"))).isPresent();
  }

  @Test
  void provisionalAKeyPrefixedByAnExclamationMarkRemovesAComponent() {
    // spec: IC-R28 (the components field accepts the `!` key; the value `{}` is an assumption)
    ItemStack stack =
        Findings.value(read("{'id':'minecraft:stone','components':{'!minecraft:rarity':{}}}"));
    assertThat(stack.component(StackSupport.rarityId())).isEmpty();
    assertThat(stack)
        .isEqualTo(TestItems.stack("stone", 1).withoutComponent(StackSupport.rarityId()));
  }

  @Test
  void provisionalAnUnknownComponentIsAFailure() {
    // spec: IC-R10
    Findings.assertFailure(
        read("{'id':'minecraft:stone','components':{'minecraft:inconnu':{}}}"),
        FindingCode.UNKNOWN_COMPONENT);
  }

  @Test
  void provisionalAnUnknownFieldOfAComponentIsAFailure() {
    // spec: IC-R12, probe PI3 (a field outside the known shape is UNKNOWN_FIELD)
    Findings.assertFailure(
        read(
            "{'id':'minecraft:bread','components':"
                + "{'minecraft:food':{'nutrition':1,'saturation':1.0,'bogus':true}}}"),
        FindingCode.UNKNOWN_FIELD);
  }

  @Test
  void provisionalAMissingRequiredFieldOfAComponentIsAFailure() {
    // spec: IC-R12, probe PI3 (nutrition and saturation are in every food prototype)
    Findings.assertFailure(
        read("{'id':'minecraft:bread','components':{'minecraft:food':{'saturation':1.0}}}"),
        FindingCode.MISSING_FIELD);
  }

  @Test
  void provisionalTheEmptyObjectIsNotAStack() {
    // spec: IC-R28 (Javadoc of fromJson: `{}` is handled by the recipes, not here)
    Findings.assertAnyFailure(read("{}"));
  }

  @Test
  void provisionalAMissingIdIsAFailure() {
    // spec: IC-R28
    Findings.assertFailure(read("{'count':2}"), FindingCode.MISSING_FIELD);
  }

  @Test
  void provisionalAMalformedIdentifierIsAFailure() {
    // spec: IC-R28 (FindingCode#INVALID_IDENTIFIER: a text that breaks the identifier syntax)
    Findings.assertFailure(read("{'id':'Minecraft:STONE'}"), FindingCode.INVALID_IDENTIFIER);
  }

  @Test
  void provisionalAnIdentifierOutsideTheRegistryIsAFailure() {
    // spec: IC-R28 (unknown item in data; the exact code is not specified for stacks)
    Decoding<ItemStack> decoding = read("{'id':'minecraft:inconnu'}");
    Findings.assertAnyFailure(decoding);
    assertThat(Findings.codesOf(decoding))
        .containsAnyOf(FindingCode.UNRESOLVED_ITEM, FindingCode.UNKNOWN_ITEM);
  }

  @Test
  void provisionalValuesOfTheWrongKindAreAFailure() {
    // spec: IC-R28 (FindingCode#TYPE_MISMATCH)
    Findings.assertFailure(read("{'id':5}"), FindingCode.TYPE_MISMATCH);
    Findings.assertFailure(read("{'id':'minecraft:stone','count':'x'}"), FindingCode.TYPE_MISMATCH);
    Findings.assertFailure(read("['minecraft:stone']"), FindingCode.TYPE_MISMATCH);
    Findings.assertFailure(read("'minecraft:stone'"), FindingCode.TYPE_MISMATCH);
  }

  @Test
  void provisionalTheWrittenFormAlwaysHasIdAndCount() {
    // spec: IC-R28 (Javadoc of toJson: count always written)
    for (int count : List.of(1, 2, 64)) {
      JsonNode node = Json.parseStrict(codec().toJson(TestItems.stack("stone", count)));
      assertThat(node).isInstanceOf(JsonObject.class);
      JsonObject object = (JsonObject) node;
      assertThat(object.get("id")).isPresent();
      JsonNode written = object.get("count").orElseThrow();
      assertThat(written).isInstanceOf(JsonNumber.class);
      assertThat(((JsonNumber) written).longValue()).isEqualTo(count);
    }
  }

  @Test
  void provisionalStacksSurviveAWriteAndARead() {
    // spec: IC-R28 (Javadoc of toJson: reads back to an equal stack)
    List<ItemStack> stacks =
        List.of(
            TestItems.stack("stone", 3),
            StackSupport.withRarity("stone", 64, "epic"),
            TestItems.stack("stone", 1).withoutComponent(StackSupport.rarityId()),
            TestItems.stack("stone_pickaxe", 1, "damage", "17"),
            TestItems.stack(
                "potion", 1, "potion_contents", "{'potion':'minecraft:strong_healing'}"),
            TestItems.stack("bread", 2, "food", "{'nutrition':1,'saturation':-0.0}"),
            TestItems.stack("stone", 5, "enchantments", "{'minecraft:sharpness':3}"));
    for (ItemStack stack : stacks) {
      String text = codec().toJson(stack);
      assertThat(Findings.value(codec().fromJson(Json.parseStrict(text))))
          .as(text)
          .isEqualTo(stack);
    }
  }

  @Test
  void writingTheEmptyStackIsRefused() {
    // spec: IC-R15 (Javadoc of toJson)
    assertThatIllegalArgumentException().isThrownBy(() -> codec().toJson(ItemStack.empty()));
  }
}
