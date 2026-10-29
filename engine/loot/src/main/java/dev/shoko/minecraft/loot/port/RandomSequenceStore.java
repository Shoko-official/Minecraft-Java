package dev.shoko.minecraft.loot.port;

import dev.shoko.minecraft.core.random.RandomSource;
import dev.shoko.minecraft.data.id.Identifier;

/**
 * The persistent named random sequences of the level ({@code random_sequences.dat}, LT-R65). The
 * store is implemented by {@code game}, which owns the world seed, the salt and the persistence;
 * the initial state of a sequence is {@code RandomSequenceSettings.initialState}.
 */
public interface RandomSequenceStore {

  /**
   * Returns a sequence, creating it from the world settings on first use (LT-R65). The returned
   * source is the persistent one: drawing from it advances the saved state, and two tables that
   * share an identifier share the state.
   *
   * @param id the sequence identifier, such as {@code minecraft:chests/simple_dungeon}
   * @return the sequence
   */
  RandomSource sequence(Identifier id);
}
