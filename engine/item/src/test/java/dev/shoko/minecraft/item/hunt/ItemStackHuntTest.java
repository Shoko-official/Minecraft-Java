package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.MergeResult;
import dev.shoko.minecraft.item.stack.SplitResult;
import dev.shoko.minecraft.item.testing.Nbt;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ItemStackHuntTest {

  private static final List<String> LIMITS =
      List.of("-5", "0", "1", "16", "64", "99", "100", "2147483647");

  private static ItemStack random(Random random) {
    ItemStack base = TestItems.stack(random.nextBoolean() ? "stone" : "ender_pearl", 1);
    if (random.nextInt(3) == 0) {
      base =
          base.withComponent(
              TestItems.id("max_stack_size"),
              TestItems.value("max_stack_size", LIMITS.get(random.nextInt(LIMITS.size()))));
    }
    if (random.nextInt(3) == 0) {
      base = base.withComponent(TestItems.id("rarity"), new ComponentValue(new StringTag("epic")));
    }
    int bound = random.nextInt(4) == 0 ? 120 : base.maxStackSize();
    return base.withCount(random.nextInt(bound + 1) - (random.nextInt(8) == 0 ? 3 : 0));
  }

  @Test
  void mergeAndSplitConserveUnitsAndRespectTheLimit() {
    Random random = new Random(20261029L);
    for (int i = 0; i < 20000; i++) {
      ItemStack a = random(random);
      ItemStack b = random(random);
      MergeResult merged = a.mergeFrom(b);
      int before = Math.max(0, a.count()) + Math.max(0, b.count());
      int after = merged.merged().count() + merged.remainder().count();
      String where = a + " <- " + b + " = " + merged;
      if (a.sameContent(b) || a.isEmpty() || b.isEmpty()) {
        assertThat(after).as(where).isEqualTo(before);
      } else {
        assertThat(merged.merged()).as(where).isEqualTo(a);
        assertThat(merged.remainder()).as(where).isEqualTo(b);
      }
      if (!merged.merged().isEmpty() && !a.isEmpty() && a.count() <= a.maxStackSize()) {
        assertThat(merged.merged().count())
            .as(where)
            .isLessThanOrEqualTo(merged.merged().maxStackSize());
      }
      int amount = random.nextInt(130) - 10;
      SplitResult split = a.split(amount);
      assertThat(split.taken().count() + split.remaining().count())
          .as(a + " split " + amount)
          .isEqualTo(Math.max(0, a.count()));
      assertThat(split.taken().count()).isLessThanOrEqualTo(Math.max(0, amount));
    }
  }

  @Test
  void equalsAndHashCodeAgreeWhateverTheOrderOfTheModifications() {
    ItemStack a =
        TestItems.stack("stone", 5)
            .withComponent(TestItems.id("rarity"), new ComponentValue(new StringTag("epic")))
            .withComponent(TestItems.id("repair_cost"), new ComponentValue(new IntTag(3)));
    ItemStack b =
        TestItems.stack("stone", 5)
            .withComponent(TestItems.id("repair_cost"), new ComponentValue(new IntTag(3)))
            .withComponent(TestItems.id("rarity"), new ComponentValue(new StringTag("epic")));
    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a.sameContent(b)).isTrue();
  }

  @Test
  void customDataCompoundsWithTheSameEntriesInAnotherOrderAreTheSameContent() {
    ItemStack a =
        TestItems.stack("stone", 1)
            .withComponent(
                TestItems.id("custom_data"), new ComponentValue(Nbt.compound("{a:1,b:{c:2,d:3}}")));
    ItemStack b =
        TestItems.stack("stone", 1)
            .withComponent(
                TestItems.id("custom_data"), new ComponentValue(Nbt.compound("{b:{d:3,c:2},a:1}")));
    assertThat(a.sameContent(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  void floatsInComponentsCompareByBits() {
    ItemStack zero =
        TestItems.stack("stone", 1)
            .withComponent(
                TestItems.id("custom_data"),
                new ComponentValue(
                    CompoundTag.of(
                        new java.util.LinkedHashMap<>(java.util.Map.of("f", FloatTag.of(0.0f))))));
    ItemStack negative =
        TestItems.stack("stone", 1)
            .withComponent(
                TestItems.id("custom_data"),
                new ComponentValue(
                    CompoundTag.of(
                        new java.util.LinkedHashMap<>(java.util.Map.of("f", FloatTag.of(-0.0f))))));
    assertThat(zero.sameContent(negative)).isFalse();
    ItemStack nan =
        TestItems.stack("stone", 1)
            .withComponent(
                TestItems.id("custom_data"),
                new ComponentValue(
                    CompoundTag.of(
                        new java.util.LinkedHashMap<>(
                            java.util.Map.of("f", DoubleTag.of(Double.NaN))))));
    assertThat(nan.sameContent(nan.withCount(3))).isTrue();
  }

  @Test
  void wornToTheBreakingPointDisappearsAndOneBelowSurvives() {
    ItemStack pickaxe = TestItems.stack("stone_pickaxe", 1);
    assertThat(pickaxe.wornBy(130).damage()).isEqualTo(130);
    assertThat(pickaxe.wornBy(131).isEmpty()).isTrue();
    assertThat(pickaxe.wornBy(Integer.MAX_VALUE).isEmpty()).isTrue();
    assertThat(pickaxe.wornBy(0)).isEqualTo(pickaxe);
    assertThat(pickaxe.wornBy(130).wornBy(1).isEmpty()).isTrue();
  }

  @Test
  void stacksForSplitsATotalExactly() {
    ItemStack stone = TestItems.stack("stone", 1);
    List<ItemStack> stacks = stone.stacksFor(100);
    assertThat(stacks).extracting(ItemStack::count).containsExactly(64, 36);
    ItemStack single =
        stone.withComponent(TestItems.id("max_stack_size"), TestItems.value("max_stack_size", "1"));
    assertThat(single.stacksFor(3)).extracting(ItemStack::count).containsExactly(1, 1, 1);
  }

  @Test
  void removingAnAbsentComponentChangesNothingAndAnEmptyComponentIsNotAbsent() {
    ItemStack stone = TestItems.stack("stone", 4);
    ItemStack removed = stone.withoutComponent(TestItems.id("unbreakable"));
    assertThat(removed).isEqualTo(stone);
    assertThat(removed.patch().isEmpty()).isTrue();
    ItemStack present =
        stone.withComponent(TestItems.id("unbreakable"), new ComponentValue(CompoundTag.empty()));
    assertThat(present.sameContent(stone)).isFalse();
    ItemStack redundant =
        stone.withComponent(TestItems.id("rarity"), new ComponentValue(new StringTag("common")));
    assertThat(redundant).isEqualTo(stone);
  }

  @Test
  void extremeCountsGiveEmptyOrBoundedStacksWithoutOverflow() {
    assertThat(ItemStack.of(TestItems.type("stone"), Integer.MIN_VALUE).isEmpty()).isTrue();
    assertThat(ItemStack.of(TestItems.type("stone"), 0).isEmpty()).isTrue();
    ItemStack huge = ItemStack.of(TestItems.type("stone"), Integer.MAX_VALUE);
    MergeResult merged = TestItems.stack("stone", 10).mergeFrom(huge);
    assertThat(merged.merged().count()).isEqualTo(64);
    assertThat((long) merged.merged().count() + merged.remainder().count())
        .isEqualTo(10L + Integer.MAX_VALUE);
    SplitResult split = huge.split(Integer.MAX_VALUE);
    assertThat(split.taken().count()).isEqualTo(Integer.MAX_VALUE);
    assertThat(split.remaining().isEmpty()).isTrue();
  }
}
