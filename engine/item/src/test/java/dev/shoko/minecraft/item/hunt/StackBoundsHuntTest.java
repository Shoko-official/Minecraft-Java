package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.Json;
import dev.shoko.minecraft.item.testing.Nbt;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: item-components-inventory IC-R16 (quantity 1 to 99, provisional), IC-R21 (wear), and the
// boundaries of the stack codecs found by the bug hunt (defects B4, B6, M05, M06, M62).
class StackBoundsHuntTest {

  private static final StackCodec STACKS = TestItems.stackCodec();

  private static Decoding<ItemStack> fromJson(int count) {
    return STACKS.fromJson(Json.parse("{'id':'minecraft:stone','count':" + count + "}"));
  }

  private static Decoding<ItemStack> fromNbt(long count) {
    return STACKS.fromNbt(Nbt.compound("{id:\"minecraft:stone\",count:" + count + "}"));
  }

  @Test
  void provisionalJsonCountIsAcceptedAt99AndRefusedAt100() {
    // spec: IC-R16
    assertThat(Findings.value(fromJson(99)).count()).isEqualTo(99);
    assertThat(Findings.value(fromJson(1)).count()).isEqualTo(1);
    Findings.assertFailure(fromJson(100), FindingCode.VALUE_RANGE);
    Findings.assertFailure(fromJson(Integer.MAX_VALUE), FindingCode.VALUE_RANGE);
  }

  @Test
  void provisionalNbtCountIsAcceptedAt99() {
    // spec: IC-R16, IC-R26
    assertThat(Findings.value(fromNbt(99)).count()).isEqualTo(99);
    assertThat(Findings.value(fromNbt(64)).count()).isEqualTo(64);
  }

  @Test
  void provisionalNbtCountAbove99NeverGivesAStackAbove99() {
    // spec: IC-R16 (defect B4: the JSON form bounds the count to 1..99, the NBT form must not
    // read a larger one as is; refusing it and reading it as 99 are both bounded)
    for (long count : List.of(100L, 101L, 1000L, 100_000L, (long) Integer.MAX_VALUE)) {
      Decoding<ItemStack> decoded = fromNbt(count);
      if (decoded instanceof Decoding.Success<ItemStack> ok) {
        assertThat(ok.value().count())
            .as("count %d", count)
            .isLessThanOrEqualTo(ItemStack.MAX_COUNT);
      }
    }
  }

  @Test
  void provisionalAStackLimitIsClampedToTheRangeOneToNinetyNine() {
    // spec: IC-R16 "plage valide 1 a 99" (mutants M05 and M06: 100 for 99, 0 for 1)
    Map<String, Integer> expected = new LinkedHashMap<>();
    expected.put("-5", 1);
    expected.put("0", 1);
    expected.put("1", 1);
    expected.put("16", 16);
    expected.put("99", 99);
    expected.put("100", 99);
    expected.put("2147483647", 99);
    expected.forEach(
        (limit, effective) -> {
          ItemStack stack =
              TestItems.stack("stone", 1)
                  .withComponent(
                      TestItems.id("max_stack_size"), TestItems.value("max_stack_size", limit));
          assertThat(stack.maxStackSize()).as("max_stack_size %s", limit).isEqualTo(effective);
        });
  }

  @Test
  void provisionalWearNeverLeavesTheDamageOutsideZeroToMaxDamage() {
    // spec: IC-R21 (defect B6: negative wear and Integer.MIN_VALUE)
    List<Integer> amounts =
        List.of(
            Integer.MIN_VALUE,
            -1_000_000,
            -131,
            -5,
            -1,
            0,
            1,
            30,
            130,
            131,
            132,
            Integer.MAX_VALUE);
    for (String damage : List.of("0", "100")) {
      ItemStack pickaxe =
          TestItems.stack("stone_pickaxe", 1)
              .withComponent(TestItems.id("damage"), TestItems.value("damage", damage));
      for (int amount : amounts) {
        ItemStack worn = pickaxe.wornBy(amount);
        if (!worn.isEmpty()) {
          assertThat(worn.damage()).as("damage %s wear %d", damage, amount).isBetween(0, 130);
        }
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalRandomWearKeepsTheDamageInRange() {
    // spec: IC-R21
    Random random = new Random(0x6EA4L);
    ItemStack pickaxe = TestItems.stack("stone_pickaxe", 1);
    for (int i = 0; i < 5000; i++) {
      ItemStack worn = pickaxe;
      for (int step = 0; step < 4; step++) {
        worn = worn.isEmpty() ? worn : worn.wornBy(random.nextInt(300) - 120);
      }
      if (!worn.isEmpty()) {
        assertThat(worn.damage()).isBetween(0, 130);
      }
    }
  }

  @Test
  void provisionalAHugeTotalOfOneUnitStacksIsRefusedNotAllocated() {
    // spec: IC-R16 (finding 9: give of a huge total for an item that does not stack)
    ItemStack pickaxe = TestItems.stack("stone_pickaxe", 1);
    assertThatThrownBy(() -> pickaxe.stacksFor(1_000_001))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> pickaxe.stacksFor(2_000_000))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void provisionalAnOrdinaryTotalStillDistributes() {
    // spec: IC-R16, V-I14
    ItemStack stone = TestItems.stack("stone", 1);
    assertThat(stone.stacksFor(10_000_000)).hasSize(156_250);
    assertThat(TestItems.stack("stone_pickaxe", 1).stacksFor(1000)).hasSize(1000);
  }

  private static ItemStack withNaN() {
    LinkedHashMap<String, dev.shoko.minecraft.core.nbt.NbtTag> entries = new LinkedHashMap<>();
    entries.put("a", FloatTag.of(Float.NaN));
    return TestItems.stack("stone", 1)
        .withComponent(
            Identifier.minecraft("custom_data"), new ComponentValue(CompoundTag.of(entries)));
  }

  @Test
  void provisionalAFloatThatJsonCannotWriteIsATypedErrorOrAValidDocument() {
    // spec: IC-R12 (finding 10: NaN and infinities have no JSON number; the documented outcome is
    // an IllegalArgumentException, a writer that finds a representation is also acceptable)
    ItemStack nan = withNaN();
    try {
      String text = STACKS.toJson(nan);
      assertThat(Json.parseStrict(text)).isNotNull();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).isNotNull();
    }
    try {
      String text =
          TestItems.componentCodec()
              .valueToJson(
                  Identifier.minecraft("custom_data"),
                  nan.component(Identifier.minecraft("custom_data")).orElseThrow());
      assertThat(Json.parseStrict(text)).isNotNull();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).isNotNull();
    }
  }
}
