package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.core.random.XoroshiroRandom;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.loot.port.RandomSequenceStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link RandomSequenceStore} that hands out one generator per identifier, seeded from a fixed
 * value and the identifier, and remembers every request in order.
 */
public final class SequenceStore implements RandomSequenceStore {

  private final long seed;
  private final Map<Identifier, RandomSource> sources = new HashMap<>();
  private final List<Identifier> requested = new ArrayList<>();

  public SequenceStore(long seed) {
    this.seed = seed;
  }

  @Override
  public RandomSource sequence(Identifier id) {
    requested.add(id);
    return sources.computeIfAbsent(
        id, key -> XoroshiroRandom.ofSeed(seed + key.toString().hashCode()));
  }

  /** The identifiers asked for, in order of the requests. */
  public List<Identifier> requested() {
    return List.copyOf(requested);
  }

  /** The generator of an identifier without counting as a request. */
  public RandomSource peek(Identifier id) {
    RandomSource known = sources.get(id);
    if (known == null) {
      throw new IllegalStateException("sequence never requested: " + id);
    }
    return known;
  }
}
