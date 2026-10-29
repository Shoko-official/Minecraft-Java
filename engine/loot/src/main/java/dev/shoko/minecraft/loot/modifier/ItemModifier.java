package dev.shoko.minecraft.loot.modifier;

import dev.shoko.minecraft.loot.predicate.LootCondition;
import java.util.List;

/**
 * One item modifier: the action and the conditions every modifier accepts (LT-R38). When a
 * condition fails the stack passes unchanged. The conditions are evaluated in order, before the
 * action, with the context of the draw.
 *
 * @param conditions the conditions in file order, unmodifiable; empty when the key is absent
 * @param action what the modifier does
 */
public record ItemModifier(List<LootCondition> conditions, ModifierAction action) {}
