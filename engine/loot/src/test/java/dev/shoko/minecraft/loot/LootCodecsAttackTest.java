package dev.shoko.minecraft.loot;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.loot.enchant.EnchantmentCodec;
import dev.shoko.minecraft.loot.enchant.effect.EffectCodec;
import dev.shoko.minecraft.loot.modifier.ModifierCodec;
import dev.shoko.minecraft.loot.number.NumberProviderCodec;
import dev.shoko.minecraft.loot.predicate.ConditionCodec;
import dev.shoko.minecraft.loot.predicate.match.MatchCodecs;
import dev.shoko.minecraft.loot.table.LootTableCodec;
import dev.shoko.minecraft.loot.testing.Corpus;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.JsonMutator;
import dev.shoko.minecraft.loot.testing.LootItems;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: LootDecoding contract (a decoding is a typed result, success without ERROR or failure with
// one, never an exception), LT-R3, LT-R50, EN-R2 (adversarial): seeded mutation of valid documents
// of every codec, deeply nested documents, odd numbers and identifiers.
@Timeout(120)
class LootCodecsAttackTest {

  private static final int MUTANTS_PER_DOCUMENT = 80;
  private static final long SEED = 20261029L;

  private static void attack(List<String> corpus, Function<JsonNode, LootDecoding<?>> codec) {
    Random random = new Random(SEED);
    int decoded = 0;
    for (String relaxed : corpus) {
      JsonNode original = Json.parse(relaxed);
      assertWellFormedOrFail(codec, original, relaxed);
      for (int i = 0; i < MUTANTS_PER_DOCUMENT; i++) {
        String mutant = JsonMutator.mutate(original, random);
        Optional<JsonNode> node = Json.tryParse(mutant);
        if (node.isPresent()) {
          assertWellFormedOrFail(codec, node.get(), mutant);
          decoded++;
        }
      }
    }
    assertThat(decoded)
        .as("mutants that the JSON reader accepted")
        .isGreaterThan(corpus.size() * 10);
  }

  private static void assertWellFormedOrFail(
      Function<JsonNode, LootDecoding<?>> codec, JsonNode node, String text) {
    LootDecoding<?> decoding;
    try {
      decoding = codec.apply(node);
    } catch (UnsupportedOperationException notYet) {
      throw notYet;
    } catch (RuntimeException | StackOverflowError e) {
      throw new AssertionError("the codec threw on: " + text, e);
    }
    Decoded.assertWellFormed(decoding);
  }

  @Test
  void mutatedTablesNeverMakeTheCodecThrow() {
    // spec: LT-R3, LT-R5a to LT-R10
    attack(
        Corpus.tables(),
        node -> LootTableCodec.decode(node, EnchantFixtures.access(), LootItems.catalog()));
  }

  @Test
  void mutatedConditionsNeverMakeTheCodecThrow() {
    // spec: LT-R4a, LT-R29 to LT-R37
    attack(Corpus.conditions(), node -> ConditionCodec.decode(node, EnchantFixtures.access()));
    attack(Corpus.conditions(), node -> ConditionCodec.decodeList(node, EnchantFixtures.access()));
  }

  @Test
  void mutatedModifiersNeverMakeTheCodecThrow() {
    // spec: LT-R38 to LT-R47
    attack(
        Corpus.modifiers(),
        node -> ModifierCodec.decode(node, EnchantFixtures.access(), LootItems.catalog()));
    attack(
        Corpus.modifiers(),
        node -> ModifierCodec.decodeList(node, EnchantFixtures.access(), LootItems.catalog()));
  }

  @Test
  void mutatedNumberProvidersNeverMakeTheCodecThrow() {
    // spec: LT-R50 to LT-R53
    attack(
        Corpus.intProviders(),
        node -> NumberProviderCodec.decodeInt(node, EnchantFixtures.access()));
    attack(
        Corpus.floatProviders(),
        node -> NumberProviderCodec.decodeFloat(node, EnchantFixtures.access()));
  }

  @Test
  void mutatedEnchantmentsNeverMakeTheCodecThrow() {
    // spec: EN-R2, EN-R11 to EN-R20
    attack(
        Corpus.enchantments(),
        node ->
            EnchantmentCodec.decode(Identifier.minecraft("fuzz"), node, EnchantFixtures.access()));
    attack(
        Corpus.providers(),
        node -> EnchantmentCodec.decodeProvider(node, EnchantFixtures.access()));
  }

  @Test
  void mutatedEffectDataNeverMakesTheCodecThrow() {
    // spec: EN-R12, EN-R17, EN-R20
    attack(Corpus.levelBased(), EffectCodec::levelBased);
    attack(Corpus.levelBased(), node -> EffectCodec.valueEffect(node));
    attack(
        List.of(
            "{'minecraft:damage':[{'effect':{'type':'minecraft:add','value':1}}],"
                + "'minecraft:prevent_armor_change':{},"
                + "'minecraft:tick':[{'effect':{'type':'minecraft:ignite','duration':1}}]}"),
        node -> EffectCodec.components(node, EnchantFixtures.access()));
  }

  @Test
  void mutatedMatchersNeverMakeTheCodecThrow() {
    // spec: LT-R34, LT-R57a to LT-R60
    List<String> entities =
        List.of(
            "{'minecraft:entity_type':'#minecraft:undead','minecraft:flags':{'is_on_fire':true},"
                + "'minecraft:equipment':{'mainhand':{'items':'minecraft:shears'}},"
                + "'minecraft:type_specific/sheep':{'sheared':false},'minecraft:periodic_tick':20}");
    attack(entities, node -> MatchCodecs.entity(node, EnchantFixtures.access()));
    attack(
        List.of(
            "{'items':'#minecraft:test_gems','count':{'min':2},'predicates':{'minecraft:map_id':{}}}"),
        node -> MatchCodecs.item(node, EnchantFixtures.access()));
    attack(
        List.of("{'blocks':'minecraft:wheat','state':{'age':'7'}}"),
        node -> MatchCodecs.block(node, EnchantFixtures.access()));
    attack(
        List.of(
            "{'block':{'blocks':'minecraft:stone'},'biomes':'minecraft:plains','can_see_sky':true}"),
        node -> MatchCodecs.location(node, EnchantFixtures.access()));
    attack(
        List.of("{'tags':[{'id':'minecraft:is_projectile','expected':true}],'is_direct':false}"),
        node -> MatchCodecs.damageSource(node, EnchantFixtures.access()));
    attack(List.of("{'min':1,'max':4}"), MatchCodecs::range);
  }

  private static String nestedAlternatives(int depth) {
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < depth; i++) {
      text.append("{'type':'minecraft:alternatives','children':[");
    }
    text.append("{'type':'minecraft:item','name':'minecraft:stone'}");
    for (int i = 0; i < depth; i++) {
      text.append("]}");
    }
    return "{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[" + text + "]}]}";
  }

  @Test
  void aDeeplyNestedTableIsDecodedOrRefusedWithoutOverflowingTheStack() {
    // spec: LT-R21, LT-R10 (composites nest), LootDecoding contract
    for (int depth : new int[] {10, 40, 90}) {
      JsonNode node = Json.parse(nestedAlternatives(depth));
      LootDecoding<?> decoding =
          LootTableCodec.decode(node, EnchantFixtures.access(), LootItems.catalog());
      Decoded.assertWellFormed(decoding);
    }
  }

  @Test
  void aDeeplyNestedConditionIsDecodedOrRefusedWithoutOverflowingTheStack() {
    // spec: LT-R29 (inverted and all_of nest), LootDecoding contract
    for (int depth : new int[] {10, 50, 100}) {
      StringBuilder text = new StringBuilder();
      for (int i = 0; i < depth; i++) {
        text.append("{'type':'minecraft:inverted','term':");
      }
      text.append("{'type':'minecraft:killed_by_player'}");
      text.append("}".repeat(depth));
      Decoded.assertWellFormed(
          ConditionCodec.decode(Json.parse(text.toString()), EnchantFixtures.access()));
    }
  }

  @Test
  void numbersAtTheLimitsAreRefusedOrAcceptedCleanly() {
    // spec: LT-R6 (rolls), LT-R39 (set_count), EN-R10 (levels up to 255)
    List<String> numbers =
        List.of(
            "2147483647",
            "-2147483648",
            "2147483648",
            "1e308",
            "-0.0",
            "4.9e-324",
            "99999999999999999999");
    for (String number : numbers) {
      Decoded.assertWellFormed(
          NumberProviderCodec.decodeInt(Json.parse(number), EnchantFixtures.access()));
      Decoded.assertWellFormed(
          NumberProviderCodec.decodeFloat(Json.parse(number), EnchantFixtures.access()));
      Decoded.assertWellFormed(
          LootTableCodec.decode(
              Json.parse(
                  "{'type':'minecraft:chest','pools':[{'rolls':" + number + ",'entries':[]}]}"),
              EnchantFixtures.access(),
              LootItems.catalog()));
    }
  }
}
