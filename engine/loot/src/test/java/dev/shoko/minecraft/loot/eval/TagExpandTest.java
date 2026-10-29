package dev.shoko.minecraft.loot.eval;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.table.LootEntry;
import dev.shoko.minecraft.loot.table.LootTable;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// spec: LT-R22 (tag entry; H-tag-cond-b: each expanded child carries its own copy of the condition,
// evaluated separately at each draw; H-tag-mod-a: the modifiers of the entry apply to the
// children),
// V-L15 (two objects, random_chance 0.5: nothing with probability 0.25), probes PL8 and PL19.
class TagExpandTest {

  private static LootTable duo(LootEntry entry) {
    return Tables.table(Tables.pool(1, entry));
  }

  private static LootEntry conditionalDuo() {
    return Tables.when(Tables.tag("duo", true, 1), Tables.chance(0.5f));
  }

  @Test
  void provisionalTwoChildrenWithARandomConditionGiveNothingOneTimeInFour() {
    // spec: V-L15 (H-tag-cond-b: 0.25; H-tag-cond-a would give 0.5)
    LootContext context = Ctx.context(Lcg48Random.ofSeed(15));
    int samples = 20000;
    int nothing = 0;
    Map<String, Integer> counts = new HashMap<>();
    for (int i = 0; i < samples; i++) {
      List<ItemStack> stacks = LootEvaluator.evaluate(duo(conditionalDuo()), context).stacks();
      if (stacks.isEmpty()) {
        nothing++;
      }
      for (ItemStack stack : stacks) {
        counts.merge(stack.type().id().path(), 1, Integer::sum);
      }
    }
    assertThat(nothing / (double) samples).isBetween(0.22, 0.28);
    double stone = counts.getOrDefault("stone", 0);
    double dirt = counts.getOrDefault("dirt", 0);
    assertThat(stone / (stone + dirt)).isBetween(0.47, 0.53);
  }

  @Test
  void provisionalEachChildDrawsItsOwnConditionInTheOrderOfTheTag() {
    // spec: LT-R22 (H-tag-cond-b: one draw per child), LT-R20: both children pass, one pick follows
    ScriptedRandom random = ScriptedRandom.of(0.1, 0.1, 0.0);
    LootEvaluator.evaluate(duo(conditionalDuo()), Ctx.context(random));
    assertThat(random.steps()).isEqualTo(3);
    ScriptedRandom none = ScriptedRandom.of(0.9, 0.9);
    assertThat(LootEvaluator.evaluate(duo(conditionalDuo()), Ctx.context(none)).stacks()).isEmpty();
    assertThat(none.steps()).isEqualTo(2);
  }

  @Test
  void provisionalTheModifiersOfTheEntryApplyToTheExpandedChildren() {
    // spec: LT-R22 (H-tag-mod-a), probe PL8 (a)
    LootEntry entry = Tables.modified(Tables.tag("duo", true, 1), Tables.setCount(5));
    ScriptedRandom random = ScriptedRandom.of(0.0);
    List<ItemStack> stacks = LootEvaluator.evaluate(duo(entry), Ctx.context(random)).stacks();
    assertThat(stacks).hasSize(1);
    assertThat(stacks.getFirst().count()).isEqualTo(5);
  }

  @Test
  void provisionalAnUnexpandedTagProducesOneStackPerItemWithTheModifiers() {
    // spec: LT-R22 (1): a single entry producing every item, modifiers applied (H-tag-mod-a)
    LootEntry entry = Tables.modified(Tables.tag("duo", false, 1), Tables.setCount(5));
    List<ItemStack> stacks =
        LootEvaluator.evaluate(duo(entry), Ctx.context(ScriptedRandom.of())).stacks();
    assertThat(stacks.stream().map(s -> s.type().id().path()).toList())
        .containsExactlyInAnyOrder("stone", "dirt");
    assertThat(stacks).allSatisfy(s -> assertThat(s.count()).isEqualTo(5));
  }
}
