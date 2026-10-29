package dev.shoko.minecraft.data.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Expected-exception assertions that cannot be satisfied by the {@link
 * UnsupportedOperationException} of an unimplemented method: that exception is rethrown as is, so a
 * test about a failure stays red for the right reason until the behavior exists.
 */
public final class Expect {

  private Expect() {}

  /** Runs the call and returns what it throws, rethrowing the unimplemented-method signal. */
  public static Throwable thrown(ThrowingCallable call) {
    Throwable thrown = catchThrowable(call);
    if (thrown instanceof UnsupportedOperationException unimplemented) {
      throw unimplemented;
    }
    return thrown;
  }

  public static void illegalArgument(ThrowingCallable call) {
    assertThat(thrown(call)).isInstanceOf(IllegalArgumentException.class);
  }

  public static void illegalState(ThrowingCallable call) {
    assertThat(thrown(call)).isInstanceOf(IllegalStateException.class);
  }

  /**
   * Asserts that the call fails with an {@link UnsupportedOperationException} raised by an
   * unmodifiable collection. The collection must be obtained before this call: asking the API for
   * it inside the lambda would let an unimplemented method pass for an unmodifiable result.
   */
  public static void unmodifiable(ThrowingCallable mutation) {
    Throwable thrown = catchThrowable(mutation);
    assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
  }
}
