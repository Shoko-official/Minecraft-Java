package dev.shoko.minecraft.loot.enchant;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.enchant.effect.EffectComponentValue;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: EN-R1 to EN-R4 (definition), EN-R5 (costs), EN-R6 (slots), EN-R7a/EN-R7b (supported and
// primary items), EN-R8 (exclusive set), EN-R11 (effects), EN-R34 (providers), V-E2, V-E3
class EnchantmentCodecTest {

  private static final String QUICK_CHARGE =
      "{'description':{'translate':'enchantment.test'},'max_level':3,'weight':5,'anvil_cost':2,"
          + "'min_cost':{'base':12,'per_level_above_first':20},"
          + "'max_cost':{'base':50,'per_level_above_first':0},"
          + "'slots':['mainhand','offhand'],'supported_items':'#minecraft:enchantable/bow'}";

  private static LootDecoding<EnchantmentDefinition> decode(String name, String json) {
    return EnchantmentCodec.decode(
        Identifier.minecraft(name), Json.parse(json), EnchantFixtures.access());
  }

  private static EnchantmentDefinition fixture(String name) {
    return Decoded.value(decode(name, EnchantFixtures.definitions().get(name)));
  }

  private static String without(String json, String key) {
    return json.replace("'" + key + "'", "'ignored_" + key + "'");
  }

  @Test
  void sharpnessReadsItsCoreFields() {
    // spec: EN-R2, EN-R4 (sharpness: 5 / 10 / 1), V-E2
    EnchantmentDefinition sharpness = fixture("sharpness");
    assertThat(sharpness.id()).isEqualTo(Identifier.minecraft("sharpness"));
    assertThat(sharpness.maxLevel()).isEqualTo(5);
    assertThat(sharpness.costs().weight()).isEqualTo(10);
    assertThat(sharpness.costs().anvilCost()).isEqualTo(1);
    assertThat(sharpness.costs().minCost()).isEqualTo(new LevelBasedValue.Linear(1f, 11f));
    assertThat(sharpness.costs().maxCost()).isEqualTo(new LevelBasedValue.Linear(21f, 11f));
    assertThat(sharpness.slots()).containsExactly(SlotGroup.MAINHAND);
  }

  @Test
  void provisionalTheCostsAreEvaluatedAtEachLevel() {
    // spec: EN-R5, V-E15 (min 1, 12, 23, 34, 45 and max 21, 32, 43, 54, 65)
    EnchantmentDefinition sharpness = fixture("sharpness");
    int[] min = {1, 12, 23, 34, 45};
    int[] max = {21, 32, 43, 54, 65};
    for (int level = 1; level <= 5; level++) {
      assertThat(sharpness.minCost(level)).as("min %d", level).isEqualTo(min[level - 1]);
      assertThat(sharpness.maxCost(level)).as("max %d", level).isEqualTo(max[level - 1]);
    }
  }

  @Test
  void provisionalTheCostsOfFortuneAndMendingAtTheirLevels() {
    // spec: EN-R5 (fortune min 15, 24, 33 and max 65, 74, 83; mending 25 and 75)
    EnchantmentDefinition fortune = fixture("fortune");
    assertThat(fortune.minCost(3)).isEqualTo(33);
    assertThat(fortune.maxCost(3)).isEqualTo(83);
    EnchantmentDefinition mending = fixture("mending");
    assertThat(mending.minCost(1)).isEqualTo(25);
    assertThat(mending.maxCost(1)).isEqualTo(75);
  }

  @Test
  void sharpnessSupportsOneTagAndPrefersAnotherForTheTable() {
    // spec: EN-R7a (sharp_weapon supported, melee_weapon primary)
    EnchantmentItems items = fixture("sharpness").items();
    assertThat(items.supported().tags())
        .containsExactly(Identifier.parse("minecraft:enchantable/sharp_weapon"));
    assertThat(items.hasPrimary()).isTrue();
    assertThat(items.primary().tags())
        .containsExactly(Identifier.parse("minecraft:enchantable/melee_weapon"));
  }

  @Test
  void provisionalADefinitionWithoutPrimaryItemsUsesTheSupportedOnes() {
    // spec: EN-R7b (absent signifie egal a supported_items)
    EnchantmentItems items = fixture("smite").items();
    assertThat(items.hasPrimary()).isFalse();
    assertThat(items.primary()).isEqualTo(items.supported());
  }

  @Test
  void theExclusiveSetIsReadAsATag() {
    // spec: EN-R8
    EnchantmentDefinition sharpness = fixture("sharpness");
    assertThat(sharpness.exclusiveSet().tags())
        .containsExactly(Identifier.parse("minecraft:exclusive_set/damage"));
    EnchantmentDefinition mending = fixture("mending");
    assertThat(mending.exclusiveSet().ids()).isEmpty();
    assertThat(mending.exclusiveSet().tags()).isEmpty();
  }

  @Test
  void sharpnessCarriesADamageComponent() {
    // spec: V-E3 (damage add linear base 1.0 per_level_above_first 0.5), EN-R11
    EnchantmentDefinition sharpness = fixture("sharpness");
    EffectComponentValue damage =
        sharpness.effects().get(Identifier.minecraft("damage")).orElseThrow();
    assertThat(damage).isInstanceOf(EffectComponentValue.ValueEffects.class);
    assertThat(((EffectComponentValue.ValueEffects) damage).entries()).hasSize(1);
  }

  @Test
  void fortuneHasNoEffects() {
    // spec: EN-R2 (fortune is the only vanilla enchantment without effects)
    assertThat(fixture("fortune").effects().components()).isEmpty();
  }

  @Test
  void severalSlotGroupsAreKeptInOrder() {
    // spec: EN-R3 (quick_charge: mainhand and offhand)
    EnchantmentDefinition quickCharge = Decoded.value(decode("quick_charge", QUICK_CHARGE));
    assertThat(quickCharge.slots()).containsExactly(SlotGroup.MAINHAND, SlotGroup.OFFHAND);
    assertThat(quickCharge.maxLevel()).isEqualTo(3);
  }

  @Test
  void everySlotGroupNameIsKnown() {
    // spec: EN-R6 (mainhand, offhand, hand, head, chest, legs, feet, armor, any, body)
    List<String> names =
        List.of(
            "mainhand", "offhand", "hand", "head", "chest", "legs", "feet", "armor", "any", "body");
    List<SlotGroup> expected =
        List.of(
            SlotGroup.MAINHAND,
            SlotGroup.OFFHAND,
            SlotGroup.HAND,
            SlotGroup.HEAD,
            SlotGroup.CHEST,
            SlotGroup.LEGS,
            SlotGroup.FEET,
            SlotGroup.ARMOR,
            SlotGroup.ANY,
            SlotGroup.BODY);
    for (int i = 0; i < names.size(); i++) {
      String json = QUICK_CHARGE.replace("['mainhand','offhand']", "['" + names.get(i) + "']");
      assertThat(Decoded.value(decode("slots_test", json)).slots())
          .containsExactly(expected.get(i));
    }
  }

  @Test
  void aMissingRequiredFieldIsReported() {
    // spec: EN-R2 (description, max_level, weight, anvil_cost, min_cost, max_cost, slots,
    // supported_items)
    for (String key :
        List.of(
            "description",
            "max_level",
            "weight",
            "anvil_cost",
            "min_cost",
            "max_cost",
            "slots",
            "supported_items")) {
      Decoded.failsWith(
          decode("broken", without(QUICK_CHARGE, key)), LootFindingCode.MISSING_FIELD);
    }
  }

  @Test
  void aNonPositiveMaxLevelIsOutOfRange() {
    // spec: EnchantmentCodec#decode contract (VALUE_RANGE), EN-R3
    Decoded.failsWith(
        decode("broken", QUICK_CHARGE.replace("'max_level':3", "'max_level':0")),
        LootFindingCode.VALUE_RANGE);
    Decoded.failsWith(
        decode("broken", QUICK_CHARGE.replace("'max_level':3", "'max_level':-1")),
        LootFindingCode.VALUE_RANGE);
  }

  @Test
  void anUnknownEffectComponentIsRefused() {
    // spec: EnchantmentCodec#decode contract (UNKNOWN_TYPE for a component outside the 31)
    String json = QUICK_CHARGE.replace("'slots'", "'effects':{'minecraft:no_such':[]},'slots'");
    Decoded.failsWith(decode("broken", json), LootFindingCode.UNKNOWN_TYPE);
  }

  @Test
  void aTagThatIsNotLoadedIsReported() {
    // spec: MatchCodecs contract (UNRESOLVED_TAG)
    String json =
        QUICK_CHARGE.replace("#minecraft:enchantable/bow", "#minecraft:enchantable/ghost");
    Decoded.failsWith(decode("broken", json), LootFindingCode.UNRESOLVED_TAG);
  }

  @Test
  void everyDecodingRespectsTheLootDecodingContract() {
    // spec: LootDecoding contract
    Decoded.assertWellFormed(decode("ok", QUICK_CHARGE));
    Decoded.assertWellFormed(decode("broken", without(QUICK_CHARGE, "weight")));
  }

  @Test
  void aSingleProviderReadsItsEnchantmentAndLevel() {
    // spec: EN-R34 (single: enchantment and level)
    EnchantmentProvider provider =
        Decoded.value(
            EnchantmentCodec.decodeProvider(
                Json.parse(
                    "{'type':'minecraft:single','enchantment':'minecraft:sharpness','level':2}"),
                EnchantFixtures.access()));
    assertThat(provider)
        .isEqualTo(new EnchantmentProvider.Single(Identifier.minecraft("sharpness"), 2));
  }

  @Test
  void aCostProviderReadsItsTagAndItsCosts() {
    // spec: EN-R34, V-E8 (by_cost_with_difficulty: tag, min_cost 5, max_cost_span 17)
    EnchantmentProvider provider =
        Decoded.value(
            EnchantmentCodec.decodeProvider(
                Json.parse(
                    "{'type':'minecraft:by_cost_with_difficulty','enchantments':'#minecraft:non_treasure',"
                        + "'min_cost':5,'max_cost_span':17}"),
                EnchantFixtures.access()));
    assertThat(provider).isInstanceOf(EnchantmentProvider.ByCostWithDifficulty.class);
    EnchantmentProvider.ByCostWithDifficulty cost =
        (EnchantmentProvider.ByCostWithDifficulty) provider;
    assertThat(cost.minCost()).isEqualTo(5);
    assertThat(cost.maxCostSpan()).isEqualTo(17);
    assertThat(cost.enchantments().tags()).containsExactly(Identifier.minecraft("non_treasure"));
  }

  @Test
  void byCostReadsTheSameFieldsWithoutDifficulty() {
    // spec: EN-R34
    EnchantmentProvider provider =
        Decoded.value(
            EnchantmentCodec.decodeProvider(
                Json.parse(
                    "{'type':'minecraft:by_cost','enchantments':'#minecraft:non_treasure','min_cost':1,"
                        + "'max_cost_span':2}"),
                EnchantFixtures.access()));
    assertThat(provider).isInstanceOf(EnchantmentProvider.ByCost.class);
  }

  @Test
  void anUnknownProviderTypeIsRefused() {
    // spec: EnchantmentCodec#decodeProvider contract (UNKNOWN_TYPE)
    Decoded.failsWith(
        EnchantmentCodec.decodeProvider(
            Json.parse("{'type':'minecraft:no_such'}"), EnchantFixtures.access()),
        LootFindingCode.UNKNOWN_TYPE);
  }
}
