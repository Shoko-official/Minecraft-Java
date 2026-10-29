/**
 * The data-driven effects of enchantments: level-based values, value effects, entity and location
 * effects, the 31 effect components, and their evaluation over an item or an entity.
 *
 * <p>Spec: {@code docs/specs/item-enchantments.md} (EN-R11 to EN-R34, probes PE5, PE8, PE10, PE11).
 * Values are 32-bit floats (EN-R21). The execution of the effects of entities and positions
 * (damage, explosions, summons) is out of scope; they are evaluated into {@link
 * dev.shoko.minecraft.loot.port.EffectIntent}s sent to the {@link
 * dev.shoko.minecraft.loot.port.ActionSink}.
 */
package dev.shoko.minecraft.loot.enchant.effect;
