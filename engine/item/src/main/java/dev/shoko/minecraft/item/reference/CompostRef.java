package dev.shoko.minecraft.item.reference;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * The {@code compostable} component of an item (RC-R34).
 *
 * @param layers the integer provider that gives the layers added to the composter, such as {@code
 *     minecraft:compostable/low}
 */
public record CompostRef(Identifier layers) {}
