package dev.shoko.minecraft.item.component;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.component.CodecFixture.Sample;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R9, IC-R12, IC-R13, IC-R18 (D forms accepted, float handling,
// value equality). The rejections are in ComponentCodecErrorsTest.
class ComponentCodecJsonTest {

  private static JsonNode reparsed(ComponentCodec codec, String component, ComponentValue value) {
    return Json.parseStrict(codec.valueToJson(CodecFixture.id(component), value));
  }

  private static JsonNumber number(JsonNode node, String key) {
    return (JsonNumber) ((JsonObject) node).get(key).orElseThrow();
  }

  private static int floatBits(JsonNumber number) {
    return Float.floatToRawIntBits((float) number.doubleValue());
  }

  @Test
  void everyDocumentedShapeIsAccepted() {
    // spec: IC-R12 (shapes observed in the prototypes), IC-R9
    ComponentCodec codec = CodecFixture.codec();
    for (Sample sample : CodecFixture.samples()) {
      var decoding =
          codec.valueFromJson(CodecFixture.id(sample.component()), Json.parse(sample.json()));
      assertThat(Findings.value(decoding)).as("%s", sample).isNotNull();
    }
  }

  @Test
  void decodingTheSameJsonTwiceGivesEqualValues() {
    // spec: IC-R18
    ComponentCodec codec = CodecFixture.codec();
    for (Sample sample : CodecFixture.samples()) {
      ComponentValue first = CodecFixture.decode(codec, sample.component(), sample.json());
      ComponentValue second = CodecFixture.decode(codec, sample.component(), sample.json());
      assertThat(second).as("%s", sample).isEqualTo(first);
      assertThat(second.hashCode()).isEqualTo(first.hashCode());
    }
  }

  @Test
  void aValueWrittenAsJsonReadsBackToTheSameValue() {
    // spec: ComponentCodec.valueToJson: "reads back to the same value"
    ComponentCodec codec = CodecFixture.codec();
    for (Sample sample : CodecFixture.samples()) {
      ComponentValue value = CodecFixture.decode(codec, sample.component(), sample.json());
      JsonNode text = reparsed(codec, sample.component(), value);
      var again = codec.valueFromJson(CodecFixture.id(sample.component()), text);
      assertThat(Findings.value(again)).as("%s", sample).isEqualTo(value);
    }
  }

  @Test
  void differentJsonValuesGiveDifferentComponentValues() {
    // spec: IC-R18
    ComponentCodec codec = CodecFixture.codec();
    assertThat(CodecFixture.decode(codec, "food", "{'nutrition':5,'saturation':6.0}"))
        .isNotEqualTo(CodecFixture.decode(codec, "food", "{'nutrition':6,'saturation':6.0}"))
        .isNotEqualTo(CodecFixture.decode(codec, "food", "{'nutrition':5,'saturation':6.5}"))
        .isNotEqualTo(
            CodecFixture.decode(
                codec, "food", "{'nutrition':5,'saturation':6.0,'can_always_eat':true}"));
    assertThat(CodecFixture.decode(codec, "rarity", "'common'"))
        .isNotEqualTo(CodecFixture.decode(codec, "rarity", "'epic'"));
  }

  @Test
  void aDecodedValueDoesNotDependOnTheOrderOfTheFieldsOfAnObject() {
    // spec: IC-R18 (compounds compare keys and values regardless of key order, core-nbt R28)
    ComponentCodec codec = CodecFixture.codec();
    assertThat(CodecFixture.decode(codec, "food", "{'saturation':6.0,'nutrition':5}"))
        .isEqualTo(CodecFixture.decode(codec, "food", "{'nutrition':5,'saturation':6.0}"));
  }

  @Test
  void aStringComponentKeepsItsTextAsAStringTag() {
    // spec: IC-R4 (rarity, item_model, break_sound are strings)
    ComponentCodec codec = CodecFixture.codec();
    for (Sample sample :
        new Sample[] {
          new Sample("rarity", "'epic'"),
          new Sample("item_model", "'minecraft:stone'"),
          new Sample("break_sound", "'minecraft:entity.item.break'")
        }) {
      ComponentValue value = CodecFixture.decode(codec, sample.component(), sample.json());
      assertThat(value.tag()).isEqualTo(new StringTag(sample.json().replace("'", "")));
    }
  }

  @Test
  void anEmptyObjectComponentIsTheEmptyCompound() {
    // spec: IC-R4 (enchantments {})
    ComponentCodec codec = CodecFixture.codec();
    for (String component : new String[] {"enchantments", "stored_enchantments"}) {
      ComponentValue value = CodecFixture.decode(codec, component, "{}");
      assertThat(value.tag()).as(component).isEqualTo(CompoundTag.empty());
    }
  }

  @Test
  void anAttributeAmountKeepsEveryBitOfItsFloat() {
    // spec: IC-R12, IC-R13: -2.799999952316284 is the double form of a float
    ComponentCodec codec = CodecFixture.codec();
    String modifiers =
        "[{'type':'minecraft:attack_speed','amount':-2.799999952316284,"
            + "'id':'minecraft:base_attack_speed','operation':'add_value','slot':'mainhand'}]";
    ComponentValue value = CodecFixture.decode(codec, "attribute_modifiers", modifiers);
    JsonArray written = (JsonArray) reparsed(codec, "attribute_modifiers", value);
    assertThat(written.elements()).hasSize(1);
    JsonNode modifier = written.elements().get(0);
    assertThat(((JsonObject) modifier).members()).containsKeys("type", "amount", "operation");
    assertThat(floatBits(number(modifier, "amount")))
        .isEqualTo(Float.floatToRawIntBits((float) -2.799999952316284));
  }

  @Test
  void aFloatFieldIsReadAsADoubleThenConvertedToFloat() {
    // spec: IC-R13: 0.1f and its exact double give the same value
    ComponentCodec codec = CodecFixture.codec();
    assertThat(
            CodecFixture.decode(codec, "food", "{'nutrition':1,'saturation':0.10000000149011612}"))
        .isEqualTo(CodecFixture.decode(codec, "food", "{'nutrition':1,'saturation':0.1}"));
  }

  @Test
  void theMaximumFloatSentinelOfAToolSpeedSurvives() {
    // spec: IC-R13: speed 3.4028235e+38 ("instant mining")
    ComponentCodec codec = CodecFixture.codec();
    String tool =
        "{'rules':[{'blocks':'#minecraft:mineable/pickaxe','correct_for_drops':true,"
            + "'speed':3.4028235e+38}]}";
    ComponentValue value = CodecFixture.decode(codec, "tool", tool);
    JsonObject written = (JsonObject) reparsed(codec, "tool", value);
    JsonObject rule =
        (JsonObject) ((JsonArray) written.get("rules").orElseThrow()).elements().get(0);
    assertThat(floatBits(number(rule, "speed")))
        .isEqualTo(Float.floatToRawIntBits(Float.MAX_VALUE));
    assertThat(
            CodecFixture.decode(
                codec, "tool", tool.replace("3.4028235e+38", "3.4028234663852886E38")))
        .isEqualTo(value);
  }

  @Test
  void provisionalNegativeZeroDiffersFromZero() {
    // spec: IC-R18, core-nbt R37 (provisional for components): floats compare by bits
    ComponentCodec codec = CodecFixture.codec();
    String positive = modifierWithAmount("0.0");
    String negative = modifierWithAmount("-0.0");
    assertThat(CodecFixture.decode(codec, "attribute_modifiers", negative))
        .isNotEqualTo(CodecFixture.decode(codec, "attribute_modifiers", positive));
  }

  @Test
  void provisionalNegativeZeroSurvivesAJsonRoundTrip() {
    // spec: IC-R18, IC-R12 (provisional): "conserves au bit pres"
    ComponentCodec codec = CodecFixture.codec();
    ComponentValue value =
        CodecFixture.decode(codec, "attribute_modifiers", modifierWithAmount("-0.0"));
    var again =
        codec.valueFromJson(
            CodecFixture.id("attribute_modifiers"), reparsed(codec, "attribute_modifiers", value));
    assertThat(Findings.value(again)).isEqualTo(value);
  }

  private static String modifierWithAmount(String amount) {
    return "[{'type':'minecraft:attack_damage','amount':"
        + amount
        + ",'id':'minecraft:base_attack_damage','operation':'add_value','slot':'mainhand'}]";
  }
}
