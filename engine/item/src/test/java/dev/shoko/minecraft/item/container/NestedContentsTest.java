package dev.shoko.minecraft.item.container;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.component.WellKnownComponents;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Findings;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// spec: item-components-inventory IC-R23 (container component: list of {slot, item}), PI4 (depth
// limit: provisional). The value representation is never built by hand: it goes through
// NestedContents.write.
class NestedContentsTest {

  private static NestedContents view(int maxDepth) {
    return NestedContents.of(TestItems.stackCodec(), maxDepth);
  }

  private static ItemStack chestHolding(NestedContents view, ItemStack... inner) {
    List<SlottedStack> slotted = new ArrayList<>();
    for (int i = 0; i < inner.length; i++) {
      slotted.add(new SlottedStack(i, inner[i]));
    }
    return TestItems.stack("chest")
        .withComponent(WellKnownComponents.CONTAINER, view.write(slotted));
  }

  private static ComponentValue valueHolding(NestedContents view, ItemStack inner) {
    return view.write(List.of(new SlottedStack(0, inner)));
  }

  @Test
  void writeThenReadGivesBackTheNestedStacks() {
    // spec: IC-R23
    NestedContents view = view(3);
    List<SlottedStack> stacks =
        List.of(
            new SlottedStack(0, Cells.stack("stone:3")),
            new SlottedStack(5, Cells.stack("ender_pearl:2")),
            new SlottedStack(26, Cells.stack("stone_pickaxe")));
    assertThat(Findings.value(view.read(view.write(stacks)))).isEqualTo(stacks);
  }

  @Test
  void theComponentsOfNestedStacksSurviveTheRoundTrip() {
    // spec: IC-R23 (the nested stacks follow IC-R14 to IC-R20)
    NestedContents view = view(3);
    ItemStack epic = TestItems.stack("stone", 8, "rarity", "'epic'");
    List<SlottedStack> stacks = List.of(new SlottedStack(2, epic));
    List<SlottedStack> back = Findings.value(view.read(view.write(stacks)));
    assertThat(back).hasSize(1);
    assertThat(back.get(0).slot()).isEqualTo(2);
    assertThat(back.get(0).stack()).isEqualTo(epic);
    assertThat(back.get(0).stack().sameContent(Cells.stack("stone"))).isFalse();
  }

  @Test
  void anEmptyListRoundTrips() {
    // spec: IC-R23 (prototype value [] of the 45 container items)
    NestedContents view = view(3);
    assertThat(Findings.value(view.read(view.write(List.of())))).isEmpty();
  }

  @Test
  void thePrototypeValueOfAChestReadsAsNoStack() {
    // spec: IC-R23, IC-R2 (the prototype container is the empty list)
    ComponentValue prototype =
        TestItems.stack("chest").component(WellKnownComponents.CONTAINER).orElseThrow();
    assertThat(Findings.value(view(3).read(prototype))).isEmpty();
  }

  @Test
  void theOrderOfTheListIsKept() {
    // spec: IC-R23 (stacks in list order)
    NestedContents view = view(3);
    List<SlottedStack> stacks =
        List.of(
            new SlottedStack(9, Cells.stack("dirt")),
            new SlottedStack(1, Cells.stack("stone")),
            new SlottedStack(4, Cells.stack("stick")));
    assertThat(Findings.value(view.read(view.write(stacks)))).isEqualTo(stacks);
  }

  @Test
  void aValueThatIsNotAListOfStacksIsAFailure() {
    // spec: IC-R23
    ComponentValue text = new ComponentValue(new StringTag("not a container"));
    Findings.assertAnyFailure(view(3).read(text));
  }

  @Test
  void provisionalAFlatValueIsAcceptedAtDepthOne() {
    // spec: IC-R23, PI4. Hypothesis (the Javadoc of NestedContents.of): the stacks of the value
    // are depth 1; a stack holding nothing adds no depth.
    NestedContents view = view(1);
    ItemStack emptyChest = TestItems.stack("chest");
    ComponentValue value =
        view.write(
            List.of(new SlottedStack(0, Cells.stack("stone")), new SlottedStack(1, emptyChest)));
    assertThat(Findings.value(view.read(value))).hasSize(2);
  }

  @Test
  void provisionalAContainerInsideAContainerNeedsDepthTwo() {
    // spec: IC-R23, PI4. Hypothesis: the stacks of the value are depth 1, the stacks held by
    // one of them depth 2 (the Javadoc of NestedContents.of).
    NestedContents deep = view(3);
    ItemStack chestWithStone = chestHolding(deep, Cells.stack("stone"));
    ComponentValue value = valueHolding(deep, chestWithStone);
    Findings.assertFailure(view(1).read(value), FindingCode.NESTING_TOO_DEEP);
    assertThat(Findings.value(view(2).read(value))).hasSize(1);
    assertThat(Findings.value(view(3).read(value))).hasSize(1);
  }

  @Test
  void provisionalThreeLevelsNeedDepthThree() {
    // spec: IC-R23, PI4 (same hypothesis as above)
    NestedContents deep = view(5);
    ItemStack level2 = chestHolding(deep, Cells.stack("stone"));
    ItemStack level3 = chestHolding(deep, level2);
    ComponentValue value = valueHolding(deep, level3);
    Findings.assertFailure(view(1).read(value), FindingCode.NESTING_TOO_DEEP);
    Findings.assertFailure(view(2).read(value), FindingCode.NESTING_TOO_DEEP);
    assertThat(Findings.value(view(3).read(value))).hasSize(1);
    assertThat(Findings.value(view(4).read(value))).hasSize(1);
  }

  @Test
  void provisionalTheLimitDoesNotChangeWhatAFlatValueReads() {
    // spec: IC-R23, PI4
    ItemStack stone = Cells.stack("stone:5");
    ComponentValue value = valueHolding(view(5), stone);
    for (int depth = 1; depth <= 5; depth++) {
      List<SlottedStack> read = Findings.value(view(depth).read(value));
      assertThat(read).containsExactly(new SlottedStack(0, stone));
    }
  }
}
