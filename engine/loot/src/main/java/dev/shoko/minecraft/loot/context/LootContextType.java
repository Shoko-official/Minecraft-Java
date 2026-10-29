package dev.shoko.minecraft.loot.context;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Set;

/**
 * One of the 31 entries of the {@code context_key_set} registry (LT-R13). The parameters of a set
 * are not in the data: they are the provisional hypothesis of LT-R14b (probe PL18).
 *
 * @param id the identifier, such as {@code minecraft:chest}
 * @param rawId the position in the registry, from 0 ({@code empty}) to 30 ({@code hit_block})
 * @param required the parameters the caller must provide, unmodifiable
 * @param allowed the parameters the set may carry, required ones included, unmodifiable
 */
public record LootContextType(
    Identifier id, int rawId, Set<ContextKey> required, Set<ContextKey> allowed) {}
