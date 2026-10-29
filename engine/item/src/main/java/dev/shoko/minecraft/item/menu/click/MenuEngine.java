package dev.shoko.minecraft.item.menu.click;

import dev.shoko.minecraft.item.menu.MenuLayout;

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

  private MenuEngine() {}

  /**
   * Creates an engine for a menu type.
   *
   * @param layout the slots of the menu
   * @param ports the services of {@code game}
   * @return the engine
   */
  public static MenuEngine of(MenuLayout layout, MenuPorts ports) {
    return Skeleton.throwNotImplemented();
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
    return Skeleton.throwNotImplemented();
  }
}
