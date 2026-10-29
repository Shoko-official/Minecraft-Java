/**
 * Slot arrays, the NBT layout of block entities that hold items, and slot index spaces.
 *
 * <p>Spec: {@code docs/specs/item-components-inventory.md} (IC-R23 to IC-R36). This module owns the
 * data and the layouts; the block entities themselves, their ticks and their menus belong to {@code
 * game} (ADR 0005), which registers behaviors in {@code world} and calls these codecs. The machine
 * state (burn time, cooking progress) is data here: its evolution is not.
 *
 * <p>{@link dev.shoko.minecraft.item.container.Container} is mutable and confined to the thread
 * that ticks the world; every other type is immutable. No method accepts or returns {@code null}.
 */
package dev.shoko.minecraft.item.container;
