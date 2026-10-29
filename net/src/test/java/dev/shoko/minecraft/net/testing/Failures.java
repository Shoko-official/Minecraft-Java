package dev.shoko.minecraft.net.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Assertions on typed failures. A runtime failure that is not the expected one, such as the {@link
 * UnsupportedOperationException} of an unimplemented method, is rethrown as is so that the test
 * reports it directly.
 */
public final class Failures {

  private Failures() {}

  /** Runs the call and returns the {@link ProtocolException} it throws. */
  public static ProtocolException failure(ThrowingCallable call) {
    Throwable thrown = catchThrowable(call);
    if (thrown instanceof RuntimeException unexpected) {
      throw unexpected;
    }
    assertThat(thrown).as("expected a ProtocolException").isInstanceOf(ProtocolException.class);
    return (ProtocolException) thrown;
  }

  /** Asserts that the call fails with this reason. */
  public static void assertFails(ThrowingCallable call, Reason reason) {
    assertThat(failure(call).reason()).isEqualTo(reason);
  }

  /** Asserts that the call throws an unchecked exception of this type. */
  public static void assertThrows(Class<? extends RuntimeException> type, ThrowingCallable call) {
    Throwable thrown = catchThrowable(call);
    if (thrown instanceof UnsupportedOperationException notImplemented
        && type != notImplemented.getClass()) {
      throw notImplemented;
    }
    assertThat(thrown).isInstanceOf(type);
  }
}
