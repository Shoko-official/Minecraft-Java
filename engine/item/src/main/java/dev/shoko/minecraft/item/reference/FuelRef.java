package dev.shoko.minecraft.item.reference;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * The {@code cooking_fuel} component of an item (RC-R32).
 *
 * @param burnTime the integer provider that gives the burn time in ticks, such as {@code
 *     minecraft:cooking/time_wood_blocks}
 * @param speedMultiplier the float provider that gives the cooking speed, such as {@code
 *     minecraft:cooking/speed_default}
 */
public record FuelRef(Identifier burnTime, Identifier speedMultiplier) {}
