package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.diag.Diagnostic;
import java.io.Serial;

/**
 * Thrown by {@link ObjectReader} when a field is invalid, to leave the function given to {@link
 * Codecs#object(java.util.function.Function)}. That method catches it and turns it into the
 * diagnostics of the outcome; it never reaches the caller of {@link Codec#decode}.
 */
public final class DecodeException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param diagnostic what is wrong, with the JSON path
   */
  public DecodeException(Diagnostic diagnostic) {
    super("decoding failed");
    Skeleton.throwNotImplemented();
  }

  /**
   * Returns the problem.
   *
   * @return the diagnostic
   */
  public Diagnostic diagnostic() {
    return Skeleton.throwNotImplemented();
  }
}
