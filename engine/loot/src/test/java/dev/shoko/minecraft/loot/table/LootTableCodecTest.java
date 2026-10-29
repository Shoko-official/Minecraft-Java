package dev.shoko.minecraft.loot.table;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.LootFindingCode;
import dev.shoko.minecraft.loot.modifier.ModifierAction;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.random.SequenceRef;
import dev.shoko.minecraft.loot.testing.Decoded;
import dev.shoko.minecraft.loot.testing.EnchantFixtures;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import org.junit.jupiter.api.Test;

// spec: LT-R1 to LT-R10 (format of tables, pools and entries), LT-R3 (26.3 format only)
class LootTableCodecTest {

  private static LootDecoding<LootTable> decode(String json) {
    return LootTableCodec.decode(Json.parse(json), EnchantFixtures.access(), LootItems.catalog());
  }

  private static LootTable ok(String json) {
    return Decoded.value(decode(json));
  }

  private static String onePool(String entry) {
    return "{'type':'minecraft:chest','pools':[{'rolls':1,'entries':[" + entry + "]}]}";
  }

  private static LootEntry firstEntry(String entry) {
    return ok(onePool(entry)).pools().getFirst().entries().getFirst();
  }

  @Test
  void aTableReadsItsTypeItsSequenceAndItsPools() {
    // spec: LT-R2, LT-R64, LT-R5a
    LootTable table =
        ok(
            "{'type':'minecraft:block','random_sequence':'minecraft:blocks/stone',"
                + "'pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:stone'}]}]}");
    assertThat(table.type().id()).isEqualTo(Identifier.minecraft("block"));
    assertThat(table.randomSequence())
        .isEqualTo(new SequenceRef.Named(Identifier.parse("minecraft:blocks/stone")));
    assertThat(table.pools()).hasSize(1);
    assertThat(table.modifiers()).isEmpty();
  }

  @Test
  void provisionalATableWithoutTypeReadsAsGeneric() {
    // spec: LT-R2 (type absent vaut generic)
    LootTable table = ok("{'pools':[]}");
    assertThat(table.type().id()).isEqualTo(Identifier.minecraft("generic"));
  }

  @Test
  void aTableWithoutSequenceIsUnnamed() {
    // spec: LT-R64 (a datapack table may omit random_sequence)
    assertThat(ok("{'type':'minecraft:chest','pools':[]}").randomSequence())
        .isEqualTo(new SequenceRef.Unnamed());
  }

  @Test
  void aTableWithoutPoolsHasNoPool() {
    // spec: LT-R2, LT-R55 (44 vanilla tables without pools)
    assertThat(ok("{'type':'minecraft:block'}").pools()).isEmpty();
    assertThat(ok("{'type':'minecraft:block','pools':[]}").pools()).isEmpty();
  }

  @Test
  void aTableTypeOutsideTheThirtyOneContextSetsIsRefused() {
    // spec: LT-R13 (UNKNOWN_TYPE)
    Decoded.failsWith(
        decode("{'type':'minecraft:no_such_set','pools':[]}"), LootFindingCode.UNKNOWN_TYPE);
  }

  @Test
  void aTableModifierAcceptsAnObjectOrAList() {
    // spec: LT-R4a (modifier: an object, or a list of objects)
    LootTable single =
        ok("{'type':'minecraft:chest','modifier':{'type':'minecraft:discard'},'pools':[]}");
    assertThat(single.modifiers()).hasSize(1);
    LootTable list =
        ok(
            "{'type':'minecraft:chest','modifier':[{'type':'minecraft:discard'},"
                + "{'type':'minecraft:explosion_decay'}],'pools':[]}");
    assertThat(list.modifiers()).hasSize(2);
    assertThat(list.modifiers().get(1).action()).isEqualTo(new ModifierAction.ExplosionDecay());
  }

  @Test
  void aPoolReadsRollsConditionsAndModifiers() {
    // spec: LT-R5a, LT-R6, LT-R4a
    LootTable table =
        ok(
            "{'type':'minecraft:chest','pools':[{'rolls':{'type':'minecraft:uniform','min':1,'max':3},"
                + "'condition':{'type':'minecraft:killed_by_player'},"
                + "'modifier':{'type':'minecraft:set_count','count':2},"
                + "'entries':[{'type':'minecraft:empty'}]}]}");
    LootPool pool = table.pools().getFirst();
    assertThat(pool.rolls())
        .isEqualTo(
            new IntProvider.Uniform(new IntProvider.Constant(1), new IntProvider.Constant(3)));
    assertThat(pool.conditions()).containsExactly(new LootCondition.KilledByPlayer());
    assertThat(pool.modifiers()).hasSize(1);
  }

  @Test
  void aPoolConditionMayBeANamedPredicate() {
    // spec: LT-R4a (137 references)
    LootTable table =
        ok(
            "{'type':'minecraft:chest','pools':[{'rolls':1,'condition':'minecraft:tool/can_shear',"
                + "'entries':[]}]}");
    assertThat(table.pools().getFirst().conditions())
        .containsExactly(new LootCondition.Reference(Identifier.parse("minecraft:tool/can_shear")));
  }

  @Test
  void aPoolWithoutConditionHasNone() {
    // spec: LT-R19a (condition optional), LT-R5a
    LootPool pool = ok(onePool("{'type':'minecraft:empty'}")).pools().getFirst();
    assertThat(pool.conditions()).isEmpty();
    assertThat(pool.modifiers()).isEmpty();
  }

  @Test
  void provisionalBonusRollsDefaultsToZero() {
    // spec: LT-R5b (absent from every vanilla table; default 0)
    LootPool pool = ok(onePool("{'type':'minecraft:empty'}")).pools().getFirst();
    assertThat(pool.bonusRolls()).isEqualTo(new FloatProvider.Constant(0f));
  }

  @Test
  void provisionalBonusRollsReadsAProvider() {
    // spec: LT-R5b
    LootTable table =
        ok("{'type':'minecraft:chest','pools':[{'rolls':1,'bonus_rolls':1.5,'entries':[]}]}");
    assertThat(table.pools().getFirst().bonusRolls()).isEqualTo(new FloatProvider.Constant(1.5f));
  }

  @Test
  void anItemEntryReadsItsNameWeightAndQuality() {
    // spec: LT-R9, LT-R8
    LootEntry entry =
        firstEntry("{'type':'minecraft:item','name':'minecraft:diamond','weight':5,'quality':-2}");
    assertThat(entry.kind()).isEqualTo(new LootEntry.Kind.Item(LootItems.type("diamond")));
    assertThat(entry.weight()).isEqualTo(5);
    assertThat(entry.quality()).isEqualTo(-2);
  }

  @Test
  void provisionalAnEntryWithoutWeightHasWeightOneAndQualityZero() {
    // spec: LT-R8 (defauts: weight 1, quality 0)
    LootEntry entry = firstEntry("{'type':'minecraft:item','name':'minecraft:diamond'}");
    assertThat(entry.weight()).isEqualTo(1);
    assertThat(entry.quality()).isZero();
    assertThat(entry.conditions()).isEmpty();
    assertThat(entry.modifiers()).isEmpty();
  }

  @Test
  void anEntryReadsItsConditionAndItsModifiers() {
    // spec: LT-R8 (condition, modifier on an entry), LT-R4a
    LootEntry entry =
        firstEntry(
            "{'type':'minecraft:item','name':'minecraft:stick','condition':'minecraft:tool/can_shear',"
                + "'modifier':[{'type':'minecraft:set_count','count':2},{'type':'minecraft:explosion_decay'}]}");
    assertThat(entry.conditions()).hasSize(1);
    assertThat(entry.modifiers()).hasSize(2);
  }

  @Test
  void anEmptyEntryHasOnlyAWeight() {
    // spec: LT-R9 (empty: no field), LT-R24
    LootEntry entry = firstEntry("{'type':'minecraft:empty','weight':35}");
    assertThat(entry.kind()).isEqualTo(new LootEntry.Kind.Empty());
    assertThat(entry.weight()).isEqualTo(35);
  }

  @Test
  void aLootTableEntryReadsAnIdentifierOrAnInlineTable() {
    // spec: LT-R9, LT-R10 (value: identifier or table written in place)
    LootEntry named =
        firstEntry("{'type':'minecraft:loot_table','value':'minecraft:gameplay/fishing/fish'}");
    assertThat(named.kind())
        .isEqualTo(
            new LootEntry.Kind.LootTableRef(
                new LootEntry.TableRef.Named(Identifier.parse("minecraft:gameplay/fishing/fish"))));
    LootEntry inline =
        firstEntry(
            "{'type':'minecraft:loot_table','value':{'type':'minecraft:chest','pools':[{'rolls':2,"
                + "'entries':[{'type':'minecraft:item','name':'minecraft:stone'}]}]}}");
    assertThat(inline.kind()).isInstanceOf(LootEntry.Kind.LootTableRef.class);
    LootEntry.TableRef ref = ((LootEntry.Kind.LootTableRef) inline.kind()).table();
    assertThat(ref).isInstanceOf(LootEntry.TableRef.Inline.class);
    LootTable nested = ((LootEntry.TableRef.Inline) ref).table();
    assertThat(nested.pools().getFirst().rolls()).isEqualTo(new IntProvider.Constant(2));
  }

  @Test
  void aDynamicEntryReadsItsName() {
    // spec: LT-R9 (dynamic: name, e.g. minecraft:sherds)
    assertThat(firstEntry("{'type':'minecraft:dynamic','name':'minecraft:sherds'}").kind())
        .isEqualTo(new LootEntry.Kind.Dynamic(Identifier.parse("minecraft:sherds")));
  }

  @Test
  void aTagEntryReadsItsTagAndExpand() {
    // spec: LT-R9 (tag: items and expand)
    assertThat(
            firstEntry("{'type':'minecraft:tag','items':'#minecraft:test_gems','expand':true}")
                .kind())
        .isEqualTo(new LootEntry.Kind.Tag(Identifier.parse("minecraft:test_gems"), true));
  }

  @Test
  void provisionalAnAbsentExpandReadsAsFalse() {
    // spec: LT-R22 (expand absent: defaut non documente, provisoire false)
    LootEntry.Kind kind =
        firstEntry("{'type':'minecraft:tag','items':'#minecraft:test_gems'}").kind();
    assertThat(kind)
        .isEqualTo(new LootEntry.Kind.Tag(Identifier.parse("minecraft:test_gems"), false));
  }

  @Test
  void theCompositeEntriesReadTheirChildrenInFileOrder() {
    // spec: LT-R9, LT-R21 (alternatives, sequence, group)
    String children =
        "'children':[{'type':'minecraft:item','name':'minecraft:stone'},"
            + "{'type':'minecraft:item','name':'minecraft:dirt'}]";
    LootEntry.Kind alternatives =
        firstEntry("{'type':'minecraft:alternatives'," + children + "}").kind();
    LootEntry.Kind sequence = firstEntry("{'type':'minecraft:sequence'," + children + "}").kind();
    LootEntry.Kind group = firstEntry("{'type':'minecraft:group'," + children + "}").kind();
    assertThat(((LootEntry.Kind.Alternatives) alternatives).children()).hasSize(2);
    assertThat(((LootEntry.Kind.Sequence) sequence).children()).hasSize(2);
    assertThat(((LootEntry.Kind.Group) group).children()).hasSize(2);
    assertThat(((LootEntry.Kind.Group) group).children().get(1).kind())
        .isEqualTo(new LootEntry.Kind.Item(LootItems.type("dirt")));
  }

  @Test
  void anUnknownEntryTypeIsRefused() {
    // spec: LT-R7 (9 registered types)
    Decoded.failsWith(
        decode(onePool("{'type':'minecraft:no_such_entry'}")), LootFindingCode.UNKNOWN_TYPE);
  }

  @Test
  void anUnknownItemIsAnInvalidIdentifier() {
    // spec: LootTableCodec#decode contract (unknown item: INVALID_IDENTIFIER)
    Decoded.failsWith(
        decode(onePool("{'type':'minecraft:item','name':'minecraft:no_such_item'}")),
        LootFindingCode.INVALID_IDENTIFIER);
  }

  @Test
  void aMissingRequiredFieldIsReported() {
    // spec: LT-R5a (rolls and entries on every pool), LT-R9 (name on item)
    Decoded.failsWith(
        decode("{'type':'minecraft:chest','pools':[{'entries':[]}]}"),
        LootFindingCode.MISSING_FIELD);
    Decoded.failsWith(
        decode("{'type':'minecraft:chest','pools':[{'rolls':1}]}"), LootFindingCode.MISSING_FIELD);
    Decoded.failsWith(decode(onePool("{'type':'minecraft:item'}")), LootFindingCode.MISSING_FIELD);
  }

  @Test
  void aWrongKindOfValueIsATypeMismatch() {
    // spec: LootDecoding contract
    Decoded.failsWith(decode("[]"), LootFindingCode.TYPE_MISMATCH);
    Decoded.failsWith(
        decode("{'type':'minecraft:chest','pools':{'rolls':1}}"), LootFindingCode.TYPE_MISMATCH);
  }

  @Test
  void theFormsOfEarlierVersionsAreNotDecoded() {
    // spec: LT-R3 (conditions, functions, function, reference are out of scope)
    Decoded.fails(
        decode("{'type':'minecraft:chest','pools':[{'rolls':1,'conditions':[],'entries':[]}]}"));
    Decoded.fails(
        decode(onePool("{'type':'minecraft:item','name':'minecraft:stone','functions':[]}")));
  }

  @Test
  void provisionalAFieldOutsideTheShapeIsRefused() {
    // spec: LootFindingCode#UNKNOWN_FIELD (refused until probe PL17), LT-R3
    Decoded.failsWith(
        decode("{'type':'minecraft:chest','pools':[],'no_such_field':1}"),
        LootFindingCode.UNKNOWN_FIELD);
  }

  @Test
  void everyDecodingRespectsTheLootDecodingContract() {
    // spec: LootDecoding contract (paths rooted at $)
    Decoded.assertWellFormed(decode("{'type':'minecraft:chest','pools':[]}"));
    Decoded.assertWellFormed(decode(onePool("{'type':'minecraft:item'}")));
    Decoded.assertWellFormed(decode(onePool("{'type':'minecraft:no_such_entry'}")));
  }
}
