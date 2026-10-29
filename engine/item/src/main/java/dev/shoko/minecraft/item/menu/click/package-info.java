/**
 * Pure click algorithms on an abstract list of slots and a cursor.
 *
 * <p>Spec: {@code docs/specs/item-components-inventory.md} (IC-R40a to IC-R47b, provisional level
 * S, vectors V-I10 to V-I13). Every operation is a function from a {@link
 * dev.shoko.minecraft.item.menu.click.MenuState} and a {@link
 * dev.shoko.minecraft.item.menu.click.ClickOperation} to a {@link
 * dev.shoko.minecraft.item.menu.click.ClickResult}: the new state and the stacks thrown to the
 * ground. No world, no player, no tick; {@code game} owns the menu that holds the state. The
 * mapping from protocol click modes to operations belongs to the packets (milestone M8).
 *
 * <p>The results are immutable. No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.item.menu.click;
