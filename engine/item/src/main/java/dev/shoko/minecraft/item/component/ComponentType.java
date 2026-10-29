package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * One entry of the {@code data_component_type} registry (122 entries, IC-R9).
 *
 * @param id the identifier, such as {@code minecraft:max_stack_size}
 * @param rawId the position in the registry, from 0
 */
public record ComponentType(Identifier id, int rawId) {}
