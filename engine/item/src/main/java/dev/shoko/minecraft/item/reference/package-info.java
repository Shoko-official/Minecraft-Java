/**
 * References to number providers carried by item prototypes: fuel, compost and brewing fuel.
 *
 * <p>Spec: {@code docs/specs/item-recipes.md} (RC-R32 to RC-R35, decision DC1) and ADR 0004,
 * amendment 1. This module only reads the identifier of a provider from a component; {@code loot}
 * evaluates the provider with a block context, and {@code game} passes the result (duration, speed,
 * layers) to the cooking, brewing or composting logic. No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.item.reference;
