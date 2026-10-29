package dev.shoko.minecraft.loot.random;

import dev.shoko.minecraft.core.random.Lcg48Random;
import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.loot.port.RandomSequenceStore;

/**
 * Chooses the random source of a top-level draw (LT-R63). A nested table never calls this: it uses
 * the source of its caller (LT-R23, LT-R64).
 */
public final class LootRandom {

  private LootRandom() {}

  /**
   * Chooses the source, by decreasing priority: a non-zero explicit seed gives a new 48-bit
   * generator ({@code Lcg48Random.ofSeed}); else the named sequence of the table, taken from the
   * store; else the source of the level. A seed of 0 means "absent" (LT-R63, LT-R68). Reducing the
   * first two to one generator is wrong. Consumes no step.
   *
   * @param explicitSeed the seed of the command or of {@code LootTableSeed}, 0 when absent
   * @param sequence the {@code random_sequence} of the table
   * @param sequences the persistent named sequences
   * @param level the source of the level
   * @return the source and its origin
   */
  public static ChosenRandom choose(
      long explicitSeed, SequenceRef sequence, RandomSequenceStore sequences, RandomSource level) {
    if (explicitSeed != 0L) {
      return new ChosenRandom(Lcg48Random.ofSeed(explicitSeed), RandomOrigin.EXPLICIT_SEED);
    }
    if (sequence instanceof SequenceRef.Named named) {
      return new ChosenRandom(sequences.sequence(named.id()), RandomOrigin.NAMED_SEQUENCE);
    }
    return new ChosenRandom(level, RandomOrigin.LEVEL);
  }
}
