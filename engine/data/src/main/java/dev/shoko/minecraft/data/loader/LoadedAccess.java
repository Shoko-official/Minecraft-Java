package dev.shoko.minecraft.data.loader;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Registry;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import java.util.List;
import java.util.Optional;

/** Answers existence questions from the identifiers and tags of a load (K3, K4). */
final class LoadedAccess implements RegistryAccess {

  private final LoadedData data;

  LoadedAccess(LoadedData data) {
    this.data = data;
  }

  @Override
  public boolean contains(RegistryKey<?> registry, Identifier id) {
    return data.hasEntry(registry.name(), id);
  }

  @Override
  public <T> Optional<Registry<T>> registry(RegistryKey<T> key) {
    return Optional.empty();
  }

  @Override
  public Optional<List<Identifier>> tagMembers(TagKey<?> tag) {
    return data.tags(tag.registry().name()).flatMap(tags -> tags.members(tag.id()));
  }
}
