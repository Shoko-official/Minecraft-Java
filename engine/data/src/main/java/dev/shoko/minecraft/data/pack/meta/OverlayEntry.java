package dev.shoko.minecraft.data.pack.meta;

/**
 * An overlay declared by {@code overlays.entries} (R48): a directory at the root of the pack,
 * holding a tree of the same shape as the root, applied when the target version is in range.
 *
 * @param directory the directory name, using {@code a-z 0-9 _ - .}
 * @param formats the versions for which the overlay is active, from {@code min_format} and {@code
 *     max_format}, or from {@code formats} when the entry gives it (R4)
 */
public record OverlayEntry(String directory, PackFormatRange formats) {}
