package dev.shoko.minecraft.item.registry;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;

/**
 * One item of the extracted reports.
 *
 * @param id the item identifier
 * @param components the {@code components} object of {@code
 *     generated/reports/minecraft/components/item/<id>.json}, unchecked
 */
public record ItemReportEntry(Identifier id, JsonNode components) {}
