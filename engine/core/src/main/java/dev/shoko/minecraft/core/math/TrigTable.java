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

  /** Number of entries over one period (R17). */
  private static final int SIZE = 65536;

  private static final int INDEX_MASK = SIZE - 1;

  /** Index offset of a quarter period, which turns the sine table into a cosine (R17). */
  private static final double QUARTER_PERIOD = SIZE / 4.0;

  /** Radians to table index, {@code 65536 / (2 * pi)} in {@code double} (R17). */
  private static final double RADIANS_TO_INDEX = SIZE / (2 * StrictMath.PI);

  // Written once by the class initializer and never exposed, so it is effectively immutable.
  private static final float[] SINE = buildTable();

  private TrigTable() {}

  /**
   * Returns the table sine of {@code x} (R17, provisional indexing DL).
   *
   * @param x angle in radians
   * @return the table entry for {@code x}
   */
  public static float tableSin(double x) {
    return SINE[(int) ((long) (x * RADIANS_TO_INDEX) & INDEX_MASK)];
  }

  /**
   * Returns the table cosine of {@code x}: the sine table read a quarter period ahead (R17,
   * provisional indexing DL).
   *
   * @param x angle in radians
   * @return the table entry for {@code x} shifted by 16,384
   */
  public static float tableCos(double x) {
    return SINE[(int) ((long) (x * RADIANS_TO_INDEX + QUARTER_PERIOD) & INDEX_MASK)];
  }

  private static float[] buildTable() {
    float[] table = new float[SIZE];
    for (int i = 0; i < SIZE; i++) {
      table[i] = (float) StrictMath.sin(i * StrictMath.PI * 2.0 / SIZE);
    }
    return table;
  }
}
