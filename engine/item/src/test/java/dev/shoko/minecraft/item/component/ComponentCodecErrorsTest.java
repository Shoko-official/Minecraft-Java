package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R10, IC-R12 (provisional rejections, probe PI3): a missing
// required field, a field outside the shape, a value of the wrong kind, an unregistered type.
class ComponentCodecErrorsTest {

  private static void fails(String component, String json, FindingCode code) {
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertFailure(codec.valueFromJson(CodecFixture.id(component), Json.parse(json)), code);
  }

  @Test
  void provisionalAFoodWithoutSaturationIsMissingAField() {
    // spec: IC-R12 (a field present in every prototype is required)
    fails("food", "{'nutrition':5}", FindingCode.MISSING_FIELD);
  }

  @Test
  void provisionalAFoodWithoutNutritionIsMissingAField() {
    // spec: IC-R12
    fails("food", "{'saturation':6.0}", FindingCode.MISSING_FIELD);
  }

  @Test
  void provisionalOtherRequiredFieldsAreRequired() {
    // spec: IC-R12: villager_food.nutrition, enchantable.value, use_remainder.id, tool.rules
    fails("villager_food", "{}", FindingCode.MISSING_FIELD);
    fails("enchantable", "{}", FindingCode.MISSING_FIELD);
    fails("use_remainder", "{}", FindingCode.MISSING_FIELD);
    fails("tool", "{}", FindingCode.MISSING_FIELD);
  }

  @Test
  void provisionalAToolRuleNeedsItsBlocks() {
    // spec: IC-R12 (blocks 81 of 81 rules)
    fails("tool", "{'rules':[{'speed':4.0,'correct_for_drops':true}]}", FindingCode.MISSING_FIELD);
  }

  @Test
  void provisionalAnOptionalFieldMayBeAbsent() {
    // spec: IC-R12 (optional fields take the default of the codec)
    ComponentCodec codec = CodecFixture.codec();
    Findings.value(
        codec.valueFromJson(
            CodecFixture.id("food"), Json.parse("{'nutrition':5,'saturation':6.0}")));
    Findings.value(
        codec.valueFromJson(
            CodecFixture.id("tool"),
            Json.parse("{'rules':[{'blocks':'#minecraft:mineable/pickaxe'}]}")));
  }

  @Test
  void provisionalAFieldOutsideTheShapeIsRefused() {
    // spec: IC-R12, PI3: "un champ absent de cette union est une erreur de decodage"
    fails("food", "{'nutrition':5,'saturation':6.0,'invented':1}", FindingCode.UNKNOWN_FIELD);
    fails("villager_food", "{'nutrition':4,'invented':1}", FindingCode.UNKNOWN_FIELD);
    fails("enchantable", "{'value':5,'invented':true}", FindingCode.UNKNOWN_FIELD);
    fails("use_remainder", "{'id':'minecraft:bowl','invented':[]}", FindingCode.UNKNOWN_FIELD);
  }

  @Test
  void provisionalAFieldOutsideTheShapeOfAToolRuleIsRefused() {
    // spec: IC-R12 (union of the fields of the rules)
    fails(
        "tool",
        "{'rules':[{'blocks':'#minecraft:mineable/pickaxe','invented':1}]}",
        FindingCode.UNKNOWN_FIELD);
  }

  @Test
  void provisionalAnObjectWhereAScalarIsExpectedIsATypeMismatch() {
    // spec: IC-R12 (provisional)
    fails("rarity", "{}", FindingCode.TYPE_MISMATCH);
    fails("max_stack_size", "{}", FindingCode.TYPE_MISMATCH);
    fails("damage", "[]", FindingCode.TYPE_MISMATCH);
  }

  @Test
  void provisionalAScalarWhereAnObjectIsExpectedIsATypeMismatch() {
    // spec: IC-R12 (provisional)
    fails("food", "'bread'", FindingCode.TYPE_MISMATCH);
    fails("food", "5", FindingCode.TYPE_MISMATCH);
    fails("tool", "true", FindingCode.TYPE_MISMATCH);
    fails("use_remainder", "'minecraft:bowl'", FindingCode.TYPE_MISMATCH);
  }

  @Test
  void provisionalAWrongKindForAFieldIsATypeMismatch() {
    // spec: IC-R12 (provisional)
    fails("rarity", "5", FindingCode.TYPE_MISMATCH);
    fails("max_stack_size", "'many'", FindingCode.TYPE_MISMATCH);
    fails("food", "{'nutrition':'five','saturation':6.0}", FindingCode.TYPE_MISMATCH);
    fails("food", "{'nutrition':5,'saturation':'sweet'}", FindingCode.TYPE_MISMATCH);
    fails("enchantable", "{'value':[1]}", FindingCode.TYPE_MISMATCH);
  }

  @Test
  void provisionalAListWhereAnObjectIsExpectedIsATypeMismatch() {
    // spec: IC-R12 (provisional)
    fails("food", "[]", FindingCode.TYPE_MISMATCH);
    fails("lore", "{}", FindingCode.TYPE_MISMATCH);
    fails("attribute_modifiers", "{}", FindingCode.TYPE_MISMATCH);
  }

  @Test
  void provisionalAnUnregisteredComponentIsRefusedWhateverTheValue() {
    // spec: IC-R10 (provisional)
    ComponentCodec codec = CodecFixture.codec();
    Findings.assertFailure(
        codec.valueFromJson(Identifier.minecraft("not_a_component"), Json.parse("{}")),
        FindingCode.UNKNOWN_COMPONENT);
    Findings.assertFailure(
        codec.valueFromJson(Identifier.of("other", "rarity"), Json.parse("'common'")),
        FindingCode.UNKNOWN_COMPONENT);
  }

  @Test
  void provisionalAFailureNeverCarriesAValue() {
    // spec: Decoding (a failure keeps its findings and produces nothing)
    ComponentCodec codec = CodecFixture.codec();
    var decoding = codec.valueFromJson(CodecFixture.id("food"), Json.parse("{'nutrition':5}"));
    Findings.assertAnyFailure(decoding);
  }
}
