package dev.shoko.minecraft.item;

/**
 * One problem or fallback met while decoding data.
 *
 * @param code the cause, a closed category
 * @param path where, as a JSON path such as {@code $.components[0]} or an NBT path such as {@code
 *     Items[2].count}; {@code $} for the root
 * @param message for humans, in English; its wording is not part of the contract
 */
public record Finding(FindingCode code, String path, String message) {}
