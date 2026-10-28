package dev.shoko.minecraft.data.registry;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.List;
import java.util.Optional;

/** The access that knows nothing. */
enum EmptyAccess implements RegistryAccess {
  INSTANCE;

  @Override
  public boolean contains(RegistryKey<?> registry, Identifier id) {
    return false;
  }

  @Override
  public <T> Optional<Registry<T>> registry(RegistryKey<T> key) {
    return Optional.empty();
  }

  @Override
  public Optional<List<Identifier>> tagMembers(TagKey<?> tag) {
    return Optional.empty();
  }
}
