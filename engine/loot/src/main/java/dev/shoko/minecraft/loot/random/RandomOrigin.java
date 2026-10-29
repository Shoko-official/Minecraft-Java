package dev.shoko.minecraft.loot.random;

/** Where the random source of a draw comes from, by decreasing priority (LT-R63). */
public enum RandomOrigin {
  /** A non-zero explicit seed: a 48-bit generator initialized with it (command, LootTableSeed). */
  EXPLICIT_SEED,
  /** The named sequence of the table ({@code random_sequence}), shared and persistent (LT-R65). */
  NAMED_SEQUENCE,
  /** The source of the level, for a seed of 0 and a table without sequence. */
  LEVEL
}
