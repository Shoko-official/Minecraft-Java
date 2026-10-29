package dev.shoko.minecraft.loot.modifier;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.loot.context.EntityTarget;
import dev.shoko.minecraft.loot.context.LootContext;
import dev.shoko.minecraft.loot.number.IntProvider;
import dev.shoko.minecraft.loot.predicate.LootCondition;
import dev.shoko.minecraft.loot.predicate.match.ItemMatch;
import dev.shoko.minecraft.loot.random.DrawReason;
import dev.shoko.minecraft.loot.testing.Ctx;
import dev.shoko.minecraft.loot.testing.DrawLog;
import dev.shoko.minecraft.loot.testing.FakeEntity;
import dev.shoko.minecraft.loot.testing.Json;
import dev.shoko.minecraft.loot.testing.LootItems;
import dev.shoko.minecraft.loot.testing.ScriptedRandom;
import dev.shoko.minecraft.loot.testing.Sets;
import dev.shoko.minecraft.loot.testing.Tables;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: LT-R38 (conditions of a modifier), LT-R39 (set_count), LT-R40 (limit_count), LT-R46
// (discard, filtered), LT-R47 (identity), LT-R48/LT-R49 (order, empty stacks)
class ModifierApplierTest {

  private static ModifierResult apply(ItemModifier modifier, ItemStack stack, LootContext context) {
    return ModifierApplier.apply(modifier, stack, context);
  }

  private static LootContext plain() {
    return Ctx.context(ScriptedRandom.of());
  }

  private static ItemStack stone(int count) {
    return LootItems.stack("stone", count);
  }

  @Test
  void provisionalSetCountReplacesTheCount() {
    // spec: LT-R39
    ModifierResult result = apply(Tables.setCount(5), stone(2), plain());
    assertThat(result.stack().count()).isEqualTo(5);
    assertThat(result.stack().type().id()).isEqualTo(Identifier.minecraft("stone"));
    assertThat(result.cause()).isEqualTo(ModifierCause.APPLIED);
  }

  @Test
  void provisionalSetCountWithAddAddsToTheExistingCount() {
    // spec: LT-R39 (add: true)
    assertThat(apply(Tables.addCount(4), stone(3), plain()).stack().count()).isEqualTo(7);
  }

  @Test
  void provisionalSetCountKeepsACountAboveTheStackSize() {
    // spec: LT-R54, ModifierApplier#apply contract (the split is done by the table)
    assertThat(apply(Tables.setCount(70), stone(1), plain()).stack().count()).isEqualTo(70);
  }

  @Test
  void provisionalSetCountDrawsOnceForAUniformCount() {
    // spec: LT-R39, LT-R51
    ScriptedRandom random = ScriptedRandom.of(0.999);
    ModifierResult result = apply(Tables.uniformCount(1, 3), stone(1), Ctx.context(random));
    assertThat(result.stack().count()).isEqualTo(3);
    assertThat(random.steps()).isEqualTo(1);
  }

  @Test
  void provisionalLimitCountClampsOnBothSides() {
    // spec: LT-R40 ({"max":4,"min":1})
    ItemModifier limit = Tables.limit(2, 4);
    assertThat(apply(limit, stone(1), plain()).stack().count()).isEqualTo(2);
    assertThat(apply(limit, stone(3), plain()).stack().count()).isEqualTo(3);
    assertThat(apply(limit, stone(9), plain()).stack().count()).isEqualTo(4);
  }

  @Test
  void provisionalLimitCountToZeroRemovesTheStack() {
    // spec: LT-R40, LT-R49 (limit_count max 0 empties the stack)
    ModifierResult result = apply(Tables.limit(0, 0), stone(5), plain());
    assertThat(result.stack().isEmpty()).isTrue();
  }

  @Test
  void provisionalLimitCountWithOnlyAMaximumLeavesSmallStacksAlone() {
    // spec: LT-R40 ({"max":9})
    assertThat(apply(Tables.limitMax(9), stone(3), plain()).stack().count()).isEqualTo(3);
    assertThat(apply(Tables.limitMax(9), stone(30), plain()).stack().count()).isEqualTo(9);
  }

  @Test
  void provisionalAFailingConditionLeavesTheStackUnchanged() {
    // spec: LT-R38 (condition on every modifier)
    ItemModifier modifier =
        Tables.modifier(
            new ModifierAction.SetCount(new IntProvider.Constant(9), false),
            new LootCondition.KilledByPlayer());
    ItemStack input = stone(2);
    ModifierResult result = apply(modifier, input, plain());
    assertThat(result.stack()).isEqualTo(input);
    assertThat(result.cause()).isEqualTo(ModifierCause.CONDITION_FAILED);
  }

  @Test
  void provisionalAHoldingConditionLetsTheActionRun() {
    // spec: LT-R38
    ItemModifier modifier =
        Tables.modifier(
            new ModifierAction.SetCount(new IntProvider.Constant(9), false),
            new LootCondition.KilledByPlayer());
    LootContext killed =
        Ctx.builder(ScriptedRandom.of())
            .entity(EntityTarget.ATTACKING_PLAYER, FakeEntity.of("player"))
            .build();
    ModifierResult result = apply(modifier, stone(2), killed);
    assertThat(result.stack().count()).isEqualTo(9);
    assertThat(result.cause()).isEqualTo(ModifierCause.APPLIED);
  }

  @Test
  void provisionalAllConditionsMustHoldAndTheyAreEvaluatedBeforeTheAction() {
    // spec: LT-R38 (conditions in order, before the action), LT-R66 (order of the draws)
    ItemModifier modifier =
        Tables.modifier(
            new ModifierAction.SetCount(Tables.uniform(1, 3), false), Tables.chance(0.5f));
    ScriptedRandom random = ScriptedRandom.of(0.1, 0.999);
    ModifierResult result = apply(modifier, stone(1), Ctx.context(random));
    assertThat(result.stack().count()).isEqualTo(3);
    assertThat(random.steps()).isEqualTo(2);
    ScriptedRandom refused = ScriptedRandom.of(0.9);
    ModifierResult skipped = apply(modifier, stone(1), Ctx.context(refused));
    assertThat(skipped.stack().count()).isEqualTo(1);
    assertThat(refused.steps()).as("the action did not draw").isEqualTo(1);
  }

  @Test
  void provisionalTheDrawsAreReportedWithTheirReasons() {
    // spec: ModifierApplier contract (MODIFIER_CONDITION then MODIFIER), LT-R66
    DrawLog log = new DrawLog();
    ScriptedRandom random = ScriptedRandom.of(0.1, 0.5);
    LootContext context = Ctx.builder(random).observer(log).build();
    ItemModifier modifier =
        Tables.modifier(
            new ModifierAction.SetCount(Tables.uniform(1, 3), false), Tables.chance(0.5f));
    apply(modifier, stone(1), context);
    assertThat(log.consumingReasons())
        .containsExactly(DrawReason.MODIFIER_CONDITION, DrawReason.MODIFIER);
  }

  @Test
  void provisionalDiscardEmptiesTheStack() {
    // spec: LT-R46 (discard)
    ModifierResult result = apply(Tables.discard(), stone(4), plain());
    assertThat(result.stack().isEmpty()).isTrue();
  }

  @Test
  void provisionalAModifierDoesNothingToAnAlreadyEmptyStack() {
    // spec: LT-R49
    ModifierResult result = apply(Tables.setCount(5), ItemStack.empty(), plain());
    assertThat(result.stack().isEmpty()).isTrue();
    assertThat(result.cause()).isEqualTo(ModifierCause.EMPTY_INPUT);
    assertThat(apply(Tables.addCount(5), ItemStack.empty(), plain()).stack().isEmpty()).isTrue();
  }

  @Test
  void provisionalADeferredModifierIsTheIdentity() {
    // spec: LT-R47 (the 21 types without behavior pass the stack unchanged)
    ItemModifier deferred =
        Tables.modifier(
            new ModifierAction.Deferred(
                Identifier.minecraft("set_lore"), (JsonObject) Json.parse("{}")));
    ItemStack input = stone(3);
    ModifierResult result = apply(deferred, input, plain());
    assertThat(result.stack()).isEqualTo(input);
    assertThat(result.cause()).isEqualTo(ModifierCause.DEFERRED_IDENTITY);
  }

  @Test
  void provisionalFilteredAppliesOnPassOrOnFail() {
    // spec: LT-R46 (item_filter, on_pass, on_fail)
    ItemMatch shears =
        new ItemMatch(
            Sets.items("shears"), Sets.anyNumber(), new LinkedHashMap<>(), new LinkedHashMap<>());
    ItemModifier filtered =
        Tables.modifier(
            new ModifierAction.Filtered(
                shears, List.of(Tables.setCount(5)), List.of(Tables.discard())));
    assertThat(apply(filtered, LootItems.stack("shears"), plain()).stack().count()).isEqualTo(5);
    assertThat(apply(filtered, stone(1), plain()).stack().isEmpty()).isTrue();
  }

  @Test
  void provisionalFilteredWithoutABranchLeavesTheStack() {
    // spec: LT-R46
    ItemMatch shears =
        new ItemMatch(
            Sets.items("shears"), Sets.anyNumber(), new LinkedHashMap<>(), new LinkedHashMap<>());
    ItemModifier filtered =
        Tables.modifier(new ModifierAction.Filtered(shears, List.of(), List.of()));
    ItemStack input = stone(2);
    assertThat(apply(filtered, input, plain()).stack()).isEqualTo(input);
  }

  @Test
  void provisionalFurnaceSmeltWithoutARecipeLeavesTheStack() {
    // spec: LT-R44k
    ItemStack input = LootItems.stack("beef", 2);
    ModifierResult result =
        apply(Tables.modifier(new ModifierAction.FurnaceSmelt()), input, plain());
    assertThat(result.stack()).isEqualTo(input);
  }

  @Test
  void provisionalCopyComponentsWithoutABlockEntityLeavesTheStack() {
    // spec: ModifierCause#PARAMETER_ABSENT, LT-R44j
    ItemModifier copy =
        Tables.modifier(
            new ModifierAction.CopyComponents(
                ModifierAction.ComponentSource.BLOCK_ENTITY,
                List.of(Identifier.minecraft("pot_decorations")),
                List.of()));
    ItemStack input = LootItems.stack("decorated_pot");
    ModifierResult result = apply(copy, input, plain());
    assertThat(result.stack()).isEqualTo(input);
    assertThat(result.cause()).isEqualTo(ModifierCause.PARAMETER_ABSENT);
  }

  @Test
  void provisionalCopyStateWithoutABlockStateLeavesTheStack() {
    // spec: ModifierCause#PARAMETER_ABSENT, LT-R44i
    ItemModifier copy =
        Tables.modifier(
            new ModifierAction.CopyState(Identifier.minecraft("wheat"), List.of("age")));
    ItemStack input = stone(1);
    ModifierResult result = apply(copy, input, plain());
    assertThat(result.stack()).isEqualTo(input);
    assertThat(result.cause()).isEqualTo(ModifierCause.PARAMETER_ABSENT);
  }

  @Test
  void provisionalApplyAllRunsModifierByModifierInOrder() {
    // spec: LT-R48 (modifiers in the order of the list)
    List<ItemStack> result =
        ModifierApplier.applyAll(
            List.of(Tables.setCount(2), Tables.addCount(3)), List.of(stone(1)), plain());
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().count()).isEqualTo(5);
    List<ItemStack> reversed =
        ModifierApplier.applyAll(
            List.of(Tables.addCount(3), Tables.setCount(2)), List.of(stone(1)), plain());
    assertThat(reversed.getFirst().count()).isEqualTo(2);
  }

  @Test
  void provisionalApplyAllDrawsModifierByModifierThenStackByStack() {
    // spec: ModifierApplier#applyAll contract (order of the draws)
    ScriptedRandom random = ScriptedRandom.of(0.0, 0.999);
    List<ItemStack> result =
        ModifierApplier.applyAll(
            List.of(Tables.uniformCount(1, 3), Tables.addCount(10)),
            List.of(stone(1), LootItems.stack("dirt")),
            Ctx.context(random));
    // the first modifier draws for the stone (0.0 -> 1) then for the dirt (0.999 -> 3)
    assertThat(result.stream().map(ItemStack::count).toList()).containsExactly(11, 13);
    assertThat(random.steps()).isEqualTo(2);
  }

  @Test
  void provisionalApplyAllDropsAStackThatBecomesEmptyAndDoesNotPassItOn() {
    // spec: LT-R49
    ScriptedRandom random = ScriptedRandom.of();
    List<ItemStack> result =
        ModifierApplier.applyAll(
            List.of(Tables.discard(), Tables.setCount(5)),
            List.of(stone(1), LootItems.stack("dirt")),
            Ctx.context(random));
    assertThat(result).isEmpty();
  }

  @Test
  void provisionalApplyAllKeepsTheOtherStacksWhenOneIsRemoved() {
    // spec: LT-R49
    List<ItemStack> result =
        ModifierApplier.applyAll(
            List.of(Tables.limit(2, 9)),
            List.of(stone(1), LootItems.stack("dirt", 4)),
            Ctx.context(Lcg48Random.ofSeed(1)));
    assertThat(result.stream().map(s -> s.type().id().path()).toList())
        .containsExactly("stone", "dirt");
    assertThat(result.stream().map(ItemStack::count).toList()).containsExactly(2, 4);
  }

  @Test
  void provisionalApplyAllDoesNotChangeItsInput() {
    // spec: ModifierApplier#applyAll contract (stacks not changed)
    List<ItemStack> input = List.of(stone(1));
    ModifierApplier.applyAll(List.of(Tables.setCount(7)), input, plain());
    assertThat(input.getFirst().count()).isEqualTo(1);
  }

  @Test
  void provisionalApplyAllWithNoModifierReturnsTheStacks() {
    // spec: LT-R48
    List<ItemStack> input = List.of(stone(2), LootItems.stack("dirt", 3));
    assertThat(ModifierApplier.applyAll(List.of(), input, plain()))
        .containsExactlyElementsOf(input);
  }
}
