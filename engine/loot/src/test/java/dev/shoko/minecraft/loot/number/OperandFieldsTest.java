package dev.shoko.minecraft.loot.number;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.table.LootTableCodec;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

// spec: LT-R79 (names of the operand fields of the number providers, provisional, probe PL27):
// binary left/right (sub, div, mod, floor_div, floor_mod), pow base/exponent, lists named inputs
// (add, mul, min, max, avg), unary input (abs, negate, ceil, floor, round, truncate, sqrt, sin,
// cos,
// from_float, from_int). The old name argument and the names left/right for pow are refused.
class OperandFieldsTest {

  private static final List<String> INT_UNARY = List.of("abs", "negate");
  private static final List<String> FLOAT_UNARY =
      List.of("abs", "negate", "ceil", "floor", "round", "truncate", "sqrt", "sin", "cos");
  private static final List<String> INT_LEFT_RIGHT =
      List.of("sub", "div", "mod", "floor_div", "floor_mod");
  private static final List<String> FLOAT_LEFT_RIGHT = List.of("sub", "div", "mod");
  private static final List<String> LISTS = List.of("add", "mul", "min", "max", "avg");

  private static LootDecoding<IntProvider> decodeInt(String json) {
    return NumberProviderCodec.decodeInt(Json.parse(json), EnchantFixtures.access());
  }

  private static LootDecoding<FloatProvider> decodeFloat(String json) {
    return NumberProviderCodec.decodeFloat(Json.parse(json), EnchantFixtures.access());
  }

  private static IntProvider.Operator intOperator(String name) {
    return IntProvider.Operator.valueOf(name.toUpperCase(Locale.ROOT));
  }

  private static FloatProvider.Operator floatOperator(String name) {
    return FloatProvider.Operator.valueOf(name.toUpperCase(Locale.ROOT));
  }

  private static IntProvider.Constant i(int value) {
    return new IntProvider.Constant(value);
  }

  private static FloatProvider.Constant f(float value) {
    return new FloatProvider.Constant(value);
  }

  private static void assertRefusedByField(LootDecoding<?> decoding) {
    Decoded.fails(decoding);
    assertThat(Decoded.codes(decoding))
        .containsAnyOf(LootFindingCode.MISSING_FIELD, LootFindingCode.UNKNOWN_FIELD);
    Decoded.assertWellFormed(decoding);
  }

  private static void assertRoundTrip(String table) {
    JsonNode original = Json.parse(table);
    LootDecoding<LootTable> decoding =
        LootTableCodec.decode(original, EnchantFixtures.access(), LootItems.catalog());
    LootTable decoded = Decoded.value(decoding);
    String encoded = LootTableCodec.encode(decoded);
    assertThat(encoded).doesNotContain("argument");
    assertThat(Json.parseStrict(encoded).sameValueAs(original)).as(encoded).isTrue();
  }

  private static String inRolls(String provider) {
    return "{'type':'minecraft:chest','pools':[{'rolls':" + provider + ",'entries':[]}]}";
  }

  private static String inBonus(String provider) {
    return "{'type':'minecraft:chest','pools':[{'rolls':1,'bonus_rolls':"
        + provider
        + ",'entries':[]}]}";
  }

  @Test
  void provisionalIntegerUnaryOperatorsReadInput() {
    // spec: LT-R79 (input for the unary operators)
    for (String name : INT_UNARY) {
      IntProvider provider =
          Decoded.value(decodeInt("{'type':'minecraft:" + name + "','input':7}"));
      assertThat(provider)
          .as(name)
          .isEqualTo(new IntProvider.Arithmetic(intOperator(name), List.of(i(7))));
    }
  }

  @Test
  void provisionalFloatUnaryOperatorsReadInput() {
    // spec: LT-R79
    for (String name : FLOAT_UNARY) {
      FloatProvider provider =
          Decoded.value(decodeFloat("{'type':'minecraft:" + name + "','input':2.5}"));
      assertThat(provider)
          .as(name)
          .isEqualTo(new FloatProvider.Arithmetic(floatOperator(name), List.of(f(2.5f))));
    }
  }

  @Test
  void provisionalFromFloatAndFromIntReadInput() {
    // spec: LT-R79, LT-R77 (the operand is itself a provider)
    assertThat(Decoded.value(decodeInt("{'type':'minecraft:from_float','input':3.0}")))
        .isEqualTo(new IntProvider.FromFloat(f(3.0f)));
    assertThat(Decoded.value(decodeFloat("{'type':'minecraft:from_int','input':4}")))
        .isEqualTo(new FloatProvider.FromInt(i(4)));
  }

  @Test
  void provisionalBinaryOperatorsReadLeftAndRight() {
    // spec: LT-R79 (D for div, S for the others)
    for (String name : INT_LEFT_RIGHT) {
      IntProvider provider =
          Decoded.value(decodeInt("{'type':'minecraft:" + name + "','left':9,'right':4}"));
      assertThat(provider)
          .as(name)
          .isEqualTo(new IntProvider.Arithmetic(intOperator(name), List.of(i(9), i(4))));
    }
    for (String name : FLOAT_LEFT_RIGHT) {
      FloatProvider provider =
          Decoded.value(decodeFloat("{'type':'minecraft:" + name + "','left':9.0,'right':4.0}"));
      assertThat(provider)
          .as(name)
          .isEqualTo(new FloatProvider.Arithmetic(floatOperator(name), List.of(f(9f), f(4f))));
    }
  }

  @Test
  void provisionalPowReadsBaseAndExponentInThatOrder() {
    // spec: LT-R79 (pow: base and exponent)
    assertThat(Decoded.value(decodeInt("{'type':'minecraft:pow','base':2,'exponent':5}")))
        .isEqualTo(new IntProvider.Arithmetic(IntProvider.Operator.POW, List.of(i(2), i(5))));
    assertThat(Decoded.value(decodeFloat("{'type':'minecraft:pow','base':2.0,'exponent':3.0}")))
        .isEqualTo(new FloatProvider.Arithmetic(FloatProvider.Operator.POW, List.of(f(2f), f(3f))));
  }

  @Test
  void provisionalListOperatorsReadInputsInOrder() {
    // spec: LT-R79 (add D, mul min max avg S: a list named inputs)
    for (String name : LISTS) {
      IntProvider provider =
          Decoded.value(decodeInt("{'type':'minecraft:" + name + "','inputs':[1,2,3]}"));
      assertThat(provider)
          .as(name)
          .isEqualTo(new IntProvider.Arithmetic(intOperator(name), List.of(i(1), i(2), i(3))));
      FloatProvider floats =
          Decoded.value(decodeFloat("{'type':'minecraft:" + name + "','inputs':[1.0,2.0,3.0]}"));
      assertThat(floats)
          .as(name)
          .isEqualTo(
              new FloatProvider.Arithmetic(floatOperator(name), List.of(f(1f), f(2f), f(3f))));
    }
  }

  @Test
  void provisionalTheOperandsAreEvaluatedWithTheirNamedRole() {
    // spec: LT-R79 (left is the first operand), LT-R50: 5 - 3 = 2 and base 2 to the power 3 = 8
    var context = Ctx.context(ScriptedRandom.of());
    assertThat(
            Decoded.value(decodeInt("{'type':'minecraft:sub','left':5,'right':3}")).sample(context))
        .isEqualTo(2);
    assertThat(
            Decoded.value(decodeFloat("{'type':'minecraft:pow','base':2.0,'exponent':3.0}"))
                .sample(context))
        .isEqualTo(8.0f);
  }

  @Test
  void provisionalTheOldArgumentNameIsRefusedForUnaryOperators() {
    // spec: LT-R79 (a decoder reading argument would depart from the page), probe PL27
    for (String name : INT_UNARY) {
      assertRefusedByField(decodeInt("{'type':'minecraft:" + name + "','argument':7}"));
    }
    for (String name : FLOAT_UNARY) {
      assertRefusedByField(decodeFloat("{'type':'minecraft:" + name + "','argument':2.5}"));
    }
    assertRefusedByField(decodeInt("{'type':'minecraft:from_float','argument':3.0}"));
    assertRefusedByField(decodeFloat("{'type':'minecraft:from_int','argument':4}"));
  }

  @Test
  void provisionalAnUnaryOperatorWithoutItsOperandIsRefused() {
    // spec: LT-R79, probe PL27 (clef absente)
    assertRefusedByField(decodeInt("{'type':'minecraft:abs'}"));
    assertRefusedByField(decodeFloat("{'type':'minecraft:sqrt'}"));
    assertRefusedByField(decodeInt("{'type':'minecraft:from_float'}"));
    Decoded.failsWith(decodeFloat("{'type':'minecraft:negate'}"), LootFindingCode.MISSING_FIELD);
  }

  @Test
  void provisionalPowWithLeftAndRightIsRefusedForIntegers() {
    // spec: LT-R79 (pow is base and exponent, not left and right)
    assertRefusedByField(decodeInt("{'type':'minecraft:pow','left':2,'right':5}"));
  }

  @Test
  void provisionalPowWithLeftAndRightIsRefusedForFloats() {
    // spec: LT-R79
    assertRefusedByField(decodeFloat("{'type':'minecraft:pow','left':2.0,'right':3.0}"));
  }

  @Test
  void provisionalPowWithoutItsExponentIsRefusedWithoutException() {
    // spec: LT-R79 (a missing operand is a finding, never an exception)
    assertRefusedByField(decodeInt("{'type':'minecraft:pow','base':2}"));
    assertRefusedByField(decodeFloat("{'type':'minecraft:pow','base':2.0}"));
  }

  @Test
  void provisionalPowWithoutItsBaseIsRefusedWithoutException() {
    // spec: LT-R79
    assertRefusedByField(decodeInt("{'type':'minecraft:pow','exponent':2}"));
    assertRefusedByField(decodeFloat("{'type':'minecraft:pow','exponent':2.0}"));
  }

  @Test
  void provisionalPowWithNeitherOperandIsRefusedWithoutException() {
    // spec: LT-R79
    assertRefusedByField(decodeInt("{'type':'minecraft:pow'}"));
    assertRefusedByField(decodeFloat("{'type':'minecraft:pow'}"));
  }

  @Test
  void provisionalTheWrongOperandShapeIsRefusedForEveryOtherOperator() {
    // spec: LT-R79: left and right for the list operators, inputs for the binary ones, input for
    // the lists, are refused
    for (String name : LISTS) {
      assertRefusedByField(decodeInt("{'type':'minecraft:" + name + "','left':1,'right':2}"));
    }
    for (String name : INT_LEFT_RIGHT) {
      assertRefusedByField(decodeInt("{'type':'minecraft:" + name + "','inputs':[1,2]}"));
    }
    for (String name : INT_UNARY) {
      assertRefusedByField(decodeInt("{'type':'minecraft:" + name + "','inputs':[1]}"));
    }
  }

  @Test
  void provisionalUnaryOperatorsEncodeBackWithInput() {
    // spec: LT-R79, plan of tests L1 (round trip through a table)
    for (String name : INT_UNARY) {
      assertRoundTrip(inRolls("{'type':'minecraft:" + name + "','input':7}"));
    }
    for (String name : FLOAT_UNARY) {
      assertRoundTrip(inBonus("{'type':'minecraft:" + name + "','input':2.5}"));
    }
    assertRoundTrip(inRolls("{'type':'minecraft:from_float','input':3.5}"));
    assertRoundTrip(inBonus("{'type':'minecraft:from_int','input':4}"));
  }

  @Test
  void provisionalBinaryPowAndListOperatorsEncodeBackWithTheirFieldNames() {
    // spec: LT-R79, plan of tests L1
    for (String name : INT_LEFT_RIGHT) {
      assertRoundTrip(inRolls("{'type':'minecraft:" + name + "','left':9,'right':4}"));
    }
    for (String name : FLOAT_LEFT_RIGHT) {
      assertRoundTrip(inBonus("{'type':'minecraft:" + name + "','left':9.5,'right':4.5}"));
    }
    assertRoundTrip(inRolls("{'type':'minecraft:pow','base':2,'exponent':5}"));
    assertRoundTrip(inBonus("{'type':'minecraft:pow','base':2.5,'exponent':3.5}"));
    for (String name : LISTS) {
      assertRoundTrip(inRolls("{'type':'minecraft:" + name + "','inputs':[1,2,3]}"));
      assertRoundTrip(inBonus("{'type':'minecraft:" + name + "','inputs':[1.5,2.5]}"));
    }
  }

  @Test
  void provisionalAnEncodedTableNeverCarriesTheOldNames() {
    // spec: LT-R79: the encoding of pow writes base and exponent, of a unary writes input
    LootTable table =
        Decoded.value(
            LootTableCodec.decode(
                Json.parse(inRolls("{'type':'minecraft:pow','base':2,'exponent':5}")),
                EnchantFixtures.access(),
                LootItems.catalog()));
    String encoded = LootTableCodec.encode(table);
    assertThat(encoded).contains("\"base\"").contains("\"exponent\"");
    assertThat(encoded)
        .doesNotContain("\"left\"")
        .doesNotContain("\"right\"")
        .doesNotContain("argument");
  }
}
