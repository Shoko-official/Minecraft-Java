package dev.shoko.minecraft.loot.table;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.loot.modifier.ModifierAction;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.random.SequenceRef;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: V-L1 to V-L4 and V-L8 (structure of decoded tables, rebuilt from the description of the
// spec, not copied from the reference data) and the round trip of plan L1 (decode, encode, same
// tree)
class LootVectorsTest {

  private static final String STONE =
      "{'type':'minecraft:block','random_sequence':'minecraft:blocks/stone','pools':[{'rolls':1,"
          + "'entries':[{'type':'minecraft:alternatives','children':["
          + "{'type':'minecraft:item','name':'minecraft:stone','condition':'minecraft:tool/can_silk_touch'},"
          + "{'type':'minecraft:item','name':'minecraft:cobblestone','condition':"
          + "{'type':'minecraft:survives_explosion'}}]}]}]}";

  private static final String OAK_LEAVES =
      "{'type':'minecraft:block','random_sequence':'minecraft:blocks/oak_leaves','pools':["
          + "{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':["
          + "{'type':'minecraft:item','name':'minecraft:oak_leaves','condition':{'type':'minecraft:any_of',"
          + "'terms':['minecraft:tool/can_shear','minecraft:tool/can_silk_touch']}},"
          + "{'type':'minecraft:item','name':'minecraft:oak_sapling','condition':"
          + "{'type':'minecraft:all_of','terms':[{'type':'minecraft:survives_explosion'},"
          + "{'type':'minecraft:table_bonus','enchantment':'minecraft:fortune',"
          + "'chances':[0.05,0.0625,0.083333336,0.1]}]}}]}]},"
          + "{'rolls':1,'condition':{'type':'minecraft:inverted','term':{'type':'minecraft:any_of',"
          + "'terms':['minecraft:tool/can_shear','minecraft:tool/can_silk_touch']}},"
          + "'entries':[{'type':'minecraft:item','name':'minecraft:stick','condition':"
          + "{'type':'minecraft:table_bonus','enchantment':'minecraft:fortune',"
          + "'chances':[0.02,0.022222223,0.025,0.033333335,0.1]},'modifier':["
          + "{'type':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':2}},"
          + "{'type':'minecraft:explosion_decay'}]}]},"
          + "{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:apple'}]}]}";

  private static final String COW =
      "{'type':'minecraft:entity','random_sequence':'minecraft:entities/cow','pools':["
          + "{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:leather','modifier':["
          + "{'type':'minecraft:set_count','count':{'type':'minecraft:uniform','min':0,'max':2}},"
          + "{'type':'minecraft:enchanted_count_increase','enchantment':'minecraft:looting',"
          + "'count':{'type':'minecraft:uniform','min':0.0,'max':1.0}}]}]},"
          + "{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:beef','modifier':["
          + "{'type':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':3}},"
          + "{'type':'minecraft:furnace_smelt','condition':{'type':'minecraft:any_of','terms':["
          + "{'type':'minecraft:entity_properties','entity':'this','predicate':"
          + "{'minecraft:flags':{'is_on_fire':true}}},"
          + "{'type':'minecraft:entity_properties','entity':'direct_attacker','predicate':"
          + "{'minecraft:equipment':{'mainhand':{'predicates':{'minecraft:enchantments':"
          + "[{'enchantments':'#minecraft:smelts_loot'}]}}}}}]}},"
          + "{'type':'minecraft:enchanted_count_increase','enchantment':'minecraft:looting',"
          + "'count':{'type':'minecraft:uniform','min':0.0,'max':1.0}}]}]}]}";

  private static final String FISHING =
      "{'type':'minecraft:fishing','random_sequence':'minecraft:gameplay/fishing','pools':[{'rolls':1,"
          + "'entries':[{'type':'minecraft:loot_table','value':'minecraft:gameplay/fishing/junk',"
          + "'weight':10,'quality':-2},"
          + "{'type':'minecraft:loot_table','value':'minecraft:gameplay/fishing/treasure',"
          + "'weight':5,'quality':2,'condition':{'type':'minecraft:entity_properties','entity':'this',"
          + "'predicate':{'minecraft:type_specific/fishing_hook':{'in_open_water':true}}}},"
          + "{'type':'minecraft:loot_table','value':'minecraft:gameplay/fishing/fish',"
          + "'weight':85,'quality':-1}]}]}";

  private static final String POT =
      "{'type':'minecraft:block','random_sequence':'minecraft:blocks/decorated_pot','pools':[{'rolls':1,"
          + "'entries':[{'type':'minecraft:alternatives','children':["
          + "{'type':'minecraft:dynamic','name':'minecraft:sherds','condition':"
          + "{'type':'minecraft:match_block','blocks':'minecraft:decorated_pot','state':"
          + "{'cracked':'true'}}},"
          + "{'type':'minecraft:item','name':'minecraft:decorated_pot','modifier':"
          + "{'type':'minecraft:copy_components','source':'block_entity',"
          + "'include':['minecraft:pot_decorations']}}]}]}]}";

  private static LootTable decode(String json) {
    return Decoded.value(
        LootTableCodec.decode(Json.parse(json), EnchantFixtures.access(), LootItems.catalog()));
  }

  private static LootEntry.Kind.Alternatives firstAlternatives(LootTable table) {
    return (LootEntry.Kind.Alternatives) table.pools().getFirst().entries().getFirst().kind();
  }

  private static void assertRoundTrip(String relaxedJson) {
    JsonNode original = Json.parse(relaxedJson);
    LootTable table = decode(relaxedJson);
    JsonNode again = Json.parseStrict(LootTableCodec.encode(table));
    assertThat(again.sameValueAs(original)).as(LootTableCodec.encode(table)).isTrue();
  }

  @Test
  void stoneHasAnAlternativeOfSilkTouchThenCobblestone() {
    // spec: V-L1
    LootTable table = decode(STONE);
    assertThat(table.type().id()).isEqualTo(Identifier.minecraft("block"));
    assertThat(table.randomSequence())
        .isEqualTo(new SequenceRef.Named(Identifier.parse("minecraft:blocks/stone")));
    assertThat(table.pools()).hasSize(1);
    List<LootEntry> children = firstAlternatives(table).children();
    assertThat(children).hasSize(2);
    assertThat(children.get(0).kind()).isEqualTo(new LootEntry.Kind.Item(LootItems.type("stone")));
    assertThat(children.get(0).conditions())
        .containsExactly(
            new LootCondition.Reference(Identifier.parse("minecraft:tool/can_silk_touch")));
    assertThat(children.get(1).kind())
        .isEqualTo(new LootEntry.Kind.Item(LootItems.type("cobblestone")));
    assertThat(children.get(1).conditions()).containsExactly(new LootCondition.SurvivesExplosion());
  }

  @Test
  void oakLeavesHasThreePoolsWithTheListedShapes() {
    // spec: V-L2
    LootTable table = decode(OAK_LEAVES);
    assertThat(table.pools()).hasSize(3);
    LootEntry.Kind.Alternatives alternatives = firstAlternatives(table);
    assertThat(alternatives.children().get(0).conditions().getFirst())
        .isInstanceOf(LootCondition.AnyOf.class);
    assertThat(alternatives.children().get(1).conditions().getFirst())
        .isInstanceOf(LootCondition.AllOf.class);
    LootPool sticks = table.pools().get(1);
    assertThat(sticks.conditions().getFirst()).isInstanceOf(LootCondition.Inverted.class);
    LootEntry stick = sticks.entries().getFirst();
    assertThat(stick.modifiers()).hasSize(2);
    assertThat(stick.modifiers().get(0).action()).isInstanceOf(ModifierAction.SetCount.class);
    assertThat(stick.modifiers().get(1).action()).isEqualTo(new ModifierAction.ExplosionDecay());
    assertThat(table.pools().get(2).entries().getFirst().kind())
        .isEqualTo(new LootEntry.Kind.Item(LootItems.type("apple")));
  }

  @Test
  void cowHasTwoPoolsWithLootingAndASmeltCondition() {
    // spec: V-L3
    LootTable table = decode(COW);
    assertThat(table.type().id()).isEqualTo(Identifier.minecraft("entity"));
    assertThat(table.pools()).hasSize(2);
    LootEntry leather = table.pools().get(0).entries().getFirst();
    assertThat(leather.modifiers().get(1).action())
        .isInstanceOf(ModifierAction.EnchantedCountIncrease.class);
    LootEntry beef = table.pools().get(1).entries().getFirst();
    assertThat(beef.modifiers()).hasSize(3);
    assertThat(beef.modifiers().get(1).action()).isEqualTo(new ModifierAction.FurnaceSmelt());
    LootCondition smelt = beef.modifiers().get(1).conditions().getFirst();
    assertThat(smelt).isInstanceOf(LootCondition.AnyOf.class);
    assertThat(((LootCondition.AnyOf) smelt).terms()).hasSize(2);
  }

  @Test
  void fishingHasThreeLootTableEntriesWithWeightsAndQualities() {
    // spec: V-L4, LT-R27
    LootTable table = decode(FISHING);
    assertThat(table.type().id()).isEqualTo(Identifier.minecraft("fishing"));
    List<LootEntry> entries = table.pools().getFirst().entries();
    assertThat(entries).hasSize(3);
    assertThat(entries.stream().map(LootEntry::weight).toList()).containsExactly(10, 5, 85);
    assertThat(entries.stream().map(LootEntry::quality).toList()).containsExactly(-2, 2, -1);
    assertThat(entries.get(0).conditions()).isEmpty();
    assertThat(entries.get(1).conditions()).hasSize(1);
    assertThat(entries.get(2).conditions()).isEmpty();
  }

  @Test
  void aDecoratedPotDropsSherdsWhenCrackedAndTheCopiedPotOtherwise() {
    // spec: V-L8
    LootTable table = decode(POT);
    List<LootEntry> children = firstAlternatives(table).children();
    assertThat(children.get(0).kind())
        .isEqualTo(new LootEntry.Kind.Dynamic(Identifier.parse("minecraft:sherds")));
    assertThat(children.get(0).conditions().getFirst())
        .isInstanceOf(LootCondition.MatchBlock.class);
    assertThat(children.get(1).kind())
        .isEqualTo(new LootEntry.Kind.Item(LootItems.type("decorated_pot")));
    assertThat(children.get(1).modifiers().getFirst().action())
        .isInstanceOf(ModifierAction.CopyComponents.class);
  }

  @Test
  void stoneEncodesBackToTheSameTree() {
    // spec: plan of tests L1 (re-encode and compare, key order aside)
    assertRoundTrip(STONE);
  }

  @Test
  void oakLeavesEncodesBackToTheSameTree() {
    // spec: plan of tests L1
    assertRoundTrip(OAK_LEAVES);
  }

  @Test
  void cowEncodesBackToTheSameTree() {
    // spec: plan of tests L1
    assertRoundTrip(COW);
  }

  @Test
  void fishingEncodesBackToTheSameTree() {
    // spec: plan of tests L1
    assertRoundTrip(FISHING);
  }

  @Test
  void aDecoratedPotEncodesBackToTheSameTree() {
    // spec: plan of tests L1
    assertRoundTrip(POT);
  }

  @Test
  void anInlineNestedTableEncodesBackToTheSameTree() {
    // spec: LT-R10 (a nested table keeps its own pools), plan of tests L1
    assertRoundTrip(
        "{'type':'minecraft:chest','pools':[{'rolls':{'type':'minecraft:uniform','min':1,'max':3},"
            + "'entries':[{'type':'minecraft:loot_table','weight':3,'value':{'type':'minecraft:chest',"
            + "'pools':[{'rolls':2,'entries':[{'type':'minecraft:item','name':'minecraft:stone'},"
            + "{'type':'minecraft:empty','weight':4}]}]}}]}]}");
  }

  @Test
  void aTableWithoutPoolsEncodesBackToTheSameTree() {
    // spec: LT-R55, plan of tests L1
    assertRoundTrip("{'type':'minecraft:chest','random_sequence':'minecraft:chests/none'}");
  }

  @Test
  void aTableCarriesNoDefaultThatWasAbsent() {
    // spec: LootTableCodec#encode contract (defaults that were absent are not written)
    String json = LootTableCodec.encode(decode(STONE));
    assertThat(json)
        .doesNotContain("weight")
        .doesNotContain("quality")
        .doesNotContain("bonus_rolls");
  }
}
