package dev.shoko.minecraft.data.diag;

/**
 * The file a value or a problem comes from.
 *
 * @param pack the pack identifier, such as {@code vanilla} or {@code file/mypack} (R40), or an
 *     empty string for a file that belongs to no pack
 * @param path the path inside the pack, with {@code /} separators on every system (R24)
 */
public record FileOrigin(String pack, String path) {}
