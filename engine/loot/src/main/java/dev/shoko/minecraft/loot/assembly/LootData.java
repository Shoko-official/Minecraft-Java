package dev.shoko.minecraft.loot.assembly;

import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.item.recipe.RecipeSet;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import dev.shoko.minecraft.loot.enchant.EnchantmentCatalog;
import dev.shoko.minecraft.loot.number.NumberProviderSet;
import dev.shoko.minecraft.loot.predicate.NamedPredicates;
import dev.shoko.minecraft.loot.table.LootTables;

/**
 * Everything the module reads that comes from the datapacks and the reference data, decoded once
 * and shared by every evaluation. Immutable.
 *
 * @param tables the loot tables
 * @param predicates the named predicates
 * @param numbers the named number providers
 * @param enchantments the enchantment definitions and providers
 * @param items the item catalog, for the item entries and the item tags
 * @param recipes the recipes, for {@code furnace_smelt}
 * @param registries the loaded registries and tags
 */
public record LootData(
    LootTables tables,
    NamedPredicates predicates,
    NumberProviderSet numbers,
    EnchantmentCatalog enchantments,
    ItemCatalog items,
    RecipeSet recipes,
    RegistryAccess registries) {}
