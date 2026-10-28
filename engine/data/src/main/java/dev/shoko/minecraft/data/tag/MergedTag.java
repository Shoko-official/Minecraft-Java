package dev.shoko.minecraft.data.tag;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The entries of one tag once the layers of all packs are merged (R34). */
final class MergedTag {

  /** An entry with the file it comes from and its position in that file. */
  record Item(TagEntry entry, TagSource source, int index) {}

  private final List<Item> items;

  private MergedTag(List<Item> items) {
    this.items = items;
  }

  List<Item> items() {
    return items;
  }

  /**
   * Merges the layers of every tag that has at least one layer, in code point order of the tag
   * identifiers.
   */
  static Map<Identifier, MergedTag> mergeAll(Map<Identifier, List<TagSource>> layers) {
    Map<Identifier, MergedTag> merged = new TreeMap<>();
    for (Map.Entry<Identifier, List<TagSource>> tag : layers.entrySet()) {
      if (!tag.getValue().isEmpty()) {
        merged.put(tag.getKey(), merge(tag.getValue()));
      }
    }
    return merged;
  }

  private static MergedTag merge(List<TagSource> layers) {
    List<Item> items = new ArrayList<>();
    for (TagSource source : layers) {
      if (source.file().replace()) {
        items.clear();
      }
      List<TagEntry> entries = source.file().entries();
      for (int i = 0; i < entries.size(); i++) {
        items.add(new Item(entries.get(i), source, i));
      }
    }
    return new MergedTag(items);
  }
}
