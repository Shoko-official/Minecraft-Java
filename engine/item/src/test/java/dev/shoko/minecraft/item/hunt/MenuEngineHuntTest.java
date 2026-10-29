package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.menu.MachineRules;
import dev.shoko.minecraft.item.menu.MenuLayout;
import dev.shoko.minecraft.item.menu.MenuLayouts;
import dev.shoko.minecraft.item.menu.PlacementRules;
import dev.shoko.minecraft.item.menu.SlotRole;
import dev.shoko.minecraft.item.menu.click.ClickEffect;
import dev.shoko.minecraft.item.menu.click.ClickOperation;
import dev.shoko.minecraft.item.menu.click.ClickResult;
import dev.shoko.minecraft.item.menu.click.MenuEngine;
import dev.shoko.minecraft.item.menu.click.MenuPorts;
import dev.shoko.minecraft.item.menu.click.MenuState;
import dev.shoko.minecraft.item.menu.click.MouseButton;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Random sequences of 8 operations over the 25 menus and the player window, fixed seeds. */
class MenuEngineHuntTest {

  /** Kinds of stacks; two stacks of one kind have the same content. */
  private static final List<ItemStack> KINDS = kinds();

  private static List<ItemStack> kinds() {
    List<ItemStack> kinds = new ArrayList<>();
    kinds.add(TestItems.stack("stone", 1));
    kinds.add(TestItems.stack("stone", 1, "rarity", "'epic'"));
    kinds.add(TestItems.stack("stone", 1, "max_stack_size", "16"));
    kinds.add(TestItems.stack("stone", 1, "max_stack_size", "1"));
    kinds.add(TestItems.stack("stone", 1, "max_stack_size", "99"));
    kinds.add(TestItems.stack("dirt", 1));
    kinds.add(TestItems.stack("ender_pearl", 1));
    kinds.add(TestItems.stack("bucket", 1));
    kinds.add(TestItems.stack("iron_helmet", 1));
    kinds.add(TestItems.stack("iron_chestplate", 1));
    kinds.add(TestItems.stack("iron_leggings", 1));
    kinds.add(TestItems.stack("iron_boots", 1));
    kinds.add(TestItems.stack("stone_pickaxe", 1));
    kinds.add(TestItems.stack("stone_pickaxe", 1, "damage", "5"));
    return kinds;
  }

  private static int kindOf(ItemStack stack) {
    for (int i = 0; i < KINDS.size(); i++) {
      if (KINDS.get(i).sameContent(stack)) {
        return i;
      }
    }
    return fail("a stack of no known kind: " + stack);
  }

  /** Machine inputs refuse armor pieces and the damaged pickaxe. */
  private static final MachineRules RULES =
      (menu, slot, stack) -> {
        String path = stack.type().id().path();
        return !path.startsWith("iron_") && stack.damage() == 0;
      };

  private static int[] totals(MenuState state, List<ItemStack> thrown) {
    int[] totals = new int[KINDS.size()];
    List<ItemStack> all = new ArrayList<>(state.slots());
    all.add(state.cursor());
    all.addAll(thrown);
    for (ItemStack stack : all) {
      if (!stack.isEmpty()) {
        totals[kindOf(stack)] += stack.count();
      }
    }
    return totals;
  }

  private static ItemStack random(Random random, boolean allowArmor) {
    ItemStack kind = KINDS.get(random.nextInt(KINDS.size()));
    if (!allowArmor && kind.type().id().path().startsWith("iron_")) {
      kind = KINDS.get(0);
    }
    return kind.withCount(1 + random.nextInt(kind.maxStackSize()));
  }

  private static MenuState randomState(Random random, MenuLayout layout) {
    List<ItemStack> slots = new ArrayList<>();
    for (int slot = 0; slot < layout.slotCount(); slot++) {
      ItemStack stack = ItemStack.empty();
      if (random.nextInt(3) > 0 && layout.roles().get(slot) != SlotRole.RESULT) {
        ItemStack candidate = random(random, true);
        if (PlacementRules.mayPlace(layout, slot, candidate, RULES)) {
          stack = candidate;
        }
      }
      slots.add(stack);
    }
    ItemStack cursor = random.nextInt(3) == 0 ? random(random, true) : ItemStack.empty();
    return new MenuState(slots, cursor);
  }

  private static ClickOperation operation(Random random, MenuLayout layout) {
    int size = layout.slotCount();
    int slot = random.nextInt(size + 4) - 2;
    MouseButton button = random.nextBoolean() ? MouseButton.LEFT : MouseButton.RIGHT;
    return switch (random.nextInt(9)) {
      case 0, 1 -> new ClickOperation.Pickup(slot, button);
      case 2 -> new ClickOperation.QuickMove(slot);
      case 3 -> new ClickOperation.SwapWithHotbar(slot, random.nextInt(9));
      case 4 -> new ClickOperation.Throw(slot, random.nextBoolean());
      case 5 -> new ClickOperation.ClickOutside(button);
      case 6 -> new ClickOperation.DoubleClick(slot);
      case 7 -> new ClickOperation.SwapWithOffhand(slot);
      default -> {
        List<Integer> slots = new ArrayList<>();
        int n = 1 + random.nextInt(8);
        for (int i = 0; i < n; i++) {
          slots.add(random.nextInt(size + 2) - 1);
        }
        yield new ClickOperation.Drag(button, slots);
      }
    };
  }

  private static List<MenuLayout> layouts() {
    List<MenuLayout> layouts = new ArrayList<>();
    for (Identifier type : MenuLayouts.menuTypes()) {
      layouts.add(MenuLayouts.of(type).orElseThrow());
    }
    layouts.add(MenuLayouts.playerWindow());
    return layouts;
  }

  @Test
  @Timeout(120)
  void everyMenuKeepsUnitsLimitsAndPlacementRulesOverRandomSequences() {
    int applied = 0;
    for (MenuLayout layout : layouts()) {
      MenuEngine engine =
          MenuEngine.of(layout, new MenuPorts(RULES, (state, slot, taken) -> state));
      for (long seed = 1; seed <= 500; seed++) {
        Random random = new Random(seed * 31L + layout.slotCount());
        MenuState state = randomState(random, layout);
        int[] initial = totals(state, List.of());
        List<ItemStack> thrown = new ArrayList<>();
        List<String> history = new ArrayList<>();
        for (int step = 0; step < 8; step++) {
          ClickOperation op = operation(random, layout);
          history.add(op.toString());
          String where = layout.menuType() + " seed " + seed + " " + history;
          ClickResult result = engine.click(state, op);
          assertThat(engine.click(state, op)).as(where).isEqualTo(result);
          if (result.effect() != ClickEffect.APPLIED) {
            assertThat(result.state()).as(where).isEqualTo(state);
            assertThat(result.thrown()).as(where).isEmpty();
          }
          if (result.effect() == ClickEffect.APPLIED) {
            applied++;
          }
          thrown.addAll(result.thrown());
          assertThat(totals(result.state(), thrown)).as(where).isEqualTo(initial);
          for (int slot = 0; slot < layout.slotCount(); slot++) {
            ItemStack stack = result.state().slots().get(slot);
            String label = where + " slot " + slot + " " + layout.roles().get(slot) + " " + stack;
            assertThat(stack.count()).as(label).isLessThanOrEqualTo(stack.maxStackSize());
            assertThat(PlacementRules.mayPlace(layout, slot, stack, RULES)).as(label).isTrue();
            if (layout.roles().get(slot) == SlotRole.RESULT) {
              assertThat(stack.isEmpty()).as(label).isTrue();
            }
          }
          ItemStack cursor = result.state().cursor();
          assertThat(cursor.count())
              .as(where + " cursor")
              .isLessThanOrEqualTo(cursor.maxStackSize());
          state = result.state();
        }
      }
    }
    assertThat(applied).as("applied clicks").isGreaterThan(20000);
  }
}
