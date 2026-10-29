package dev.shoko.minecraft.loot.modifier;

/**
 * Temporary: delete once this package is implemented. Body of every method of the API skeleton.
 * Calling it instead of a bare {@code throw} keeps callers compiling: Error Prone suggests
 * {@code @DoNotCall} for non-overridable methods whose only statement is a {@code throw}, and that
 * annotation would turn every call from the tests into a compile error.
 */
final class Skeleton {

  private Skeleton() {}

  /**
   * Always throws. The empty varargs only let the compiler infer {@code T} from the caller's return
   * type; the method never returns, so no value of {@code T} is ever produced.
   */
  @SafeVarargs
  static <T> T throwNotImplemented(T... inferredFromReturnType) {
    throw new UnsupportedOperationException("not implemented");
  }
}
