package dev.shoko.minecraft.item.testing;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Registry;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A hand-made {@link RegistryAccess}: the fake item registry and a few item tags. */
public final class TestAccess implements RegistryAccess {

  private final ItemCatalog items;
  private final Map<Identifier, List<Identifier>> itemTags;

  private TestAccess(ItemCatalog items, Map<Identifier, List<Identifier>> itemTags) {
    this.items = items;
    this.itemTags = itemTags;
  }

  /** The fake items with the tags used by the recipe tests. */
  public static TestAccess standard() {
    return new TestAccess(TestItems.catalog(), new HashMap<>())
        .withTag("planks", "oak_planks", "birch_planks")
        .withTag("logs", "oak_log")
        .withTag("netherite_tool_materials", "netherite_ingot")
        .withTag("trim_materials", "emerald", "iron_ingot")
        .withTag("trimmable_armor", "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots")
        .withTag("empty_tag");
  }

  /** The same access with one more item tag (no member gives an empty but loaded tag). */
  public TestAccess withTag(String tag, String... members) {
    Map<Identifier, List<Identifier>> copy = new HashMap<>(itemTags);
    List<Identifier> ids = new ArrayList<>();
    for (String member : members) {
      ids.add(Identifier.minecraft(member));
    }
    copy.put(Identifier.minecraft(tag), List.copyOf(ids));
    return new TestAccess(items, copy);
  }

  @Override
  public boolean contains(RegistryKey<?> registry, Identifier id) {
    return registry.equals(ItemCatalog.REGISTRY) && items.find(id).isPresent();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<Registry<T>> registry(RegistryKey<T> key) {
    return key.equals(ItemCatalog.REGISTRY)
        ? Optional.of((Registry<T>) items.registry())
        : Optional.empty();
  }

  @Override
  public Optional<List<Identifier>> tagMembers(TagKey<?> tag) {
    if (!tag.registry().equals(ItemCatalog.REGISTRY)) {
      return Optional.empty();
    }
    return Optional.ofNullable(itemTags.get(tag.id()));
  }
}
