package dev.shoko.minecraft.core.random;

/**
 * Seeded pseudo-random generator of the engine: derived draws, reseeding, step counter and skip.
 *
 * <p>A step is one transition of the generator state; the consumption of a call is its number of
 * steps (spec vocabulary, R11, R21). Each family documents the consumption of each draw. A
 * generator counts its steps since its creation or its last {@link #setSeed(long)}, so that tests
 * can check how much a consumer draws (spec section on inputs and outputs).
 *
 * <p>Implementations are mutable and not thread-safe. They do not extend {@code java.util.Random}
 * nor implement {@code java.util.random.RandomGenerator} (R12).
 */
public interface RandomSource {

  /**
   * Draws an int over the whole range (R4, R17).
   *
   * @return the value
   */
  int nextInt();

  /**
   * Draws an int in {@code [0, bound)} (R5, R18). A bound that is not positive is an error and
   * consumes no step.
   *
   * @param bound the exclusive upper bound, positive
   * @return the value, {@code 0 <= v < bound}
   * @throws IllegalArgumentException when {@code bound <= 0}
   */
  int nextInt(int bound);

  /**
   * Draws an int in the inclusive range {@code [min, max]} the way the game's number providers do
   * (R12): {@code min + nextInt(max - min + 1)} when {@code min < max}; when {@code min >= max},
   * returns {@code min} and consumes no step.
   *
   * <p>When {@code max - min + 1} overflows the int range (for example {@code [-2^31, 2^31 - 1]}),
   * the computed bound is not positive and the call fails as {@link #nextInt(int)} does, without
   * consuming a step. The game's behavior in that case is open (spec Q4).
   *
   * @param min the inclusive lower bound
   * @param max the inclusive upper bound
   * @return the value, {@code min <= v <= max}, or {@code min} when {@code min >= max}
   * @throws IllegalArgumentException when {@code max - min + 1} overflows the int range
   */
  default int nextIntBetweenInclusive(int min, int max) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Draws a long (R4, R16).
   *
   * @return the value
   */
  long nextLong();

  /**
   * Draws a boolean (R4, R19).
   *
   * @return the value
   */
  boolean nextBoolean();

  /**
   * Draws a float, a multiple of {@code 2^-24} in {@code [0, 1)} (R6, R19).
   *
   * @return the value
   */
  float nextFloat();

  /**
   * Draws a double, a multiple of {@code 2^-53} in {@code [0, 1)} (R6, R19).
   *
   * @return the value
   */
  double nextDouble();

  /**
   * Draws a normally distributed double by Marsaglia's polar method with one cached value (R7, R9,
   * R20). A cached value is returned without consuming a step and survives other draws in between;
   * {@link #setSeed(long)} drops it. The logarithm and square root are those of {@link StrictMath}
   * (provisional choice, spec Q3).
   *
   * @return the value
   */
  double nextGaussian();

  /**
   * Reinitializes the generator from a seed (R2, R25): resets the state, drops the cached gaussian
   * and resets {@link #steps()} to 0. Consumes no step.
   *
   * @param seed the seed, any long
   */
  void setSeed(long seed);

  /**
   * Returns the number of steps taken since creation or the last {@link #setSeed(long)} (R11, R21).
   * {@link #skip(long)} counts its steps too.
   *
   * @return the step count, 0 or more
   */
  long steps();

  /**
   * Advances the state by exactly {@code n} steps, as {@code n} single-step draws would (R10, R21).
   * The cached gaussian, if any, is kept.
   *
   * @param n the number of steps, 0 or more
   * @throws IllegalArgumentException when {@code n < 0}
   */
  void skip(long n);

  /**
   * Provisional, not faithful (R28, spec Q7): derives a new generator of the same family from this
   * one. The child is a deterministic function of this generator's state, and the derivation
   * advances this generator by a fixed number of steps. Each family documents its provisional
   * construction; the faithful one will be set by the worldgen spec.
   *
   * @return a new, independent generator of the same family
   */
  RandomSource fork();
}
