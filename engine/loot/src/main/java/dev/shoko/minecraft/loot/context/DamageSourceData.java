package dev.shoko.minecraft.loot.context;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * The damage source parameter (LT-R12, LT-R60). The source and direct entities that the predicate
 * keys {@code source_entity} and {@code direct_entity} look at are the {@code attacker} and {@code
 * direct_attacker} parameters of the context (assumption, see the report of the skeleton); the tags
 * of the damage type are looked up through the registries.
 *
 * @param damageType the damage type, such as {@code minecraft:player_attack}
 * @param direct whether the damage is direct, the {@code is_direct} key
 */
public record DamageSourceData(Identifier damageType, boolean direct) {}
