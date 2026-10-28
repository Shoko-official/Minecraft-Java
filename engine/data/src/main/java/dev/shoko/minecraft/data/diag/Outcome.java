package dev.shoko.minecraft.data.diag;

import java.util.List;
import java.util.Optional;

/**
 * The result of an operation that keeps going after a problem: a value, when there is one, and the
 * diagnostics found on the way. An empty value means the input produced nothing (R54); a present
 * value may come with error diagnostics when part of it was dropped, such as a failed tag (R36).
 * Immutable.
 *
 * @param <T> the type of the value
 */
public final class Outcome<T> {

  private Outcome() {}

  /**
   * Returns a result with a value and no diagnostic.
   *
   * @param value the value, not {@code null}
   * @param <T> the type of the value
   * @return the result
   */
  public static <T> Outcome<T> success(T value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a result with a value and diagnostics.
   *
   * @param value the value, not {@code null}
   * @param diagnostics the diagnostics, in the order they were found
   * @param <T> the type of the value
   * @return the result; later changes to {@code diagnostics} do not affect it
   */
  public static <T> Outcome<T> success(T value, List<Diagnostic> diagnostics) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a result without value.
   *
   * @param diagnostics the diagnostics, in the order they were found
   * @param <T> the type of the value
   * @return the result; later changes to {@code diagnostics} do not affect it
   */
  public static <T> Outcome<T> failure(List<Diagnostic> diagnostics) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the value.
   *
   * @return the value, or empty when the input produced nothing
   */
  public Optional<T> value() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the diagnostics in the order they were found.
   *
   * @return an unmodifiable list
   */
  public List<Diagnostic> diagnostics() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether a diagnostic has {@link Severity#ERROR}.
   *
   * @return whether at least one diagnostic is an error
   */
  public boolean hasErrors() {
    return Skeleton.throwNotImplemented();
  }
}
