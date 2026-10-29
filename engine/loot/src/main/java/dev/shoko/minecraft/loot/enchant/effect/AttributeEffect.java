package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * An attribute modifier granted while the enchantment is active (EN-R12 form c, EN-R27, EN-R33).
 *
 * @param id the modifier identifier, unique per enchantment, such as {@code
 *     minecraft:enchantment.efficiency}
 * @param attribute the attribute identifier
 * @param operation the operation as read, such as {@code add_value} or {@code add_multiplied_total}
 * @param amount the amount by level
 */
public record AttributeEffect(
    Identifier id, Identifier attribute, String operation, LevelBasedValue amount) {}
