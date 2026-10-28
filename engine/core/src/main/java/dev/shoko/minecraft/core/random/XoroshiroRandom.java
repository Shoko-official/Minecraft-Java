package dev.shoko.minecraft.core.random;

/**
 * Xoroshiro128++ generator, X128 in the spec (R13 to R21). Mutable, not thread-safe.
 *
 * <p>State: two 64-bit words {@code (s0, s1)}, never both zero. One step computes the output {@code
 * r = rotl(s0 + s1, 17) + s0} on the state before the transition, then {@code t = s1 XOR s0},
 * {@code s0' = rotl(s0, 49) XOR t XOR (t << 21)}, {@code s1' = rotl(t, 28)} (R13). How the game
 * assembles the derived draws is not established (R16 to R20 are hypotheses H-X1 to H-X5).
 * Consumption (R21): one step for every draw except {@link #nextInt(int)} (one per attempt, rarely
 * more) and {@link #nextGaussian()} (0 or 2 per attempt).
 */
public final class XoroshiroRandom implements RandomSource {

  private static final long MASK_32 = 0xFFFFFFFFL;

  private final PolarGaussian gaussian = new PolarGaussian();
  private long s0;
  private long s1;
  private long steps;

  private XoroshiroRandom(Seed128 state) {
    this.s0 = state.lo();
    this.s1 = state.hi();
  }

  /**
   * Returns a generator with this raw state and a step count of 0 (R13). The all-zero state is
   * refused (R15, provisional until spec Q2): no known initialization produces it.
   *
   * @param state the state, {@code lo} is {@code s0} and {@code hi} is {@code s1}
   * @return the generator
   * @throws IllegalArgumentException when both words are zero
   */
  public static XoroshiroRandom ofState(Seed128 state) {
    if (state.lo() == 0 && state.hi() == 0) {
      throw new IllegalArgumentException("xoroshiro128++ state must not be all zero");
    }
    return new XoroshiroRandom(state);
  }

  /**
   * Returns a generator initialized from a 64-bit seed by {@link SeedExpansion#init64(long)} (R24,
   * R25), with no cached gaussian and a step count of 0.
   *
   * @param seed the seed, any long
   * @return the generator
   */
  public static XoroshiroRandom ofSeed(long seed) {
    return new XoroshiroRandom(SeedExpansion.init64(seed));
  }

  /**
   * Returns the current state (R13), as persisted in a named sequence's {@code source} (R32).
   *
   * @return the state, never all-zero
   */
  public Seed128 state() {
    return new Seed128(s0, s1);
  }

  /** One step: the output on the current state, then the transition (R13). */
  private long nextOutput() {
    long r = Long.rotateLeft(s0 + s1, 17) + s0;
    long t = s1 ^ s0;
    s0 = Long.rotateLeft(s0, 49) ^ t ^ (t << 21);
    s1 = Long.rotateLeft(t, 28);
    steps++;
    return r;
  }

  /** {@inheritDoc} X128: the 32 low bits of the output read as a signed int, one step (R17). */
  @Override
  public int nextInt() {
    return (int) nextOutput();
  }

  /**
   * {@inheritDoc} X128 (R18, hypothesis H-X2): Lemire's multiply-and-reject method on the 32 low
   * bits of the output, with no special case for powers of two; one step per attempt.
   */
  @Override
  public int nextInt(int bound) {
    if (bound <= 0) {
      throw new IllegalArgumentException("bound must be positive: " + bound);
    }
    // An unsigned 32-bit draw times a bound below 2^31 fits in a non-negative long.
    long product = (nextOutput() & MASK_32) * bound;
    if ((product & MASK_32) < bound) {
      long threshold = ((1L << 32) - bound) % bound;
      while ((product & MASK_32) < threshold) {
        product = (nextOutput() & MASK_32) * bound;
      }
    }
    return (int) (product >>> 32);
  }

  /** {@inheritDoc} X128: the output read as a signed long, one step (R16). */
  @Override
  public long nextLong() {
    return nextOutput();
  }

  /** {@inheritDoc} X128: the lowest bit of the output, one step (R19, hypothesis H-X5). */
  @Override
  public boolean nextBoolean() {
    return (nextOutput() & 1) != 0;
  }

  /** {@inheritDoc} X128: {@code (r >>> 40) * 2^-24}, one step (R19, hypothesis H-X3). */
  @Override
  public float nextFloat() {
    return (nextOutput() >>> 40) * 0x1.0p-24f;
  }

  /** {@inheritDoc} X128: {@code (r >>> 11) * 2^-53}, one step (R19, hypothesis H-X4). */
  @Override
  public double nextDouble() {
    return (nextOutput() >>> 11) * 0x1.0p-53;
  }

  /** {@inheritDoc} X128: two steps per attempt (two {@link #nextDouble()}), R20. */
  @Override
  public double nextGaussian() {
    return gaussian.next(this);
  }

  /** {@inheritDoc} X128: state {@link SeedExpansion#init64(long)} of the seed (R25). */
  @Override
  public void setSeed(long seed) {
    Seed128 state = SeedExpansion.init64(seed);
    s0 = state.lo();
    s1 = state.hi();
    gaussian.clear();
    steps = 0;
  }

  @Override
  public long steps() {
    return steps;
  }

  /**
   * {@inheritDoc} X128: executed step by step, in O(n); no closed form exists for an arbitrary
   * {@code n} (R21).
   */
  @Override
  public void skip(long n) {
    if (n < 0) {
      throw new IllegalArgumentException("step count must not be negative: " + n);
    }
    for (long i = 0; i < n; i++) {
      nextOutput();
    }
  }

  /**
   * {@inheritDoc} Provisional, not faithful (R28): this generator draws one {@link #nextLong()}
   * (one step) and the child is {@link #ofSeed(long)} of that long.
   */
  @Override
  public XoroshiroRandom fork() {
    return ofSeed(nextLong());
  }
}
