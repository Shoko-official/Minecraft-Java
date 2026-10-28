package dev.shoko.minecraft.world;

/**
 * Receives the non-fatal findings of a lenient decoder: a provisional fallback was applied, an
 * entry was ignored (BS-R21, BS-R22, CF-R11, CF-R34). The decoders that build a {@link ReadResult}
 * collect these findings into its warning list themselves.
 *
 * <p>Port: the caller decides where warnings go (log, test collector). The engine never prints. A
 * warning never changes the result, which depends only on the input.
 */
@FunctionalInterface
public interface WarningSink {

  /**
   * Reports one finding.
   *
   * @param code the kind of finding
   * @param message a description for humans, in English, naming the faulty element
   */
  void warn(WarningCode code, String message);
}
