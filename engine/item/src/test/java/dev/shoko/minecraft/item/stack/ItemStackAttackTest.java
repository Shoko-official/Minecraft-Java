package dev.shoko.minecraft.item.stack;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: item-components-inventory IC-R14 to IC-R20 (adversarial): fixed seeds, conservation of
// units, equivalence of contents and consistency of equals and hashCode. Level S, hence the
// provisional prefix.
class ItemStackAttackTest {

  private static final int ROUNDS = 3000;

  /** Contents of every kind, with counts inside 1 to the stack limit. */
  private static ItemStack randomStack(Random random) {
    ItemStack base =
        switch (random.nextInt(8)) {
          case 0 -> TestItems.stack("stone", 1);
          case 1 -> StackSupport.withRarity("stone", 1, "epic");
          case 2 -> TestItems.stack("cobblestone", 1);
          case 3 -> TestItems.stack("ender_pearl", 1);
          case 4 -> TestItems.stack("bucket", 1);
          case 5 -> TestItems.stack("stone_pickaxe", 1);
          case 6 -> TestItems.stack("stone", 1, "enchantments", "{'minecraft:sharpness':2}");
          default -> ItemStack.empty();
        };
    if (base.isEmpty()) {
      return base;
    }
    return base.withCount(1 + random.nextInt(base.maxStackSize()));
  }

  private static List<ItemStack> randomStacks(Random random, int n) {
    List<ItemStack> stacks = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      stacks.add(randomStack(random));
    }
    return stacks;
  }

  @Test
  @Timeout(60)
  void provisionalMergeConservesUnitsAndRespectsTheLimit() {
    // spec: IC-R19
    Random random = new Random(0xA1L);
    for (int i = 0; i < ROUNDS; i++) {
      ItemStack target = randomStack(random);
      ItemStack source = randomStack(random);
      MergeResult result = target.mergeFrom(source);
      assertThat(result.merged().count() + result.remainder().count())
          .as("%s <- %s", target, source)
          .isEqualTo(target.count() + source.count());
      boolean compatible = target.isEmpty() || source.isEmpty() || target.sameContent(source);
      if (!compatible) {
        assertThat(result.merged()).as("%s <- %s", target, source).isEqualTo(target);
        assertThat(result.remainder()).as("%s <- %s", target, source).isEqualTo(source);
      } else if (!result.merged().isEmpty()) {
        assertThat(result.merged().count()).isLessThanOrEqualTo(result.merged().maxStackSize());
      }
      if (compatible && !result.merged().isEmpty() && !result.remainder().isEmpty()) {
        assertThat(result.merged().sameContent(result.remainder()))
            .as("%s <- %s", target, source)
            .isTrue();
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalSplitConservesUnitsAndKeepsTheContent() {
    // spec: IC-R20
    Random random = new Random(0xA2L);
    for (int i = 0; i < ROUNDS; i++) {
      ItemStack stack = randomStack(random);
      int amount = random.nextInt(stack.maxStackSize() + 4);
      SplitResult result = stack.split(amount);
      assertThat(result.taken().count() + result.remaining().count())
          .as("%s split %d", stack, amount)
          .isEqualTo(stack.count());
      assertThat(result.taken().count()).isEqualTo(Math.min(amount, stack.count()));
      if (!stack.isEmpty()) {
        if (!result.taken().isEmpty()) {
          assertThat(result.taken().sameContent(stack)).isTrue();
        }
        if (!result.remaining().isEmpty()) {
          assertThat(result.remaining().sameContent(stack)).isTrue();
        }
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalSplittingThenMergingBackGivesTheOriginal() {
    // spec: IC-R19, IC-R20
    Random random = new Random(0xA3L);
    for (int i = 0; i < ROUNDS; i++) {
      ItemStack stack = randomStack(random);
      SplitResult split = stack.split(random.nextInt(stack.count() + 2));
      MergeResult back = split.remaining().mergeFrom(split.taken());
      assertThat(back.remainder().isEmpty()).as("%s", stack).isTrue();
      assertThat(back.merged()).as("%s", stack).isEqualTo(stack);
    }
  }

  @Test
  @Timeout(60)
  void provisionalSameContentIsAnEquivalenceRelation() {
    // spec: IC-R18
    Random random = new Random(0xA4L);
    List<ItemStack> stacks = randomStacks(random, 40);
    for (ItemStack a : stacks) {
      assertThat(a.sameContent(a)).isTrue();
      for (ItemStack b : stacks) {
        assertThat(a.sameContent(b)).as("%s ~ %s", a, b).isEqualTo(b.sameContent(a));
        if (!a.sameContent(b)) {
          continue;
        }
        for (ItemStack c : stacks) {
          if (b.sameContent(c)) {
            assertThat(a.sameContent(c)).as("%s ~ %s ~ %s", a, b, c).isTrue();
          }
        }
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalEqualsMeansSameContentAndSameCountAndMatchesHashCode() {
    // spec: IC-R18
    Random random = new Random(0xA5L);
    List<ItemStack> stacks = randomStacks(random, 60);
    for (ItemStack a : stacks) {
      for (ItemStack b : stacks) {
        boolean expected =
            (a.isEmpty() && b.isEmpty()) || (a.sameContent(b) && a.count() == b.count());
        assertThat(a.equals(b)).as("%s vs %s", a, b).isEqualTo(expected);
        if (expected) {
          assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalGivingATotalSumsToTheTotalInStacksOfTheLimit() {
    // spec: IC-R16, V-I14
    Random random = new Random(0xA6L);
    for (int i = 0; i < ROUNDS; i++) {
      ItemStack model = randomStack(random);
      if (model.isEmpty()) {
        continue;
      }
      int total = random.nextInt(400) - 10;
      List<ItemStack> stacks = model.stacksFor(total);
      int max = model.maxStackSize();
      if (total <= 0) {
        assertThat(stacks).isEmpty();
        continue;
      }
      assertThat(stacks.stream().mapToInt(ItemStack::count).sum())
          .as("total %d", total)
          .isEqualTo(total);
      assertThat(stacks).hasSize((total + max - 1) / max);
      for (int k = 0; k < stacks.size(); k++) {
        assertThat(stacks.get(k).sameContent(model)).isTrue();
        assertThat(stacks.get(k).count()).isLessThanOrEqualTo(max);
        if (k < stacks.size() - 1) {
          assertThat(stacks.get(k).count()).isEqualTo(max);
        }
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalRandomEditsKeepTheNormalFormAndTheCount() {
    // spec: IC-R14, IC-R15, IC-R17
    Random random = new Random(0xA7L);
    String[] rarities = {"common", "uncommon", "epic"};
    for (int i = 0; i < ROUNDS; i++) {
      ItemStack stack = TestItems.stack("stone", 1 + random.nextInt(64));
      ItemStack edited = stack;
      for (int step = 0; step < 4; step++) {
        edited =
            switch (random.nextInt(3)) {
              case 0 ->
                  edited.withComponent(
                      StackSupport.rarityId(), StackSupport.text(rarities[random.nextInt(3)]));
              case 1 -> edited.withoutComponent(StackSupport.rarityId());
              default -> edited.withCount(random.nextInt(68) - 3);
            };
      }
      assertThat(edited.isEmpty()).isEqualTo(edited.count() <= 0);
      ItemStack common = edited.withComponent(StackSupport.rarityId(), StackSupport.text("common"));
      assertThat(common.sameContent(TestItems.stack("stone", 1)) || edited.isEmpty()).isTrue();
      assertThat(common.patch().isEmpty() || common.isEmpty()).isTrue();
    }
  }
}
