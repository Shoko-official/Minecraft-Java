package dev.shoko.minecraft.item.reference;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * The {@code brewing_fuel} component of an item (RC-R35).
 *
 * @param speedMultiplier the float provider that gives the brewing speed, such as {@code
 *     minecraft:brewing/speed_default}
 * @param uses the integer provider that gives the uses of one fuel item, such as {@code
 *     minecraft:brewing/uses_default}
 */
public record BrewingFuelRef(Identifier speedMultiplier, Identifier uses) {}
