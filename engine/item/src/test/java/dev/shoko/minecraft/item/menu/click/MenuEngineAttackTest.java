package dev.shoko.minecraft.item.menu.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import dev.shoko.minecraft.item.menu.MachineRules;
import dev.shoko.minecraft.item.menu.MenuLayout;
import dev.shoko.minecraft.item.menu.MenuLayouts;
import dev.shoko.minecraft.item.menu.SlotRole;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.testing.Cells;
import dev.shoko.minecraft.item.testing.Menus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: item-components-inventory IC-R38 to IC-R47 (adversarial, level S: provisional). Fixed
// seeds, random states and click sequences; the invariants are written from the rules, not from
// an implementation: units are conserved, no stack exceeds its stack size, no slot holds a stack
// it refuses, a click that does not apply changes nothing, and nothing else throws.
class MenuEngineAttackTest {

  private static final List<String> POOL =
      List.of("stone", "dirt", "ender_pearl", "stone_pickaxe", "bread", "bucket");

  private static final int STEPS = 250;

  /** Furnace inputs accept stone and dirt only. */
  private static final MachineRules STONE_AND_DIRT =
      (menu, slot, stack) -> {
        String path = stack.type().id().path();
        return path.equals("stone") || path.equals("dirt");
      };

  private record Scenario(String name, MenuLayout layout, MachineRules rules) {}

  private static List<Scenario> scenarios() {
    return List.of(
        new Scenario("chest", Menus.layout("generic_9x3"), Menus.ACCEPT_ALL),
        new Scenario("player", MenuLayouts.playerWindow(), Menus.ACCEPT_ALL),
        new Scenario("furnace", Menus.layout("furnace"), STONE_AND_DIRT),
        new Scenario("crafting", Menus.layout("crafting"), Menus.ACCEPT_ALL));
  }

  private static ItemStack randomStack(Random random, List<String> pool) {
    String path = pool.get(random.nextInt(pool.size()));
    int max = Cells.stack(path).maxStackSize();
    return Cells.stack(path + ":" + (1 + random.nextInt(max)));
  }

  private static MenuState randomState(Random random, Scenario scenario) {
    List<ItemStack> slots = new ArrayList<>();
    for (SlotRole role : scenario.layout().roles()) {
      boolean filled = random.nextBoolean();
      ItemStack stack =
          switch (role) {
            case ARMOR_HEAD, ARMOR_CHEST, ARMOR_LEGS, ARMOR_FEET -> ItemStack.empty();
            case INPUT ->
                filled ? randomStack(random, List.of("stone", "dirt")) : ItemStack.empty();
            case RESULT ->
                random.nextInt(3) == 0
                    ? Cells.stack("stone:" + (1 + random.nextInt(9)))
                    : ItemStack.empty();
            default -> filled ? randomStack(random, POOL) : ItemStack.empty();
          };
      slots.add(stack);
    }
    ItemStack cursor = random.nextInt(3) == 0 ? randomStack(random, POOL) : ItemStack.empty();
    return new MenuState(slots, cursor);
  }

  private static List<Integer> distinctSlots(Random random, int size, int count) {
    Set<Integer> chosen = new HashSet<>();
    List<Integer> ordered = new ArrayList<>();
    while (ordered.size() < count) {
      int slot = random.nextInt(size);
      if (chosen.add(slot)) {
        ordered.add(slot);
      }
    }
    return ordered;
  }

  private static ClickOperation randomOperation(Random random, MenuLayout layout, boolean middle) {
    int size = layout.slotCount();
    int slot = random.nextInt(size + 4) - 2;
    MouseButton button = random.nextBoolean() ? MouseButton.LEFT : MouseButton.RIGHT;
    boolean offhand = !layout.ranges(SlotRole.OFFHAND).isEmpty();
    return switch (random.nextInt(offhand ? 9 : 8)) {
      case 0, 1 -> new ClickOperation.Pickup(slot, button);
      case 2 -> new ClickOperation.QuickMove(slot);
      case 3 -> new ClickOperation.SwapWithHotbar(slot, random.nextInt(9));
      case 4 -> new ClickOperation.Throw(slot, random.nextBoolean());
      case 5 -> new ClickOperation.ClickOutside(button);
      case 6 -> new ClickOperation.DoubleClick(slot);
      case 7 ->
          new ClickOperation.Drag(
              middle && random.nextBoolean() ? MouseButton.MIDDLE : button,
              distinctSlots(random, size, 1 + random.nextInt(5)));
      default -> new ClickOperation.SwapWithOffhand(slot);
    };
  }

  private static void checkStacks(Scenario scenario, MenuState state, String where) {
    List<SlotRole> roles = scenario.layout().roles();
    assertThat(state.slots()).as(where).hasSameSizeAs(roles);
    for (int slot = 0; slot < roles.size(); slot++) {
      ItemStack stack = state.slots().get(slot);
      String label = where + " slot " + slot + " " + roles.get(slot) + " " + Cells.text(stack);
      assertThat(stack.count()).as(label).isLessThanOrEqualTo(stack.maxStackSize());
      if (stack.isEmpty()) {
        continue;
      }
      switch (roles.get(slot)) {
        case ARMOR_HEAD, ARMOR_CHEST, ARMOR_LEGS, ARMOR_FEET -> fail(label);
        case INPUT ->
            assertThat(scenario.rules().acceptsInput(scenario.layout().menuType(), slot, stack))
                .as(label)
                .isTrue();
        default -> {}
      }
    }
    ItemStack cursor = state.cursor();
    assertThat(cursor.count()).as(where + " cursor").isLessThanOrEqualTo(cursor.maxStackSize());
  }

  private static void checkResultSlots(Scenario scenario, MenuState before, MenuState after) {
    List<SlotRole> roles = scenario.layout().roles();
    for (int slot = 0; slot < roles.size(); slot++) {
      if (roles.get(slot) == SlotRole.RESULT) {
        ItemStack now = after.slots().get(slot);
        assertThat(now.isEmpty() || now.equals(before.slots().get(slot)))
            .as(
                "result slot %d: %s then %s",
                slot, Cells.text(before.slots().get(slot)), Cells.text(now))
            .isTrue();
      }
    }
  }

  private static void runSequence(Scenario scenario, Random random, boolean middle) {
    MenuEngine engine = Menus.engine(scenario.layout(), scenario.rules());
    MenuState state = randomState(random, scenario);
    Map<String, Integer> initial = Menus.totals(state);
    List<ItemStack> thrown = new ArrayList<>();
    for (int step = 0; step < STEPS; step++) {
      ClickOperation operation = randomOperation(random, scenario.layout(), middle);
      String where = scenario.name() + " step " + step + " " + operation;
      ClickResult result = engine.click(state, operation);
      assertThat(engine.click(state, operation)).as(where).isEqualTo(result);
      checkStacks(scenario, result.state(), where);
      checkResultSlots(scenario, state, result.state());
      if (result.effect() != ClickEffect.APPLIED) {
        assertThat(result.state()).as(where).isEqualTo(state);
        assertThat(result.thrown()).as(where).isEmpty();
      }
      boolean mayThrow =
          operation instanceof ClickOperation.Throw
              || operation instanceof ClickOperation.ClickOutside;
      if (!mayThrow) {
        assertThat(result.thrown()).as(where).isEmpty();
      }
      thrown.addAll(result.thrown());
      if (!middle) {
        assertThat(Menus.totals(result.state(), thrown)).as(where).isEqualTo(initial);
      }
      state = result.state();
    }
  }

  @Test
  @Timeout(60)
  void provisionalRandomClickSequencesKeepTheInvariantsOfTheRules() {
    // spec: IC-R38a, IC-R38b, IC-R40a to IC-R47b
    for (Scenario scenario : scenarios()) {
      for (long seed = 1; seed <= 8; seed++) {
        runSequence(scenario, new Random(seed * 7919L + scenario.name().length()), false);
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalMiddleDragsKeepTheStackSizeAndSlotInvariants() {
    // spec: IC-R45c (creative: units are created, so only the other invariants are checked)
    for (Scenario scenario : scenarios()) {
      for (long seed = 100; seed <= 103; seed++) {
        runSequence(scenario, new Random(seed * 104729L + scenario.name().length()), true);
      }
    }
  }

  @Test
  @Timeout(60)
  void provisionalAFreshStateIsAlwaysAcceptedByTheEngineOfItsLayout() {
    // spec: IC-R37a (the state has exactly the slots of the layout)
    for (Scenario scenario : scenarios()) {
      MenuEngine engine = Menus.engine(scenario.layout(), scenario.rules());
      Random random = new Random(42L + scenario.name().length());
      for (int i = 0; i < 50; i++) {
        MenuState state = randomState(random, scenario);
        ClickResult result =
            engine.click(
                state,
                new ClickOperation.Pickup(random.nextInt(state.slots().size()), MouseButton.LEFT));
        assertThat(result.state().slots()).hasSameSizeAs(state.slots());
      }
    }
  }
}
