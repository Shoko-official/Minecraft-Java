package dev.shoko.minecraft.loot.modifier;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentChange;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: LT-R3, LT-R38 to LT-R47 (JSON forms of the item modifiers)
class ModifierCodecTest {

  private static LootDecoding<ItemModifier> decode(String json) {
    return ModifierCodec.decode(Json.parse(json), EnchantFixtures.access(), LootItems.catalog());
  }

  private static ModifierAction action(String json) {
    ItemModifier modifier = Decoded.value(decode(json));
    return modifier.action();
  }

  private static IntProvider.Constant c(int value) {
    return new IntProvider.Constant(value);
  }

  @Test
  void setCountReadsAConstantAndDefaultsToReplace() {
    // spec: LT-R39 (add defaults to false)
    ItemModifier modifier = Decoded.value(decode("{'type':'minecraft:set_count','count':3}"));
    assertThat(modifier.conditions()).isEmpty();
    assertThat(modifier.action()).isEqualTo(new ModifierAction.SetCount(c(3), false));
  }

  @Test
  void setCountReadsUniformBinomialAndAdd() {
    // spec: LT-R39 (forms of count: constant, uniform, binomial)
    assertThat(
            action(
                "{'type':'minecraft:set_count','add':true,"
                    + "'count':{'type':'minecraft:uniform','min':1,'max':3}}"))
        .isEqualTo(new ModifierAction.SetCount(new IntProvider.Uniform(c(1), c(3)), true));
    assertThat(
            action(
                "{'type':'minecraft:set_count','count':{'type':'minecraft:binomial','n':3,'p':0.5}}"))
        .isEqualTo(
            new ModifierAction.SetCount(
                new IntProvider.Binomial(c(3), new FloatProvider.Constant(0.5f)), false));
  }

  @Test
  void provisionalEveryModifierAcceptsACondition() {
    // spec: LT-R38 (condition on every modifier, inline or reference)
    ItemModifier inline =
        Decoded.value(
            decode(
                "{'type':'minecraft:set_count','count':1,'condition':"
                    + "{'type':'minecraft:killed_by_player'}}"));
    assertThat(inline.conditions()).containsExactly(new LootCondition.KilledByPlayer());
    ItemModifier reference =
        Decoded.value(
            decode("{'type':'minecraft:furnace_smelt','condition':'minecraft:tool/can_shear'}"));
    assertThat(reference.conditions())
        .containsExactly(new LootCondition.Reference(Identifier.parse("minecraft:tool/can_shear")));
    assertThat(reference.action()).isEqualTo(new ModifierAction.FurnaceSmelt());
  }

  @Test
  void limitCountReadsItsBounds() {
    // spec: LT-R40 ({"min":0}, {"max":9}, {"max":4,"min":1}); an absent bound clamps nothing
    assertThat(action("{'type':'minecraft:limit_count','limit':{'min':0}}"))
        .isEqualTo(new ModifierAction.LimitCount(c(0), c(Integer.MAX_VALUE)));
    assertThat(action("{'type':'minecraft:limit_count','limit':{'max':9}}"))
        .isEqualTo(new ModifierAction.LimitCount(c(Integer.MIN_VALUE), c(9)));
    assertThat(action("{'type':'minecraft:limit_count','limit':{'max':4,'min':1}}"))
        .isEqualTo(new ModifierAction.LimitCount(c(1), c(4)));
  }

  @Test
  void explosionDecayAndDiscardHaveNoField() {
    // spec: LT-R41, LT-R46
    assertThat(action("{'type':'minecraft:explosion_decay'}"))
        .isEqualTo(new ModifierAction.ExplosionDecay());
    assertThat(action("{'type':'minecraft:discard'}")).isEqualTo(new ModifierAction.Discard());
  }

  @Test
  void applyBonusReadsTheThreeFormulas() {
    // spec: LT-R42 (ore_drops, uniform_bonus_count, binomial_with_bonus_count)
    Identifier fortune = Identifier.minecraft("fortune");
    assertThat(
            action(
                "{'type':'minecraft:apply_bonus','enchantment':'minecraft:fortune',"
                    + "'formula':'minecraft:ore_drops'}"))
        .isEqualTo(new ModifierAction.ApplyBonus(fortune, new BonusFormula.OreDrops()));
    assertThat(
            action(
                "{'type':'minecraft:apply_bonus','enchantment':'minecraft:fortune',"
                    + "'formula':'minecraft:uniform_bonus_count','parameters':{'bonusMultiplier':2}}"))
        .isEqualTo(new ModifierAction.ApplyBonus(fortune, new BonusFormula.UniformBonusCount(2)));
    assertThat(
            action(
                "{'type':'minecraft:apply_bonus','enchantment':'minecraft:fortune',"
                    + "'formula':'minecraft:binomial_with_bonus_count',"
                    + "'parameters':{'extra':3,'probability':0.5714286}}"))
        .isEqualTo(
            new ModifierAction.ApplyBonus(
                fortune, new BonusFormula.BinomialWithBonusCount(3, 0.5714286f)));
  }

  @Test
  void enchantedCountIncreaseReadsCountEnchantmentAndLimit() {
    // spec: LT-R43 (uniform 0.0 to 1.0, looting, limit 1)
    FloatProvider count =
        new FloatProvider.Uniform(new FloatProvider.Constant(0f), new FloatProvider.Constant(1f));
    assertThat(
            action(
                "{'type':'minecraft:enchanted_count_increase','enchantment':'minecraft:looting',"
                    + "'count':{'type':'minecraft:uniform','min':0.0,'max':1.0},'limit':1}"))
        .isEqualTo(
            new ModifierAction.EnchantedCountIncrease(count, Identifier.minecraft("looting"), 1));
    ModifierAction unlimited =
        action(
            "{'type':'minecraft:enchanted_count_increase','enchantment':'minecraft:looting',"
                + "'count':{'type':'minecraft:uniform','min':0.0,'max':1.0}}");
    assertThat(((ModifierAction.EnchantedCountIncrease) unlimited).limit()).isZero();
  }

  @Test
  void copyComponentsReadsSourceAndInclude() {
    // spec: LT-R44j, V-L8 (source block_entity, include pot_decorations)
    ModifierAction.CopyComponents copy =
        (ModifierAction.CopyComponents)
            action(
                "{'type':'minecraft:copy_components','source':'block_entity',"
                    + "'include':['minecraft:pot_decorations']}");
    assertThat(copy.source()).isEqualTo(ModifierAction.ComponentSource.BLOCK_ENTITY);
    assertThat(copy.include()).containsExactly(Identifier.parse("minecraft:pot_decorations"));
    assertThat(copy.exclude()).isEmpty();
  }

  @Test
  void copyStateReadsTheBlockAndTheProperties() {
    // spec: LT-R44i
    assertThat(
            action(
                "{'type':'minecraft:copy_state','block':'minecraft:wheat','properties':['age']}"))
        .isEqualTo(new ModifierAction.CopyState(Identifier.minecraft("wheat"), List.of("age")));
  }

  @Test
  void enchantRandomlyDefaultsToOnlyCompatible() {
    // spec: LT-R45 (only_compatible true, include_additional_cost_component false)
    ModifierAction.EnchantRandomly defaults =
        (ModifierAction.EnchantRandomly)
            action("{'type':'minecraft:enchant_randomly','options':'#minecraft:non_treasure'}");
    assertThat(defaults.onlyCompatible()).isTrue();
    assertThat(defaults.includeAdditionalCostComponent()).isFalse();
    assertThat(defaults.options().tags()).containsExactly(Identifier.minecraft("non_treasure"));
    ModifierAction.EnchantRandomly loose =
        (ModifierAction.EnchantRandomly)
            action(
                "{'type':'minecraft:enchant_randomly','only_compatible':false,"
                    + "'options':['minecraft:sharpness']}");
    assertThat(loose.onlyCompatible()).isFalse();
  }

  @Test
  void enchantWithLevelsReadsTheLevelsAndAnOptionalSet() {
    // spec: LT-R45 (levels uniform 20 to 39, options optional: all enchantments if absent)
    ModifierAction.EnchantWithLevels all =
        (ModifierAction.EnchantWithLevels)
            action(
                "{'type':'minecraft:enchant_with_levels',"
                    + "'levels':{'type':'minecraft:uniform','min':20,'max':39}}");
    assertThat(all.levels()).isEqualTo(new IntProvider.Uniform(c(20), c(39)));
    assertThat(all.options().unrestricted()).isTrue();
    ModifierAction.EnchantWithLevels some =
        (ModifierAction.EnchantWithLevels)
            action(
                "{'type':'minecraft:enchant_with_levels','levels':30,"
                    + "'options':'#minecraft:non_treasure'}");
    assertThat(some.levels()).isEqualTo(c(30));
    assertThat(some.options().unrestricted()).isFalse();
  }

  @Test
  void filteredReadsItsFilterAndItsBranches() {
    // spec: LT-R46 (item_filter with map_id, on_fail discard)
    ModifierAction.Filtered filtered =
        (ModifierAction.Filtered)
            action(
                "{'type':'minecraft:filtered','item_filter':{'predicates':{'minecraft:map_id':{}}},"
                    + "'on_fail':{'type':'minecraft:discard'}}");
    assertThat(filtered.onPass()).isEmpty();
    assertThat(filtered.onFail()).hasSize(1);
    assertThat(filtered.onFail().getFirst().action()).isEqualTo(new ModifierAction.Discard());
  }

  @Test
  void setComponentsReadsTheComponentsToSetAndToRemove() {
    // spec: LT-R44h (a "!" key removes a component)
    ModifierAction.SetComponents set =
        (ModifierAction.SetComponents)
            action(
                "{'type':'minecraft:set_components','components':"
                    + "{'minecraft:custom_name':'Hello','!minecraft:lore':{}}}");
    assertThat(set.patch().changes().get(Identifier.minecraft("custom_name")))
        .isInstanceOf(ComponentChange.Set.class);
    assertThat(set.patch().changes().get(Identifier.minecraft("lore")))
        .isInstanceOf(ComponentChange.Removed.class);
  }

  @Test
  void setNameReadsTheNameAndTheTarget() {
    // spec: LT-R44e
    ModifierAction.SetName name =
        (ModifierAction.SetName)
            action("{'type':'minecraft:set_name','name':'Gem','target':'custom_name'}");
    assertThat(name.target()).isEqualTo(ModifierAction.NameTarget.CUSTOM_NAME);
    assertThat(name.name().sameValueAs(Json.parse("'Gem'"))).isTrue();
    ModifierAction.SetName item =
        (ModifierAction.SetName)
            action("{'type':'minecraft:set_name','name':{'text':'Gem'},'target':'item_name'}");
    assertThat(item.target()).isEqualTo(ModifierAction.NameTarget.ITEM_NAME);
  }

  @Test
  void setPotionSetDamageAndSetEnchantmentsReadTheirFields() {
    // spec: LT-R44a, LT-R44f, LT-R44g
    assertThat(action("{'type':'minecraft:set_potion','id':'minecraft:healing'}"))
        .isEqualTo(new ModifierAction.SetPotion(Identifier.minecraft("healing")));
    assertThat(
            action(
                "{'type':'minecraft:set_damage','damage':"
                    + "{'type':'minecraft:uniform','min':0.15,'max':0.8}}"))
        .isEqualTo(
            new ModifierAction.SetDamage(
                new FloatProvider.Uniform(
                    new FloatProvider.Constant(0.15f), new FloatProvider.Constant(0.8f)),
                false));
    ModifierAction.SetEnchantments enchantments =
        (ModifierAction.SetEnchantments)
            action(
                "{'type':'minecraft:set_enchantments','add':true,"
                    + "'enchantments':{'minecraft:sharpness':3}}");
    assertThat(enchantments.add()).isTrue();
    assertThat(enchantments.enchantments())
        .containsEntry(Identifier.minecraft("sharpness"), c(3))
        .hasSize(1);
  }

  @Test
  void setOminousBottleAmplifierAndSetStewEffectReadTheirFields() {
    // spec: LT-R44b, LT-R44c
    assertThat(action("{'type':'minecraft:set_ominous_bottle_amplifier','amplifier':2}"))
        .isEqualTo(new ModifierAction.SetOminousBottleAmplifier(c(2)));
    ModifierAction.SetStewEffect stew =
        (ModifierAction.SetStewEffect)
            action(
                "{'type':'minecraft:set_stew_effect','effects':"
                    + "[{'type':'minecraft:night_vision','duration':100}]}");
    assertThat(stew.effects()).hasSize(1);
    assertThat(stew.effects().getFirst().effect()).isEqualTo(Identifier.minecraft("night_vision"));
    assertThat(stew.effects().getFirst().duration()).isEqualTo(c(100));
  }

  @Test
  void provisionalATypeOfTheTwentyOneWithoutBehaviorIsKeptAsDeferredWithAWarning() {
    // spec: LT-R47 (decoded, kept, NOT_IMPLEMENTED_MODIFIER warning)
    LootDecoding<ItemModifier> decoding =
        decode("{'type':'minecraft:set_lore','lore':[],'mode':'append'}");
    ItemModifier modifier = Decoded.warnings(decoding, LootFindingCode.NOT_IMPLEMENTED_MODIFIER);
    assertThat(modifier.action()).isInstanceOf(ModifierAction.Deferred.class);
    assertThat(((ModifierAction.Deferred) modifier.action()).type())
        .isEqualTo(Identifier.minecraft("set_lore"));
  }

  @Test
  void provisionalTheDeferredWarningIsGivenOncePerTypePerCall() {
    // spec: LT-R47
    LootDecoding<List<ItemModifier>> decoding =
        ModifierCodec.decodeList(
            Json.parse(
                "[{'type':'minecraft:set_lore','lore':[]},{'type':'minecraft:set_lore','lore':[]},"
                    + "{'type':'minecraft:set_item','item':'minecraft:stone'}]"),
            EnchantFixtures.access(),
            LootItems.catalog());
    Decoded.warnings(
        decoding,
        LootFindingCode.NOT_IMPLEMENTED_MODIFIER,
        LootFindingCode.NOT_IMPLEMENTED_MODIFIER);
  }

  @Test
  void provisionalSetRandomDyesUsesTheFieldNumberOfDyes() {
    // spec: LT-R47 (the data prevail over the documentation: number_of_dyes)
    LootDecoding<ItemModifier> decoding =
        decode("{'type':'minecraft:set_random_dyes','number_of_dyes':2}");
    assertThat(Decoded.value(decoding).action()).isInstanceOf(ModifierAction.Deferred.class);
  }

  @Test
  void decodeListReadsAnObjectOrAList() {
    // spec: LT-R4a (modifier: an object or a list), LT-R4b (a list applies in order)
    List<ItemModifier> one =
        Decoded.value(
            ModifierCodec.decodeList(
                Json.parse("{'type':'minecraft:discard'}"),
                EnchantFixtures.access(),
                LootItems.catalog()));
    assertThat(one).hasSize(1);
    List<ItemModifier> two =
        Decoded.value(
            ModifierCodec.decodeList(
                Json.parse(
                    "[{'type':'minecraft:set_count','count':2},{'type':'minecraft:explosion_decay'}]"),
                EnchantFixtures.access(),
                LootItems.catalog()));
    assertThat(two.get(0).action()).isEqualTo(new ModifierAction.SetCount(c(2), false));
    assertThat(two.get(1).action()).isEqualTo(new ModifierAction.ExplosionDecay());
  }

  @Test
  void anUnknownTypeIsRefused() {
    // spec: LT-R3 (42 registered types)
    Decoded.failsWith(
        decode("{'type':'minecraft:no_such_function'}"), LootFindingCode.UNKNOWN_TYPE);
  }

  @Test
  void theFormOfEarlierVersionsIsNotDecoded() {
    // spec: LT-R3 ({"function": ...} as discriminant is out of scope)
    Decoded.fails(decode("{'function':'minecraft:set_count','count':1}"));
  }

  @Test
  void anEnchantmentThatIsNotLoadedIsReported() {
    // spec: ModifierCodec#decode contract (UNKNOWN_ENCHANTMENT)
    Decoded.failsWith(
        decode(
            "{'type':'minecraft:apply_bonus','enchantment':'minecraft:no_such','formula':"
                + "'minecraft:ore_drops'}"),
        LootFindingCode.UNKNOWN_ENCHANTMENT);
  }

  @Test
  void aMissingRequiredFieldIsReported() {
    // spec: LT-R39 (count is needed)
    Decoded.failsWith(decode("{'type':'minecraft:set_count'}"), LootFindingCode.MISSING_FIELD);
  }

  @Test
  void everyDecodingRespectsTheLootDecodingContract() {
    // spec: LootDecoding contract
    Decoded.assertWellFormed(decode("{'type':'minecraft:set_count','count':1}"));
    Decoded.assertWellFormed(decode("{'type':'minecraft:set_count'}"));
    Decoded.assertWellFormed(decode("{'type':'minecraft:no_such_function'}"));
  }
}
