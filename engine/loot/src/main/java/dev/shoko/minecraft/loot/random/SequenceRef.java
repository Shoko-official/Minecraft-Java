package dev.shoko.minecraft.loot.random;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * The {@code random_sequence} of a table (LT-R2, LT-R64): all 1447 vanilla tables name one, a table
 * of a datapack may not.
 */
public sealed interface SequenceRef permits SequenceRef.Unnamed, SequenceRef.Named {

  /** The table has no {@code random_sequence}: it falls back to the source of the level. */
  record Unnamed() implements SequenceRef {}

  /**
   * The table names a sequence.
   *
   * @param id the sequence identifier, such as {@code minecraft:blocks/stone}
   */
  record Named(Identifier id) implements SequenceRef {}
}
