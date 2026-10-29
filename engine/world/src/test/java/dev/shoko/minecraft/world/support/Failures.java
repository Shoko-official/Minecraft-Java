package dev.shoko.minecraft.world.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Assertions on thrown failures that never mistake an unimplemented method for the expected one.
 */
public final class Failures {

  private Failures() {}

  /**
   * Runs the call and returns what it throws, which must be an instance of {@code type}. An {@link
   * UnsupportedOperationException} is rethrown as is, unless it is the expected type, so that a
   * test of an unimplemented method reports it directly instead of a failed type assertion.
   */
  public static <T extends Throwable> T expect(Class<T> type, ThrowingCallable call) {
    Throwable thrown = catchThrowable(call);
    if (thrown instanceof UnsupportedOperationException unsupported
        && !type.isAssignableFrom(UnsupportedOperationException.class)) {
      throw unsupported;
    }
    assertThat(thrown).as("expected a " + type.getSimpleName()).isInstanceOf(type);
    return type.cast(thrown);
  }
}
