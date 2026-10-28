package dev.shoko.minecraft.core.math;

/**
 * Sine and cosine read from a table, an internal contract of the engine (R17, provisional).
 *
 * <p>Provisional parameters, to be confirmed by probe P17: a table of 65,536 {@code float} entries,
 * entry {@code i} being the {@code float} rounding of {@code sin(2 * pi * i / 65536)} computed in
 * {@code double}; factor {@code K = 65536 / (2 * pi)} in {@code double}; indexing by candidate DL
 * of the spec: {@code (int) ((long) (x * K) & 65535)}, the cosine adding {@code 16384.0} before the
 * conversion. Consequences: the period is correct for every finite {@code x}, NaN gives 0, and
 * {@code tableCos(-1.0) != tableCos(1.0)}.
 */
public final class TrigTable {

  private TrigTable() {}

  /**
   * Returns the table sine of {@code x} (R17, provisional indexing DL).
   *
   * @param x angle in radians
   * @return the table entry for {@code x}
   */
  public static float tableSin(double x) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the table cosine of {@code x}: the sine table read a quarter period ahead (R17,
   * provisional indexing DL).
   *
   * @param x angle in radians
   * @return the table entry for {@code x} shifted by 16,384
   */
  public static float tableCos(double x) {
    return Skeleton.throwNotImplemented();
  }
}
