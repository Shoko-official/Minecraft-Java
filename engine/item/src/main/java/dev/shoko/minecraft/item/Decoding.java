package dev.shoko.minecraft.item;

import java.util.List;
import java.util.function.Supplier;

/**
 * The outcome of decoding data: a value, possibly with warnings, or a failure. Unlike an exception,
 * a failure keeps every finding made before giving up, in the order they were made.
 *
 * <p>A {@link Success} with warnings is a lenient read with a provisional fallback (IC-R26: an
 * unknown item gives the empty stack). A {@link Failure} contains at least one finding of severity
 * {@code ERROR}. Every list is unmodifiable and never {@code null}.
 *
 * @param <T> what a successful decoding yields
 */
public sealed interface Decoding<T> permits Decoding.Success, Decoding.Failure {

  /**
   * Returns the findings, in the order they were made.
   *
   * @return the findings, empty when there is nothing to report
   */
  List<Finding> findings();

  /**
   * Runs a decoder that reports a refusal by throwing {@link FindingException}.
   *
   * @param <T> what a successful decoding yields
   * @param body the decoder
   * @return a success without finding, or the failure carrying the findings of the refusal
   */
  static <T> Decoding<T> attempt(Supplier<T> body) {
    try {
      return new Success<>(body.get(), List.of());
    } catch (FindingException refusal) {
      return new Failure<>(refusal.findings());
    }
  }

  /**
   * The data was decoded.
   *
   * @param <T> the type of the value
   * @param value the decoded value
   * @param findings the warnings, none of severity {@code ERROR}
   */
  record Success<T>(T value, List<Finding> findings) implements Decoding<T> {}

  /**
   * The data was refused and nothing was produced.
   *
   * @param <T> the type a success would have yielded
   * @param findings what went wrong, at least one of severity {@code ERROR}
   */
  record Failure<T>(List<Finding> findings) implements Decoding<T> {}
}
