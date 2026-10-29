package dev.shoko.minecraft.item.recipe.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Registry;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import dev.shoko.minecraft.item.registry.ItemCatalog;
import java.util.List;
import java.util.Optional;

/**
 * The access that the ingredients of a recipe keep: the tags come from the loaded data, the item
 * registry from the item catalog. The access of {@code data} resolves tags but holds no registry of
 * objects, so a tag ingredient could not turn its members into items by itself.
 */
final class ItemAccess implements RegistryAccess {

  private final RegistryAccess tags;
  private final ItemCatalog items;

  ItemAccess(RegistryAccess tags, ItemCatalog items) {
    this.tags = tags;
    this.items = items;
  }

  @Override
  public boolean contains(RegistryKey<?> registry, Identifier id) {
    if (registry.equals(ItemCatalog.REGISTRY)) {
      return items.find(id).isPresent();
    }
    return tags.contains(registry, id);
  }

  @Override
  @SuppressWarnings("unchecked") // the key of the item registry is typed by ItemType
  public <T> Optional<Registry<T>> registry(RegistryKey<T> key) {
    if (key.equals(ItemCatalog.REGISTRY)) {
      return Optional.of((Registry<T>) items.registry());
    }
    return tags.registry(key);
  }

  @Override
  public Optional<List<Identifier>> tagMembers(TagKey<?> tag) {
    return tags.tagMembers(tag);
  }
}
