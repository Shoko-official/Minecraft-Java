package dev.shoko.minecraft.loot.enchant.effect;

import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.List;

/**
 * An entry of a list-shaped effect component with a value effect (EN-R12 form a, EN-R13).
 *
 * @param effect the value effect
 * @param requirements the conditions, all must hold in the effect context (EN-R15); empty when the
 *     key is absent: always
 */
public record ConditionalValueEffect(ValueEffect effect, List<LootCondition> requirements) {}
