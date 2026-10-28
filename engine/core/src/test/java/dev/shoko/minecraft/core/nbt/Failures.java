package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Assertions on {@link NbtException} failures: the cause and the offset, not only the type. */
public final class Failures {

  private Failures() {}

  /**
   * Runs the call and returns the {@link NbtException} it throws. Any other runtime failure, such
   * as the {@link UnsupportedOperationException} of an unimplemented method, is rethrown as is so
   * that the test reports it directly.
   */
  public static NbtException failure(ThrowingCallable call) {
    Throwable thrown = catchThrowable(call);
    if (thrown instanceof RuntimeException unexpected) {
      throw unexpected;
    }
    assertThat(thrown).as("expected an NbtException").isInstanceOf(NbtException.class);
    return (NbtException) thrown;
  }

  /** Asserts that the call fails with this reason at this offset. */
  public static void assertFails(ThrowingCallable call, Reason reason, long offset) {
    NbtException failure = failure(call);
    assertThat(failure.reason()).isEqualTo(reason);
    assertThat(failure.offset()).hasValue(offset);
  }

  /** Asserts that the call fails with this reason, whatever the offset. */
  public static void assertFails(ThrowingCallable call, Reason reason) {
    assertThat(failure(call).reason()).isEqualTo(reason);
  }

  /** Asserts that the call fails with this reason and no offset. */
  public static void assertFailsWithoutOffset(ThrowingCallable call, Reason reason) {
    NbtException failure = failure(call);
    assertThat(failure.reason()).isEqualTo(reason);
    assertThat(failure.offset()).isEmpty();
  }
}
