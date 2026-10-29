package dev.shoko.minecraft.loot.number;

import dev.shoko.minecraft.data.id.Identifier;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * The named providers of the loaded data: the 26 elements of {@code context_int_provider} and the 4
 * of {@code context_float_provider} (LT-R50). Immutable.
 *
 * @param ints the integer providers by identifier, in identifier order, unmodifiable
 * @param floats the float providers by identifier, in identifier order, unmodifiable
 */
public record NumberProviderSet(
    SequencedMap<Identifier, IntProvider> ints, SequencedMap<Identifier, FloatProvider> floats) {

  /**
   * Finds a named integer provider.
   *
   * @param id an identifier such as {@code minecraft:cooking/time_coal}
   * @return the provider, empty when it is not loaded
   */
  public Optional<IntProvider> intProvider(Identifier id) {
    return Optional.ofNullable(ints.get(id));
  }

  /**
   * Finds a named float provider.
   *
   * @param id an identifier such as {@code minecraft:cooking/speed_default}
   * @return the provider, empty when it is not loaded
   */
  public Optional<FloatProvider> floatProvider(Identifier id) {
    return Optional.ofNullable(floats.get(id));
  }
}
