package dev.shoko.minecraft.loot.assembly;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.number.FloatProvider;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.number.NumberProviderSet;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.NamedPredicates;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.table.LootTables;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R1 (tables by identifier), LT-R61a (named predicates), LT-R50 (named providers): the
// finders of the immutable registries of the loaded data
class LootRegistriesTest {

  private static Identifier id(String path) {
    return Identifier.parse("minecraft:" + path);
  }

  @Test
  void aTableIsFoundByItsIdentifier() {
    // spec: LT-R1
    LootTable stone = Tables.table(Tables.pool(1, Tables.item("stone")));
    LootTables tables = new LootTables(new LinkedHashMap<>(Map.of(id("blocks/stone"), stone)));
    assertThat(tables.find(id("blocks/stone"))).containsSame(stone);
    assertThat(tables.find(id("blocks/dirt"))).isEmpty();
    assertThat(tables.find(Identifier.parse("other:blocks/stone"))).isEmpty();
  }

  @Test
  void aNamedPredicateIsFoundByItsIdentifier() {
    // spec: LT-R61a
    NamedPredicates named = Ctx.standardPredicates();
    assertThat(named.find(id("tool/can_shear"))).containsInstanceOf(LootCondition.MatchTool.class);
    assertThat(named.find(id("tool/can_silk_touch"))).isPresent();
    assertThat(named.find(id("block/fast_cooking"))).isEmpty();
  }

  @Test
  void aNamedProviderIsFoundByItsIdentifierInItsFamily() {
    // spec: LT-R50
    NumberProviderSet set =
        Ctx.providers(
            Map.of("cooking/time_coal", new IntProvider.Constant(1600)),
            Map.of("cooking/speed_default", new FloatProvider.Constant(1f)));
    assertThat(set.intProvider(id("cooking/time_coal"))).contains(new IntProvider.Constant(1600));
    assertThat(set.floatProvider(id("cooking/speed_default")))
        .contains(new FloatProvider.Constant(1f));
    assertThat(set.intProvider(id("cooking/speed_default"))).isEmpty();
    assertThat(set.floatProvider(id("cooking/time_coal"))).isEmpty();
  }
}
