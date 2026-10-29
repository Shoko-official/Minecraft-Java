package dev.shoko.minecraft.loot.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.enchant.effect.LevelBasedValue;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.port.EntityFlag;
import dev.shoko.minecraft.loot.predicate.match.DamageSourcePredicate;
import dev.shoko.minecraft.loot.predicate.match.EntityPredicate;
import dev.shoko.minecraft.loot.predicate.match.Tristate;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.Sets;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: LT-R3 (type is the discriminant), LT-R4a/LT-R4b (inline, reference, list), LT-R29 to LT-R37
class ConditionCodecTest {

  private static LootDecoding<LootCondition> decode(String json) {
    return ConditionCodec.decode(Json.parse(json), EnchantFixtures.access());
  }

  private static LootCondition ok(String json) {
    return Decoded.value(decode(json));
  }

  @Test
  void randomChanceReadsAConstantChance() {
    // spec: LT-R30
    assertThat(ok("{'type':'minecraft:random_chance','chance':0.5}"))
        .isEqualTo(new LootCondition.RandomChance(new FloatProvider.Constant(0.5f)));
  }

  @Test
  void randomChanceReadsAnEnchantmentLevelProvider() {
    // spec: LT-R30 ({"type":"minecraft:enchantment_level","amount":...})
    LootCondition condition =
        ok(
            "{'type':'minecraft:random_chance','chance':{'type':'minecraft:enchantment_level',"
                + "'amount':{'type':'minecraft:linear','base':0.15,'per_level_above_first':0.15}}}");
    assertThat(condition)
        .isEqualTo(
            new LootCondition.RandomChance(
                new FloatProvider.EnchantmentLevel(new LevelBasedValue.Linear(0.15f, 0.15f))));
  }

  @Test
  void randomChanceWithEnchantedBonusReadsItsThreeFields() {
    // spec: LT-R31
    LootCondition condition =
        ok(
            "{'type':'minecraft:random_chance_with_enchanted_bonus','unenchanted_chance':0.0625,"
                + "'enchanted_chance':{'type':'minecraft:linear','base':0.13,"
                + "'per_level_above_first':0.02},'enchantment':'minecraft:looting'}");
    assertThat(condition)
        .isEqualTo(
            new LootCondition.RandomChanceWithEnchantedBonus(
                Identifier.minecraft("looting"),
                0.0625f,
                new LevelBasedValue.Linear(0.13f, 0.02f)));
  }

  @Test
  void tableBonusReadsTheEnchantmentAndTheChances() {
    // spec: LT-R32 (the list seen on oak leaves saplings)
    LootCondition condition =
        ok(
            "{'type':'minecraft:table_bonus','enchantment':'minecraft:fortune',"
                + "'chances':[0.05,0.0625,0.083333336,0.1]}");
    assertThat(condition)
        .isEqualTo(
            new LootCondition.TableBonus(
                Identifier.minecraft("fortune"), List.of(0.05f, 0.0625f, 0.083333336f, 0.1f)));
  }

  @Test
  void conditionsWithoutFieldsDecode() {
    // spec: LT-R35, LT-R33 (killed_by_player, survives_explosion)
    assertThat(ok("{'type':'minecraft:killed_by_player'}"))
        .isEqualTo(new LootCondition.KilledByPlayer());
    assertThat(ok("{'type':'minecraft:survives_explosion'}"))
        .isEqualTo(new LootCondition.SurvivesExplosion());
  }

  @Test
  void invertedReadsItsTerm() {
    // spec: LT-R29 (inverted: term)
    assertThat(ok("{'type':'minecraft:inverted','term':{'type':'minecraft:killed_by_player'}}"))
        .isEqualTo(new LootCondition.Inverted(new LootCondition.KilledByPlayer()));
  }

  @Test
  void allOfAndAnyOfAcceptObjectsAndReferencesInTerms() {
    // spec: LT-R4a (terms accepts objects and references), LT-R29
    LootCondition all =
        ok(
            "{'type':'minecraft:all_of','terms':[{'type':'minecraft:survives_explosion'},"
                + "'minecraft:tool/can_silk_touch']}");
    assertThat(all)
        .isEqualTo(
            new LootCondition.AllOf(
                List.of(
                    new LootCondition.SurvivesExplosion(),
                    new LootCondition.Reference(
                        Identifier.parse("minecraft:tool/can_silk_touch")))));
    LootCondition any = ok("{'type':'minecraft:any_of','terms':[]}");
    assertThat(any).isEqualTo(new LootCondition.AnyOf(List.of()));
  }

  @Test
  void provisionalAListNestedInTermsBehavesAsAllOf() {
    // spec: LT-R29 (a list nested in terms is an all_of)
    LootCondition any =
        ok(
            "{'type':'minecraft:any_of','terms':[[{'type':'minecraft:killed_by_player'},"
                + "{'type':'minecraft:survives_explosion'}]]}");
    assertThat(any).isInstanceOf(LootCondition.AnyOf.class);
    assertThat(((LootCondition.AnyOf) any).terms().getFirst())
        .isEqualTo(
            new LootCondition.AllOf(
                List.of(
                    new LootCondition.KilledByPlayer(), new LootCondition.SurvivesExplosion())));
  }

  @Test
  void aStringIsAReferenceToANamedPredicate() {
    // spec: LT-R4a (137 references in the tables)
    assertThat(ok("'minecraft:tool/can_shear'"))
        .isEqualTo(new LootCondition.Reference(Identifier.parse("minecraft:tool/can_shear")));
  }

  @Test
  void weatherCheckReadsThundering() {
    // spec: LT-R36 ({"thundering": true})
    assertThat(ok("{'type':'minecraft:weather_check','thundering':true}"))
        .isEqualTo(new LootCondition.WeatherCheck(Tristate.UNSET, Tristate.TRUE));
    assertThat(ok("{'type':'minecraft:weather_check','raining':false}"))
        .isEqualTo(new LootCondition.WeatherCheck(Tristate.FALSE, Tristate.UNSET));
  }

  @Test
  void matchBlockReadsTheBlocksAndTheState() {
    // spec: LT-R34 ({"blocks":"minecraft:wheat","state":{"age":"7"}})
    LootCondition condition =
        ok("{'type':'minecraft:match_block','blocks':'minecraft:wheat','state':{'age':'7'}}");
    assertThat(condition).isInstanceOf(LootCondition.MatchBlock.class);
    var match = ((LootCondition.MatchBlock) condition).match();
    assertThat(match.blocks().ids()).containsExactly(Identifier.minecraft("wheat"));
    assertThat(match.state()).containsEntry("age", "7").hasSize(1);
  }

  @Test
  void matchToolReadsAnItemPredicate() {
    // spec: LT-R35, LT-R58 (items: shears)
    LootCondition condition =
        ok("{'type':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}}");
    assertThat(condition).isInstanceOf(LootCondition.MatchTool.class);
    assertThat(((LootCondition.MatchTool) condition).predicate().items().ids())
        .containsExactly(Identifier.minecraft("shears"));
  }

  @Test
  void locationCheckReadsItsOffset() {
    // spec: LT-R35 (offsetY seen in the data)
    LootCondition condition =
        ok("{'type':'minecraft:location_check','offsetY':-1,'predicate':{'can_see_sky':true}}");
    assertThat(condition).isInstanceOf(LootCondition.LocationCheck.class);
    LootCondition.LocationCheck check = (LootCondition.LocationCheck) condition;
    assertThat(check.offsetY()).isEqualTo(-1);
    assertThat(check.offsetX()).isZero();
    assertThat(check.offsetZ()).isZero();
  }

  @Test
  void entityPropertiesReadsTheTargetAndTheFlags() {
    // spec: LT-R35, LT-R57a (flags: is_on_fire)
    LootCondition condition =
        ok(
            "{'type':'minecraft:entity_properties','entity':'direct_attacker',"
                + "'predicate':{'minecraft:flags':{'is_on_fire':true}}}");
    assertThat(condition).isInstanceOf(LootCondition.EntityProperties.class);
    LootCondition.EntityProperties properties = (LootCondition.EntityProperties) condition;
    assertThat(properties.entity()).isEqualTo(EntityTarget.DIRECT_ATTACKER);
    assertThat(properties.predicate().checks()).hasSize(1);
    EntityPredicate.Check check = properties.predicate().checks().getFirst();
    assertThat(check).isInstanceOf(EntityPredicate.Check.Flags.class);
    assertThat(((EntityPredicate.Check.Flags) check).flags())
        .containsEntry(EntityFlag.ON_FIRE, true);
  }

  @Test
  void entityPropertiesAcceptsThisAndAttacker() {
    // spec: LT-R12
    assertThat(
            ((LootCondition.EntityProperties)
                    ok("{'type':'minecraft:entity_properties','entity':'this','predicate':{}}"))
                .entity())
        .isEqualTo(EntityTarget.THIS);
    assertThat(
            ((LootCondition.EntityProperties)
                    ok("{'type':'minecraft:entity_properties','entity':'attacker','predicate':{}}"))
                .entity())
        .isEqualTo(EntityTarget.ATTACKER);
  }

  @Test
  void damageSourcePropertiesReadsTheTags() {
    // spec: LT-R60 (tags: id and expected)
    LootCondition condition =
        ok(
            "{'type':'minecraft:damage_source_properties','predicate':{'tags':"
                + "[{'id':'minecraft:is_projectile','expected':true}]}}");
    assertThat(condition)
        .isEqualTo(
            new LootCondition.DamageSourceProperties(
                new DamageSourcePredicate(
                    List.of(
                        new DamageSourcePredicate.Requirement.Tags(
                            List.of(
                                new DamageSourcePredicate.TagExpectation(
                                    Identifier.minecraft("is_projectile"), true)))))));
  }

  @Test
  void enchantmentActiveCheckReadsActive() {
    // spec: LT-R37 (enchantment_active_check: active)
    assertThat(ok("{'type':'minecraft:enchantment_active_check','active':true}"))
        .isEqualTo(new LootCondition.EnchantmentActiveCheck(true));
  }

  @Test
  void provisionalIntValueCheckReadsAValueAndATest() {
    // spec: LT-R37 (value: provider, test: interval)
    assertThat(ok("{'type':'minecraft:int_value_check','value':5,'test':{'min':3,'max':7}}"))
        .isEqualTo(
            new LootCondition.IntValueCheck(new IntProvider.Constant(5), Sets.between(3, 7)));
  }

  @Test
  void anUnknownTypeIsRefused() {
    // spec: LT-R3, LT-R28 table of types (20 registered)
    Decoded.failsWith(
        decode("{'type':'minecraft:no_such_condition'}"), LootFindingCode.UNKNOWN_TYPE);
  }

  @Test
  void theFormOfEarlierVersionsIsNotDecoded() {
    // spec: LT-R3 ({"condition": ...} as discriminant is out of scope)
    Decoded.fails(decode("{'condition':'minecraft:killed_by_player'}"));
  }

  @Test
  void aMissingRequiredFieldIsReported() {
    // spec: LT-R29 (inverted needs term)
    Decoded.failsWith(decode("{'type':'minecraft:inverted'}"), LootFindingCode.MISSING_FIELD);
  }

  @Test
  void aWrongKindOfValueIsATypeMismatch() {
    // spec: ConditionCodec#decode contract (an object or a string)
    Decoded.failsWith(decode("5"), LootFindingCode.TYPE_MISMATCH);
    Decoded.failsWith(decode("true"), LootFindingCode.TYPE_MISMATCH);
  }

  @Test
  void aTagThatIsNotLoadedIsReported() {
    // spec: MatchCodecs contract (UNRESOLVED_TAG)
    Decoded.failsWith(
        decode("{'type':'minecraft:match_tool','predicate':{'items':'#minecraft:ghost'}}"),
        LootFindingCode.UNRESOLVED_TAG);
  }

  @Test
  void decodeListReadsAnObjectAsAListOfOne() {
    // spec: LT-R4a (condition is an object or a string, never a list in the data)
    List<LootCondition> one =
        Decoded.value(
            ConditionCodec.decodeList(
                Json.parse("{'type':'minecraft:killed_by_player'}"), EnchantFixtures.access()));
    assertThat(one).containsExactly(new LootCondition.KilledByPlayer());
    List<LootCondition> ref =
        Decoded.value(
            ConditionCodec.decodeList(
                Json.parse("'minecraft:tool/can_shear'"), EnchantFixtures.access()));
    assertThat(ref)
        .containsExactly(new LootCondition.Reference(Identifier.parse("minecraft:tool/can_shear")));
  }

  @Test
  void provisionalDecodeListKeepsAListInOrder() {
    // spec: LT-R4b (a list of conditions is kept in order)
    List<LootCondition> list =
        Decoded.value(
            ConditionCodec.decodeList(
                Json.parse(
                    "[{'type':'minecraft:survives_explosion'},{'type':'minecraft:killed_by_player'}]"),
                EnchantFixtures.access()));
    assertThat(list)
        .containsExactly(new LootCondition.SurvivesExplosion(), new LootCondition.KilledByPlayer());
    assertThat(Decoded.value(ConditionCodec.decodeList(Json.parse("[]"), EnchantFixtures.access())))
        .isEmpty();
  }

  @Test
  void everyDecodingRespectsTheLootDecodingContract() {
    // spec: LootDecoding contract (success without ERROR, failure with ERROR, paths rooted at $)
    Decoded.assertWellFormed(decode("{'type':'minecraft:killed_by_player'}"));
    Decoded.assertWellFormed(decode("{'type':'minecraft:inverted'}"));
    Decoded.assertWellFormed(decode("{'type':'minecraft:no_such_condition'}"));
  }
}
