package dev.shoko.minecraft.core.random;

/**
 * 48-bit linear congruential generator, G48 in the spec (R1 to R12), with the constants and draw
 * algorithms of the public {@code java.util.Random} specification (R8). Mutable, not thread-safe.
 *
 * <p>State: an integer {@code x}, {@code 0 <= x < 2^48}. One step replaces {@code x} by {@code
 * (0x5DEECE66D * x + 0xB) mod 2^48} (R1). Consumption per call (R11): 1 step for {@link
 * #next(int)}, {@link #nextInt()}, {@link #nextBoolean()}, {@link #nextFloat()} and {@link
 * #nextInt(int)} with a power-of-two bound; 1 step per attempt for {@link #nextInt(int)} with
 * another bound; 2 for {@link #nextLong()} and {@link #nextDouble()}; 0 or 4 per attempt for {@link
 * #nextGaussian()}; 0 for {@link #setSeed(long)}.
 */
public final class Lcg48Random implements RandomSource {

  /** Multiplier of the step, 25214903917 (R1). */
  private static final long MULTIPLIER = 0x5DEECE66DL;

  /** Increment of the step, 11 (R1). */
  private static final long INCREMENT = 0xBL;

  private static final long MASK_48 = (1L << 48) - 1;

  private final PolarGaussian gaussian = new PolarGaussian();
  private long state;
  private long steps;

  private Lcg48Random(long seed) {
    this.state = scramble(seed);
  }

  private static long scramble(long seed) {
    return (seed ^ MULTIPLIER) & MASK_48;
  }

  /**
   * Returns a generator initialized with this seed (R2): state {@code (seed XOR 0x5DEECE66D) mod
   * 2^48}, no cached gaussian, step count 0. Only the 48 low bits of the seed matter.
   *
   * @param seed the seed, any long
   * @return the generator
   */
  public static Lcg48Random ofSeed(long seed) {
    return new Lcg48Random(seed);
  }

  /**
   * Returns the current state (R1).
   *
   * @return the state, {@code 0 <= x < 2^48}
   */
  public long state() {
    return state;
  }

  /**
   * Takes one step and returns the {@code bits} high bits of the new state, {@code x >>> (48 -
   * bits)} (R3). For 32 bits the result is read as a signed int.
   *
   * @param bits the number of bits, 1 to 32
   * @return the bits, non-negative when {@code bits < 32}
   * @throws IllegalArgumentException when {@code bits} is outside 1 to 32; no step is consumed
   */
  public int next(int bits) {
    if (bits < 1 || bits > 32) {
      throw new IllegalArgumentException("bits must be between 1 and 32: " + bits);
    }
    state = (state * MULTIPLIER + INCREMENT) & MASK_48;
    steps++;
    return (int) (state >>> (48 - bits));
  }

  /** {@inheritDoc} G48: {@code next(32)}, one step (R4). */
  @Override
  public int nextInt() {
    return next(32);
  }

  /**
   * {@inheritDoc} G48 (R5): for a power-of-two bound (1 included), {@code (bound * next(31)) >>
   * 31}, one step; otherwise draw {@code r = next(31)} and {@code v = r mod bound} until {@code r -
   * v + (bound - 1) < 2^31}, one step per attempt.
   */
  @Override
  public int nextInt(int bound) {
    if (bound <= 0) {
      throw new IllegalArgumentException("bound must be positive: " + bound);
    }
    if ((bound & -bound) == bound) {
      return (int) ((bound * (long) next(31)) >> 31);
    }
    int r;
    int v;
    do {
      r = next(31);
      v = r % bound;
    } while ((long) r - v + (bound - 1) >= 1L << 31);
    return v;
  }

  /**
   * {@inheritDoc} G48: {@code (next(32) << 32) + next(32)} mod {@code 2^64}, the second draw sign
   * extended before the addition, two steps (R4).
   */
  @Override
  public long nextLong() {
    long high = next(32);
    return (high << 32) + next(32);
  }

  /** {@inheritDoc} G48: {@code next(1) != 0}, one step (R4). */
  @Override
  public boolean nextBoolean() {
    return next(1) != 0;
  }

  /** {@inheritDoc} G48: {@code next(24) * 2^-24}, one step (R6). */
  @Override
  public float nextFloat() {
    return next(24) * 0x1.0p-24f;
  }

  /** {@inheritDoc} G48: {@code (next(26) * 2^27 + next(27)) * 2^-53}, two steps (R6). */
  @Override
  public double nextDouble() {
    long high = next(26);
    return ((high << 27) + next(27)) * 0x1.0p-53;
  }

  /** {@inheritDoc} G48: four steps per attempt (two {@link #nextDouble()}), R7, R9. */
  @Override
  public double nextGaussian() {
    return gaussian.next(this);
  }

  /** {@inheritDoc} G48: state {@code (seed XOR 0x5DEECE66D) mod 2^48} (R2). */
  @Override
  public void setSeed(long seed) {
    state = scramble(seed);
    gaussian.clear();
    steps = 0;
  }

  @Override
  public long steps() {
    return steps;
  }

  /**
   * {@inheritDoc} G48: computed in O(log n) by composing the affine step {@code x -> a*x + c} by
   * squaring, {@code (a1, c1)} then {@code (a2, c2)} giving {@code (a2*a1, a2*c1 + c2)} mod {@code
   * 2^48} (R10).
   */
  @Override
  public void skip(long n) {
    if (n < 0) {
      throw new IllegalArgumentException("step count must not be negative: " + n);
    }
    // (a, c) accumulates the applied steps, (powA, powC) is the step raised to 2^bit.
    long a = 1;
    long c = 0;
    long powA = MULTIPLIER;
    long powC = INCREMENT;
    for (long rest = n; rest != 0; rest >>>= 1) {
      if ((rest & 1) != 0) {
        a = (powA * a) & MASK_48;
        c = (powA * c + powC) & MASK_48;
      }
      powC = (powA * powC + powC) & MASK_48;
      powA = (powA * powA) & MASK_48;
    }
    state = (a * state + c) & MASK_48;
    steps += n;
  }

  /**
   * {@inheritDoc} Provisional, not faithful (R28): this generator draws one {@link #nextLong()}
   * (two steps) and the child is {@link #ofSeed(long)} of that long.
   */
  @Override
  public Lcg48Random fork() {
    return ofSeed(nextLong());
  }
}
