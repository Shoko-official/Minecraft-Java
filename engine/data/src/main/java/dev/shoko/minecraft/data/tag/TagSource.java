package dev.shoko.minecraft.data.tag;

import dev.shoko.minecraft.data.diag.FileOrigin;

/**
 * A tag file with the place it was read from, one layer of the merge of R34.
 *
 * @param origin the file, used by diagnostics
 * @param file the content
 */
public record TagSource(FileOrigin origin, TagFile file) {}
