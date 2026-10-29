package dev.shoko.minecraft.net.testing;

import java.lang.management.ManagementFactory;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Measures the memory a call allocates on the calling thread. A byte count is deterministic enough
 * to bound an allocation without any clock.
 */
public final class Allocation {

  private static final com.sun.management.ThreadMXBean THREADS =
      (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

  private Allocation() {}

  /**
   * Runs the call and returns the bytes it allocated. A failure of the call is not an error here:
   * the caller checks the failure separately, only the allocation matters. An unimplemented method
   * is not hidden: its {@link UnsupportedOperationException} is rethrown.
   */
  public static long bytesAllocatedBy(ThrowingCallable call) {
    long before = THREADS.getCurrentThreadAllocatedBytes();
    try {
      call.call();
    } catch (UnsupportedOperationException notImplemented) {
      throw notImplemented;
    } catch (Throwable expected) {
      // The type of the failure is asserted by the caller.
      long after = THREADS.getCurrentThreadAllocatedBytes();
      return after - before;
    }
    return THREADS.getCurrentThreadAllocatedBytes() - before;
  }
}
