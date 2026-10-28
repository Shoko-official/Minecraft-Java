package dev.shoko.minecraft.data.testing;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.tag.TagEntry;
import dev.shoko.minecraft.data.tag.TagFile;
import dev.shoko.minecraft.data.tag.TagResolution;
import dev.shoko.minecraft.data.tag.TagResolver;
import dev.shoko.minecraft.data.tag.TagSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;

/**
 * Builds the input of {@link TagResolver}: tag files written in a short notation, and the layers of
 * each tag in pack priority order.
 *
 * <p>An entry is {@code a} (an element), {@code #u} (a tag), and may end with {@code ?} to be
 * optional ({@code required} false). Identifiers are read with the default namespace.
 */
public final class TagLayers {

  private final SequencedMap<Identifier, List<TagSource>> layers = new LinkedHashMap<>();

  public static TagLayers create() {
    return new TagLayers();
  }

  public static RegistryKey<String> registry() {
    return new RegistryKey<>(Identifier.minecraft("block"));
  }

  public static TagEntry entry(String text) {
    boolean optional = text.endsWith("?");
    String body = optional ? text.substring(0, text.length() - 1) : text;
    boolean tag = body.startsWith("#");
    return new TagEntry(Identifier.parse(tag ? body.substring(1) : body), tag, !optional);
  }

  public static TagFile file(boolean replace, String... entries) {
    List<TagEntry> list = new ArrayList<>();
    for (String entry : entries) {
      list.add(entry(entry));
    }
    return new TagFile(replace, list);
  }

  /** The file a pack holds for a tag, as the loader would name it. */
  public static FileOrigin origin(String pack, String tag) {
    return new FileOrigin(pack, "data/minecraft/tags/block/" + tag + ".json");
  }

  /** Adds a layer (a file of a pack) on top of the layers already added for this tag. */
  public TagLayers add(String tag, String pack, TagFile file) {
    layers
        .computeIfAbsent(Identifier.parse(tag), k -> new ArrayList<>())
        .add(new TagSource(origin(pack, tag), file));
    return this;
  }

  /** Adds a layer without {@code replace}. */
  public TagLayers add(String tag, String pack, String... entries) {
    return add(tag, pack, file(false, entries));
  }

  public TagLayers replace(String tag, String pack, String... entries) {
    return add(tag, pack, file(true, entries));
  }

  public SequencedMap<Identifier, List<TagSource>> layers() {
    return layers;
  }

  public static Set<Identifier> universe(String... elements) {
    Set<Identifier> set = new HashSet<>();
    for (String element : elements) {
      set.add(Identifier.parse(element));
    }
    return set;
  }

  public Outcome<TagResolution> resolve(String... universe) {
    return TagResolver.resolve(registry(), layers, universe(universe));
  }
}
