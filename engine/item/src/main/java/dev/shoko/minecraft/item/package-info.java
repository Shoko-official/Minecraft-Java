/**
 * Items, data components, item stacks, containers, menu click algorithms and recipes.
 *
 * <p>Specs: {@code docs/specs/item-components-inventory.md} (rules {@code IC-Rn}) and {@code
 * docs/specs/item-recipes.md} (rules {@code RC-Rn}); the Javadoc cites rules with these prefixes.
 * Rules marked provisional (level S) describe the contract of the engine until an oracle probe
 * settles the behavior of the game.
 *
 * <ul>
 *   <li>{@link dev.shoko.minecraft.item.registry}: the item registry and the prototype components
 *       of each item (IC-R1 to IC-R5).
 *   <li>{@link dev.shoko.minecraft.item.component}: the 122 component types, component maps,
 *       patches and their decoding (IC-R6 to IC-R13).
 *   <li>{@link dev.shoko.minecraft.item.stack}: item stacks and their NBT, JSON and command text
 *       forms (IC-R14 to IC-R29).
 *   <li>{@link dev.shoko.minecraft.item.container}: slot arrays, block entity inventories and slot
 *       index spaces (IC-R30 to IC-R36).
 *   <li>{@link dev.shoko.minecraft.item.reference}: references to number providers read from the
 *       prototypes (fuel, compost, brewing fuel); the providers are evaluated by {@code loot} (ADR
 *       0004, amendment 1).
 *   <li>{@link dev.shoko.minecraft.item.menu} and {@link dev.shoko.minecraft.item.menu.click}: slot
 *       layouts of the 25 menus and the pure click algorithms (IC-R37 to IC-R47). Opening and
 *       closing menus, ticks and block entities belong to {@code game}.
 *   <li>{@link dev.shoko.minecraft.item.recipe} and its subpackages: recipe model, loading,
 *       recognition of grids and lookups of machine recipes (RC-R1 to RC-R55).
 * </ul>
 *
 * <p>Failures of data are results, never exceptions: a decoder answers with a {@link
 * dev.shoko.minecraft.item.Decoding}, whose findings carry a closed {@link
 * dev.shoko.minecraft.item.FindingCode}. Callers and tests rely on the code, never on the message.
 * Nothing reads a clock or a random source: this module has no randomness. Stacks, maps and recipes
 * are immutable; {@link dev.shoko.minecraft.item.container.Container} is the only mutable type and
 * is confined to the thread that ticks the world.
 *
 * <p>This module depends on {@code core} and {@code data} only: it knows neither blocks nor
 * entities nor loot. No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.item;
