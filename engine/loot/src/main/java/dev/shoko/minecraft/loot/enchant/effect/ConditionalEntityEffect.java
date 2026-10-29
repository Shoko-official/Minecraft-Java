package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.List;

/**
 * An entry of a list-shaped effect component with an entity effect (EN-R12 form a, EN-R13, EN-R14).
 *
 * @param effect the entity effect
 * @param requirements the conditions, all must hold (EN-R15); empty: always
 * @param enchanted who must carry the enchantment
 * @param affected who suffers the effect
 */
public record ConditionalEntityEffect(
    EntityEffect effect,
    List<LootCondition> requirements,
    EnchantedTarget enchanted,
    AffectedTarget affected) {}
