package dev.shoko.minecraft.loot.number;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

// spec: LT-R50 (23 int types, 28 float types, bare number = constant, string = reference),
// LT-R51 to LT-R53 (forms of uniform and binomial)
class NumberProviderCodecTest {

  private static LootDecoding<IntProvider> decodeInt(String json) {
    return NumberProviderCodec.decodeInt(Json.parse(json), EnchantFixtures.access());
  }

  private static LootDecoding<FloatProvider> decodeFloat(String json) {
    return NumberProviderCodec.decodeFloat(Json.parse(json), EnchantFixtures.access());
  }

  private static List<Identifier> ids(String... names) {
    return Stream.of(names).map(Identifier::minecraft).toList();
  }

  @Test
  void aBareNumberIsAnIntegerConstant() {
    // spec: LT-R50
    assertThat(Decoded.value(decodeInt("3"))).isEqualTo(new IntProvider.Constant(3));
    assertThat(Decoded.value(decodeInt("0"))).isEqualTo(new IntProvider.Constant(0));
  }

  @Test
  void aBareNumberIsAFloatConstant() {
    // spec: LT-R50
    assertThat(Decoded.value(decodeFloat("0.5"))).isEqualTo(new FloatProvider.Constant(0.5f));
    assertThat(Decoded.value(decodeFloat("2"))).isEqualTo(new FloatProvider.Constant(2f));
  }

  @Test
  void anIntegerUniformHasInclusiveBounds() {
    // spec: LT-R51 ({"type":"minecraft:uniform","max":3,"min":1})
    IntProvider decoded = Decoded.value(decodeInt("{'type':'minecraft:uniform','max':3,'min':1}"));
    assertThat(decoded)
        .isEqualTo(
            new IntProvider.Uniform(new IntProvider.Constant(1), new IntProvider.Constant(3)));
  }

  @Test
  void aFloatUniformReadsMinAndMax() {
    // spec: LT-R52 (enchanted_count_increase: uniform 0.0 to 1.0)
    FloatProvider decoded =
        Decoded.value(decodeFloat("{'type':'minecraft:uniform','min':0.0,'max':1.0}"));
    assertThat(decoded)
        .isEqualTo(
            new FloatProvider.Uniform(
                new FloatProvider.Constant(0f), new FloatProvider.Constant(1f)));
  }

  @Test
  void aBinomialReadsNAndP() {
    // spec: LT-R53 (melon seeds: n=3, p=0.53333336)
    IntProvider decoded =
        Decoded.value(decodeInt("{'type':'minecraft:binomial','n':3,'p':0.53333336}"));
    assertThat(decoded)
        .isEqualTo(
            new IntProvider.Binomial(
                new IntProvider.Constant(3), new FloatProvider.Constant(0.53333336f)));
  }

  @Test
  void aStringIsAReferenceToANamedProvider() {
    // spec: LT-R50 (named providers)
    assertThat(Decoded.value(decodeInt("'minecraft:cooking/time_coal'")))
        .isEqualTo(new IntProvider.Reference(Identifier.parse("minecraft:cooking/time_coal")));
    assertThat(Decoded.value(decodeFloat("'minecraft:cooking/speed_default'")))
        .isEqualTo(
            new FloatProvider.Reference(Identifier.parse("minecraft:cooking/speed_default")));
  }

  @Test
  void anEnchantmentLevelProviderReadsItsAmount() {
    // spec: LT-R30 ({"type":"minecraft:enchantment_level","amount":...}), EN-R20
    FloatProvider decoded =
        Decoded.value(
            decodeFloat(
                "{'type':'minecraft:enchantment_level','amount':"
                    + "{'type':'minecraft:linear','base':0.5,'per_level_above_first':0.25}}"));
    assertThat(decoded)
        .isEqualTo(new FloatProvider.EnchantmentLevel(new LevelBasedValue.Linear(0.5f, 0.25f)));
  }

  @Test
  void anUnknownTypeIsRefused() {
    // spec: LT-R50 (a type outside the registry is UNKNOWN_TYPE)
    Decoded.failsWith(decodeInt("{'type':'minecraft:no_such_type'}"), LootFindingCode.UNKNOWN_TYPE);
    Decoded.failsWith(
        decodeFloat("{'type':'minecraft:no_such_type'}"), LootFindingCode.UNKNOWN_TYPE);
  }

  @Test
  void aFloatOnlyTypeIsRefusedAsAnIntegerProvider() {
    // spec: LT-R50 (enchantment_level is in the 28 float types, not in the 23 int types)
    Decoded.failsWith(
        decodeInt("{'type':'minecraft:enchantment_level','amount':1}"),
        LootFindingCode.UNKNOWN_TYPE);
  }

  @Test
  void aMissingFieldIsReported() {
    // spec: LT-R51 (uniform needs min and max)
    Decoded.failsWith(
        decodeInt("{'type':'minecraft:uniform','min':1}"), LootFindingCode.MISSING_FIELD);
  }

  @Test
  void anObjectWithoutTypeIsRefused() {
    // spec: LT-R3 (type is the discriminant)
    Decoded.fails(decodeInt("{'min':1,'max':2}"));
  }

  @Test
  void aWrongKindOfValueIsATypeMismatch() {
    // spec: LT-R50 (number, object or string only)
    Decoded.failsWith(decodeInt("true"), LootFindingCode.TYPE_MISMATCH);
    Decoded.failsWith(decodeInt("[1,2]"), LootFindingCode.TYPE_MISMATCH);
    Decoded.failsWith(decodeFloat("null"), LootFindingCode.TYPE_MISMATCH);
  }

  @Test
  void everyFindingCarriesAPathRootedAtTheDocument() {
    // spec: LootDecoding contract
    Decoded.assertWellFormed(decodeInt("{'type':'minecraft:uniform','min':1}"));
    Decoded.assertWellFormed(decodeInt("{'type':'minecraft:uniform','min':1,'max':2}"));
  }

  @Test
  void theRegisteredIntegerTypesAreTheTwentyThree() {
    // spec: LT-R50 (context_int_provider_type, 23)
    assertThat(NumberProviderCodec.registeredTypes(false))
        .containsExactlyInAnyOrderElementsOf(
            ids(
                "constant",
                "uniform",
                "binomial",
                "weighted_list",
                "conditional",
                "number_dispatcher",
                "score",
                "storage",
                "environment_attribute",
                "from_float",
                "abs",
                "avg",
                "add",
                "sub",
                "mul",
                "div",
                "mod",
                "floor_div",
                "floor_mod",
                "negate",
                "pow",
                "min",
                "max"));
  }

  @Test
  void theRegisteredFloatTypesAreTheTwentyEight() {
    // spec: LT-R50 (context_float_provider_type, 28)
    assertThat(NumberProviderCodec.registeredTypes(true))
        .containsExactlyInAnyOrderElementsOf(
            ids(
                "constant",
                "uniform",
                "weighted_list",
                "conditional",
                "number_dispatcher",
                "storage",
                "environment_attribute",
                "enchantment_level",
                "from_int",
                "length",
                "abs",
                "avg",
                "ceil",
                "floor",
                "round",
                "truncate",
                "add",
                "sub",
                "mul",
                "div",
                "mod",
                "negate",
                "pow",
                "sqrt",
                "sin",
                "cos",
                "min",
                "max"));
  }
}
