package dev.shoko.minecraft.data.tag;

import java.util.List;

/**
 * The content of one tag file (R32).
 *
 * @param replace whether the entries replace those accumulated from packs of lower priority (R34)
 * @param entries the entries in file order; may be empty, which is a valid empty tag (R38)
 */
public record TagFile(boolean replace, List<TagEntry> entries) {}
