package dev.shoko.minecraft.loot.number;

/**
 * One entry of a {@code weighted_list} distribution (LT-R50): the shape read in the data is {@code
 * {"data": ..., "weight": n}}.
 *
 * @param <T> the provider type of the data
 * @param data the value of the entry
 * @param weight the weight, an integer
 */
public record WeightedEntry<T>(T data, int weight) {}
