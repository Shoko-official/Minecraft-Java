package dev.shoko.minecraft.loot;

import java.util.List;

/**
 * The outcome of decoding data: a value, possibly with warnings, or a failure. Unlike an exception,
 * a failure keeps every finding made before giving up, in the order they were made. Same shape as
 * the decoding of {@code item}, with the causes of this module.
 *
 * <p>A {@link Success} carries no finding of severity {@code ERROR}; a {@link Failure} carries at
 * least one. Every list is unmodifiable and never {@code null}.
 *
 * @param <T> what a successful decoding yields
 */
public sealed interface LootDecoding<T> permits LootDecoding.Success, LootDecoding.Failure {

  /**
   * Returns the findings, in the order they were made.
   *
   * @return the findings, empty when there is nothing to report
   */
  List<LootFinding> findings();

  /**
   * The data was decoded.
   *
   * @param <T> the type of the value
   * @param value the decoded value
   * @param findings the warnings and observations, none of severity {@code ERROR}
   */
  record Success<T>(T value, List<LootFinding> findings) implements LootDecoding<T> {}

  /**
   * The data was refused and nothing was produced.
   *
   * @param <T> the type a success would have yielded
   * @param findings what went wrong, at least one of severity {@code ERROR}
   */
  record Failure<T>(List<LootFinding> findings) implements LootDecoding<T> {}
}
