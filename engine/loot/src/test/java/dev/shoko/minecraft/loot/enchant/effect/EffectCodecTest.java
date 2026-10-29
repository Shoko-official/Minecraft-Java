package dev.shoko.minecraft.loot.enchant.effect;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: EN-R11, EN-R12 (three forms of component value), EN-R13/EN-R14 (requirements, enchanted,
// affected), EN-R17 (value effects), EN-R20 (level-based values), EN-R26 to EN-R28 (entity effects)
class EffectCodecTest {

  private static LevelBasedValue level(String json) {
    return Decoded.value(EffectCodec.levelBased(Json.parse(json)));
  }

  private static ValueEffect value(String json) {
    return Decoded.value(EffectCodec.valueEffect(Json.parse(json)));
  }

  private static EffectComponents components(String json) {
    return Decoded.value(EffectCodec.components(Json.parse(json), EnchantFixtures.access()));
  }

  private static LootDecoding<EffectComponents> decodeComponents(String json) {
    return EffectCodec.components(Json.parse(json), EnchantFixtures.access());
  }

  private static EffectComponentValue component(String json, String name) {
    return components(json).get(Identifier.minecraft(name)).orElseThrow();
  }

  @Test
  void aBareNumberIsAConstantLevelBasedValue() {
    // spec: EN-R20
    assertThat(level("0.33333334")).isEqualTo(new LevelBasedValue.Constant(0.33333334f));
    assertThat(level("2")).isEqualTo(new LevelBasedValue.Constant(2f));
  }

  @Test
  void theSixLevelBasedTypesDecode() {
    // spec: EN-R20 (linear, levels_squared, fraction, clamped, lookup, exponent)
    assertThat(level("{'type':'minecraft:linear','base':1.0,'per_level_above_first':0.5}"))
        .isEqualTo(new LevelBasedValue.Linear(1f, 0.5f));
    assertThat(level("{'type':'minecraft:levels_squared','added':1.0}"))
        .isEqualTo(new LevelBasedValue.LevelsSquared(1f));
    assertThat(
            level(
                "{'type':'minecraft:fraction','numerator':{'type':'minecraft:linear','base':1,"
                    + "'per_level_above_first':1},'denominator':2}"))
        .isEqualTo(
            new LevelBasedValue.Fraction(
                new LevelBasedValue.Linear(1f, 1f), new LevelBasedValue.Constant(2f)));
    assertThat(level("{'type':'minecraft:clamped','value':3,'min':1,'max':2}"))
        .isEqualTo(new LevelBasedValue.Clamped(new LevelBasedValue.Constant(3f), 1f, 2f));
    assertThat(level("{'type':'minecraft:exponent','base':2,'power':3}"))
        .isEqualTo(
            new LevelBasedValue.Exponent(
                new LevelBasedValue.Constant(2f), new LevelBasedValue.Constant(3f)));
  }

  @Test
  void lookupReadsItsValuesAndItsFallback() {
    // spec: EN-R20, V-E7 (wind_burst: lookup of [1.2, 1.75, 2.2], fallback linear 1.5 / 0.35)
    LevelBasedValue lookup =
        level(
            "{'type':'minecraft:lookup','values':[1.2,1.75,2.2],"
                + "'fallback':{'type':'minecraft:linear','base':1.5,'per_level_above_first':0.35}}");
    assertThat(lookup)
        .isEqualTo(
            new LevelBasedValue.Lookup(
                List.of(1.2f, 1.75f, 2.2f), new LevelBasedValue.Linear(1.5f, 0.35f)));
  }

  @Test
  void anUnknownLevelBasedTypeIsRefused() {
    // spec: EN-R20 (6 registered types)
    Decoded.failsWith(
        EffectCodec.levelBased(Json.parse("{'type':'minecraft:no_such'}")),
        LootFindingCode.UNKNOWN_TYPE);
  }

  @Test
  void theValueEffectsDecode() {
    // spec: EN-R17 (add, set, multiply, remove_binomial, all_of, exponential)
    LevelBasedValue.Constant one = new LevelBasedValue.Constant(1f);
    assertThat(value("{'type':'minecraft:add','value':1}")).isEqualTo(new ValueEffect.Add(one));
    assertThat(value("{'type':'minecraft:set','value':1}")).isEqualTo(new ValueEffect.Set(one));
    assertThat(value("{'type':'minecraft:multiply','factor':1}"))
        .isEqualTo(new ValueEffect.Multiply(one));
    assertThat(value("{'type':'minecraft:remove_binomial','chance':1}"))
        .isEqualTo(new ValueEffect.RemoveBinomial(one));
    assertThat(value("{'type':'minecraft:exponential','factor':1}"))
        .isEqualTo(new ValueEffect.Exponential(one));
    assertThat(
            value(
                "{'type':'minecraft:all_of','effects':[{'type':'minecraft:add','value':1},"
                    + "{'type':'minecraft:multiply','factor':1}]}"))
        .isEqualTo(
            new ValueEffect.AllOf(
                List.of(new ValueEffect.Add(one), new ValueEffect.Multiply(one))));
  }

  @Test
  void anEmptyObjectIsTheEmptyValueEffect() {
    // spec: EN-R17 (damage_immunity has an empty object as effect)
    assertThat(value("{}")).isEqualTo(new ValueEffect.Empty());
  }

  @Test
  void anUnknownValueEffectIsRefused() {
    // spec: EN-R17 (6 registered types)
    Decoded.failsWith(
        EffectCodec.valueEffect(Json.parse("{'type':'minecraft:no_such'}")),
        LootFindingCode.UNKNOWN_TYPE);
  }

  @Test
  void aListComponentReadsItsValueEffectsAndRequirements() {
    // spec: EN-R12 (a), EN-R13 (requirements)
    EffectComponentValue damage =
        component(
            "{'minecraft:damage':[{'effect':{'type':'minecraft:add','value':1}},"
                + "{'effect':{'type':'minecraft:add','value':2},"
                + "'requirements':{'type':'minecraft:killed_by_player'}}]}",
            "damage");
    assertThat(damage).isInstanceOf(EffectComponentValue.ValueEffects.class);
    List<ConditionalValueEffect> entries = ((EffectComponentValue.ValueEffects) damage).entries();
    assertThat(entries).hasSize(2);
    assertThat(entries.get(0).requirements()).isEmpty();
    assertThat(entries.get(1).requirements()).containsExactly(new LootCondition.KilledByPlayer());
  }

  @Test
  void aDirectValueComponentHasNoList() {
    // spec: EN-R12 (b: crossbow_charge_time, trident_spin_attack_strength)
    EffectComponentValue direct =
        component(
            "{'minecraft:trident_spin_attack_strength':{'type':'minecraft:add','value':1}}",
            "trident_spin_attack_strength");
    assertThat(direct)
        .isEqualTo(
            new EffectComponentValue.DirectValue(
                new ValueEffect.Add(new LevelBasedValue.Constant(1f))));
  }

  @Test
  void anIndicatorComponentIsAnEmptyObject() {
    // spec: EN-R12 (c: prevent_armor_change, prevent_equipment_drop)
    assertThat(component("{'minecraft:prevent_armor_change':{}}", "prevent_armor_change"))
        .isEqualTo(new EffectComponentValue.Flag());
  }

  @Test
  void aSoundListComponentReadsItsIdentifiers() {
    // spec: EN-R12 (c: trident_sound)
    assertThat(
            component(
                "{'minecraft:trident_sound':['minecraft:item.trident.thunder']}", "trident_sound"))
        .isEqualTo(
            new EffectComponentValue.Sounds(
                List.of(Identifier.parse("minecraft:item.trident.thunder"))));
  }

  @Test
  void anAttributesComponentReadsItsModifiers() {
    // spec: EN-R12 (c: attributes {id, attribute, operation, amount}), EN-R27
    EffectComponentValue attributes =
        component(
            "{'minecraft:attributes':[{'id':'minecraft:enchantment.efficiency',"
                + "'attribute':'minecraft:mining_efficiency','operation':'add_value','amount':2}]}",
            "attributes");
    assertThat(attributes)
        .isEqualTo(
            new EffectComponentValue.Attributes(
                List.of(
                    new AttributeEffect(
                        Identifier.parse("minecraft:enchantment.efficiency"),
                        Identifier.parse("minecraft:mining_efficiency"),
                        "add_value",
                        new LevelBasedValue.Constant(2f)))));
  }

  @Test
  void aChargingSoundsComponentIsKeptRaw() {
    // spec: EN-R12 (crossbow_charging_sounds: list of {start, end})
    EffectComponentValue sounds =
        component(
            "{'minecraft:crossbow_charging_sounds':[{'start':'minecraft:a.b','end':'minecraft:c.d'}]}",
            "crossbow_charging_sounds");
    assertThat(sounds).isInstanceOf(EffectComponentValue.Raw.class);
  }

  @Test
  void anEntityEffectEntryReadsEnchantedAffectedAndRequirements() {
    // spec: EN-R12 (a), EN-R14 (enchanted: attacker, affected: victim)
    EffectComponentValue postAttack =
        component(
            "{'minecraft:post_attack':[{'enchanted':'attacker','affected':'victim',"
                + "'effect':{'type':'minecraft:ignite','duration':4},"
                + "'requirements':{'type':'minecraft:killed_by_player'}}]}",
            "post_attack");
    assertThat(postAttack).isInstanceOf(EffectComponentValue.EntityEffects.class);
    ConditionalEntityEffect entry =
        ((EffectComponentValue.EntityEffects) postAttack).entries().getFirst();
    assertThat(entry.enchanted()).isEqualTo(EnchantedTarget.ATTACKER);
    assertThat(entry.affected()).isEqualTo(AffectedTarget.VICTIM);
    assertThat(entry.requirements()).hasSize(1);
    assertThat(entry.effect()).isInstanceOf(EntityEffect.Typed.class);
    assertThat(((EntityEffect.Typed) entry.effect()).type())
        .isEqualTo(Identifier.parse("minecraft:ignite"));
  }

  @Test
  void anEntityEffectEntryWithoutTargetsLeavesThemUnset() {
    // spec: EN-R14 (enchanted and affected optional)
    EffectComponentValue tick =
        component(
            "{'minecraft:tick':[{'effect':{'type':'minecraft:ignite','duration':1}}]}", "tick");
    ConditionalEntityEffect entry =
        ((EffectComponentValue.EntityEffects) tick).entries().getFirst();
    assertThat(entry.enchanted()).isEqualTo(EnchantedTarget.UNSET);
    assertThat(entry.affected()).isEqualTo(AffectedTarget.UNSET);
    assertThat(entry.requirements()).isEmpty();
  }

  @Test
  void anAllOfEntityEffectKeepsItsEffectsInOrder() {
    // spec: EN-R17, EN-R26 (all_of runs its sub-effects in series)
    EffectComponentValue tick =
        component(
            "{'minecraft:tick':[{'effect':{'type':'minecraft:all_of','effects':["
                + "{'type':'minecraft:ignite','duration':1},"
                + "{'type':'minecraft:apply_exhaustion','amount':1}]}}]}",
            "tick");
    EntityEffect effect = ((EffectComponentValue.EntityEffects) tick).entries().getFirst().effect();
    assertThat(effect).isInstanceOf(EntityEffect.AllOf.class);
    assertThat(((EntityEffect.AllOf) effect).effects()).hasSize(2);
  }

  @Test
  void theComponentsKeepTheirFileOrder() {
    // spec: EffectComponents contract (values in file order)
    EffectComponents decoded =
        components(
            "{'minecraft:prevent_armor_change':{},'minecraft:damage':[],'minecraft:tick':[]}");
    assertThat(decoded.components().keySet())
        .extracting(Identifier::path)
        .containsExactly("prevent_armor_change", "damage", "tick");
  }

  @Test
  void anAbsentComponentReadsAsEmpty() {
    // spec: EffectComponents#get contract
    assertThat(components("{'minecraft:damage':[]}").get(Identifier.minecraft("tick"))).isEmpty();
    assertThat(components("{}").components()).isEmpty();
  }

  @Test
  void anUnknownComponentIsRefused() {
    // spec: EN-R11 (31 registered components)
    Decoded.failsWith(decodeComponents("{'minecraft:no_such':[]}"), LootFindingCode.UNKNOWN_TYPE);
  }

  @Test
  void everyDecodingRespectsTheLootDecodingContract() {
    // spec: LootDecoding contract
    Decoded.assertWellFormed(decodeComponents("{'minecraft:damage':[]}"));
    Decoded.assertWellFormed(decodeComponents("{'minecraft:no_such':[]}"));
    Decoded.assertWellFormed(EffectCodec.levelBased(Json.parse("{'type':'minecraft:no_such'}")));
  }
}
