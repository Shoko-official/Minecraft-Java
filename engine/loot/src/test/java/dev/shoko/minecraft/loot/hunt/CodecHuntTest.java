package dev.shoko.minecraft.loot.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.loot.LootDecoding;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.eval.LootEvaluator;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.table.LootTableCodec;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.TestAccess;
import org.junit.jupiter.api.Test;

// Adversarial tests on the decoding of tables against the defaults of the spec.
class CodecHuntTest {

  private static LootDecoding<LootTable> decode(String json) {
    return LootTableCodec.decode(Json.parse(json), TestAccess.standard(), LootItems.catalog());
  }

  private static LootTable table(String json) {
    LootDecoding<LootTable> decoded = decode(json);
    assertThat(decoded)
        .as("findings %s", decoded.findings())
        .isInstanceOf(LootDecoding.Success.class);
    return ((LootDecoding.Success<LootTable>) decoded).value();
  }

  // spec: LT-R4b: a list of conditions given to condition is an all_of (provisional).
  @Test
  void provisionalAListOfPoolConditionsIsAnAllOf() {
    LootTable t =
        table(
            "{'type':'minecraft:chest','pools':[{'rolls':1,'condition':["
                + "{'type':'minecraft:random_chance','chance':1.0},"
                + "{'type':'minecraft:random_chance','chance':0.0}],"
                + "'entries':[{'type':'minecraft:item','name':'minecraft:stick'}]}]}");
    LootContext context = Ctx.context(Lcg48Random.ofSeed(1));
    for (int i = 0; i < 100; i++) {
      assertThat(LootEvaluator.evaluate(t, context).stacks()).isEmpty();
    }
  }

  // spec: LT-R29: a list nested in terms behaves as an all_of.
  @Test
  void provisionalAListNestedInTermsIsAnAllOf() {
    LootTable t =
        table(
            "{'type':'minecraft:chest','pools':[{'rolls':1,'condition':{'type':'minecraft:any_of',"
                + "'terms':[[{'type':'minecraft:random_chance','chance':1.0},"
                + "{'type':'minecraft:random_chance','chance':0.0}]]},"
                + "'entries':[{'type':'minecraft:item','name':'minecraft:stick'}]}]}");
    LootContext context = Ctx.context(Lcg48Random.ofSeed(1));
    for (int i = 0; i < 100; i++) {
      assertThat(LootEvaluator.evaluate(t, context).stacks()).isEmpty();
    }
  }

  // spec: LT-R8 defaults weight 1 / quality 0, LT-R22 expand default false, LT-R5b bonus_rolls 0.
  @Test
  void provisionalOmittedFieldsTakeTheDocumentedDefaults() {
    LootTable t =
        table(
            "{'pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:stick'},"
                + "{'type':'minecraft:tag','items':'#minecraft:test_gems'}]}]}");
    assertThat(t.type().id().path()).isEqualTo("generic");
    var pool = t.pools().getFirst();
    assertThat(pool.entries().getFirst().weight()).isEqualTo(1);
    assertThat(pool.entries().getFirst().quality()).isZero();
    var tag = (dev.shoko.minecraft.loot.table.LootEntry.Kind.Tag) pool.entries().get(1).kind();
    assertThat(tag.expand()).isFalse();
  }

  // spec: LT-R6 provisional: a fractional rolls (PL17) must not crash the decoder.
  @Test
  void provisionalAFractionalRollsIsDecodedOrRefusedCleanly() {
    LootDecoding<LootTable> decoded =
        decode(
            "{'type':'minecraft:chest','pools':[{'rolls':1.5,"
                + "'entries':[{'type':'minecraft:empty'}]}]}");
    assertThat(decoded.findings()).isNotNull();
  }

  // spec: LT-R4a: the pool condition may be a bare named predicate.
  @Test
  void provisionalANamedPredicateIsResolvedByTheReferenceCondition() {
    LootTable t =
        table(
            "{'type':'minecraft:chest','pools':[{'rolls':1,'condition':'minecraft:tool/can_shear',"
                + "'entries':[{'type':'minecraft:empty'}]}]}");
    assertThat(t.pools().getFirst().conditions().getFirst())
        .isInstanceOf(LootCondition.Reference.class);
  }
}
