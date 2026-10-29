package dev.shoko.minecraft.loot;

/**
 * One problem or fallback met while decoding data or evaluating it.
 *
 * @param code the cause, a closed category
 * @param path where, as a JSON path such as {@code $.pools[0].entries[2]}; {@code $} for the root
 * @param message for humans, in English; its wording is not part of the contract
 */
public record LootFinding(LootFindingCode code, String path, String message) {}
