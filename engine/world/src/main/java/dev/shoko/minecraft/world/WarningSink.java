package dev.shoko.minecraft.world;

/**
 * Receives the non-fatal findings of readers: a provisional fallback was applied, an entry was
 * ignored, a record was treated as absent (BS-R21, CF-R11, CF-R26, AR-R20, AR-R21).
 *
 * <p>Port: the caller decides where warnings go (log, test collector). The engine never prints. A
 * warning never changes the result, which depends only on the input.
 */
@FunctionalInterface
public interface WarningSink {

  /**
   * Reports one finding.
   *
   * @param message a description for humans, in English, naming the faulty element
   */
  void warn(String message);
}
