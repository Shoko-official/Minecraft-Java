package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.menu.MenuLayout;
import java.util.List;

/**
 * Applies clicks to the state of one menu type (IC-R40a to IC-R47b). Stateless: the state is an
 * argument and a result. The general quick-move rules (IC-R42a to IC-R42d) apply to every menu; the
 * per-menu tables of IC-R42e are not specified yet and are not applied.
 *
 * <p>The capacity of a slot is the stack size of the content (IC-R30); the result slots accept no
 * deposit and the machine inputs follow {@link MenuPorts#machine()}, both through {@code
 * PlacementRules}. An operation that would place a stack in a slot that refuses it leaves that slot
 * as it was.
 */
public final class MenuEngine {

  private final MenuLayout layout;
  private final MenuPorts ports;

  private MenuEngine(MenuLayout layout, MenuPorts ports) {
    this.layout = layout;
    this.ports = ports;
  }

  /**
   * Creates an engine for a menu type.
   *
   * @param layout the slots of the menu
   * @param ports the services of {@code game}
   * @return the engine
   */
  public static MenuEngine of(MenuLayout layout, MenuPorts ports) {
    return new MenuEngine(layout, ports);
  }

  /**
   * Applies one operation.
   *
   * @param state the state before, with exactly as many slots as the layout
   * @param operation the operation
   * @return the state after, the stacks thrown and the effect
   * @throws IllegalArgumentException when the state does not have the slots of the layout
   */
  public ClickResult click(MenuState state, ClickOperation operation) {
    if (state.slots().size() != layout.slotCount()) {
      throw new IllegalArgumentException(
          "the state has " + state.slots().size() + " slots, the layout " + layout.slotCount());
    }
    Workspace ws = new Workspace(layout, ports, state);
    ClickEffect effect = apply(ws, operation);
    if (effect != ClickEffect.APPLIED) {
      return new ClickResult(state, List.of(), effect);
    }
    return new ClickResult(ws.toState(), ws.thrown(), ClickEffect.APPLIED);
  }

  private static ClickEffect apply(Workspace ws, ClickOperation operation) {
    return switch (operation) {
      case ClickOperation.Pickup op -> PickupClick.run(ws, op.slot(), op.button());
      case ClickOperation.QuickMove op -> QuickMove.run(ws, op.slot());
      case ClickOperation.SwapWithHotbar op ->
          SwapClick.withHotbar(ws, op.slot(), op.hotbarIndex());
      case ClickOperation.SwapWithOffhand op -> SwapClick.withOffhand(ws, op.slot());
      case ClickOperation.Throw op -> ThrowClick.fromSlot(ws, op.slot(), op.wholeStack());
      case ClickOperation.ClickOutside op -> ThrowClick.fromCursor(ws, op.button());
      case ClickOperation.Drag op -> DragClick.run(ws, op.button(), op.slots());
      case ClickOperation.DoubleClick op -> GatherClick.run(ws, op.slot());
    };
  }
}
