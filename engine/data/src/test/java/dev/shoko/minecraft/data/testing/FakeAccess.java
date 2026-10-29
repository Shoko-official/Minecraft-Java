package dev.shoko.minecraft.data.testing;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.Registry;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import dev.shoko.minecraft.data.registry.RegistryKey;
import dev.shoko.minecraft.data.registry.TagKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A hand-made {@link RegistryAccess}: the universe of each registry, the members of each tag and,
 * optionally, typed registries. It counts the calls, to check that lookups are lazy.
 */
public final class FakeAccess implements RegistryAccess {

  private final Map<Identifier, Set<Identifier>> universes = new HashMap<>();
  private final Map<TagKey<?>, List<Identifier>> tags = new HashMap<>();
  private final Map<Identifier, Registry<?>> typed = new HashMap<>();
  private final AtomicInteger lookups = new AtomicInteger();

  public static FakeAccess create() {
    return new FakeAccess();
  }

  /** Declares the entries of a registry, each written like a JSON value (R22). */
  public FakeAccess entries(RegistryKey<?> registry, String... ids) {
    Set<Identifier> set = universes.computeIfAbsent(registry.name(), k -> new HashSet<>());
    for (String id : ids) {
      set.add(Identifier.parse(id));
    }
    return this;
  }

  /** Declares a tag that exists and resolves to these members, in this order. */
  public FakeAccess tag(RegistryKey<?> registry, String tag, String... members) {
    List<Identifier> ids = new ArrayList<>();
    for (String member : members) {
      ids.add(Identifier.parse(member));
    }
    tags.put(new TagKey<>(registry, Identifier.parse(tag)), ids);
    return this;
  }

  public <T> FakeAccess typedRegistry(Registry<T> registry) {
    typed.put(registry.key().name(), registry);
    return this;
  }

  /** Number of calls received since creation, all methods together. */
  public int lookups() {
    return lookups.get();
  }

  @Override
  public boolean contains(RegistryKey<?> registry, Identifier id) {
    lookups.incrementAndGet();
    return universes.getOrDefault(registry.name(), Set.of()).contains(id);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<Registry<T>> registry(RegistryKey<T> key) {
    lookups.incrementAndGet();
    return Optional.ofNullable((Registry<T>) typed.get(key.name()));
  }

  @Override
  public Optional<List<Identifier>> tagMembers(TagKey<?> tag) {
    lookups.incrementAndGet();
    return Optional.ofNullable(tags.get(tag)).map(List::copyOf);
  }
}
