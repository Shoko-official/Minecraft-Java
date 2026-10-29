package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.core.random.RandomSource;
import java.util.ArrayList;
import java.util.List;

/**
 * A random source that counts the draws of a consumer and fails the test with {@link TooMuchWork}
 * once a limit is passed, so that a missing budget in the code under test ends the test instead of
 * hanging it. With {@link #recording} it also keeps the bounded integer draws in order, written as
 * {@code nextInt(bound)}.
 */
public final class WatchedRandom implements RandomSource {

  /** Thrown when the consumer draws more than the limit; an error, never caught by the module. */
  public static final class TooMuchWork extends Error {
    private static final long serialVersionUID = 1L;

    TooMuchWork(long draws) {
      super("the consumer made " + draws + " draws without a budget error");
    }
  }

  private final RandomSource delegate;
  private final long limit;
  private final List<String> calls;
  private long draws;

  private WatchedRandom(RandomSource delegate, long limit, List<String> calls) {
    this.delegate = delegate;
    this.limit = limit;
    this.calls = calls;
  }

  /** Counts the draws of {@code delegate}, failing the test beyond {@code limit}. */
  public static WatchedRandom limited(RandomSource delegate, long limit) {
    return new WatchedRandom(delegate, limit, null);
  }

  /** Keeps every bounded integer draw of {@code delegate}, with no limit. */
  public static WatchedRandom recording(RandomSource delegate) {
    return new WatchedRandom(delegate, Long.MAX_VALUE, new ArrayList<>());
  }

  /** The number of draws made so far. */
  public long draws() {
    return draws;
  }

  /** The recorded bounded integer draws, in order; empty unless made by {@link #recording}. */
  public List<String> calls() {
    return calls == null ? List.of() : List.copyOf(calls);
  }

  private void tick() {
    if (++draws > limit) {
      throw new TooMuchWork(draws);
    }
  }

  @Override
  public int nextInt() {
    tick();
    return delegate.nextInt();
  }

  @Override
  public int nextInt(int bound) {
    tick();
    if (calls != null) {
      calls.add("nextInt(" + bound + ")");
    }
    return delegate.nextInt(bound);
  }

  @Override
  public long nextLong() {
    tick();
    return delegate.nextLong();
  }

  @Override
  public boolean nextBoolean() {
    tick();
    return delegate.nextBoolean();
  }

  @Override
  public float nextFloat() {
    tick();
    return delegate.nextFloat();
  }

  @Override
  public double nextDouble() {
    tick();
    return delegate.nextDouble();
  }

  @Override
  public double nextGaussian() {
    tick();
    return delegate.nextGaussian();
  }

  @Override
  public void setSeed(long seed) {
    delegate.setSeed(seed);
  }

  @Override
  public long steps() {
    return delegate.steps();
  }

  @Override
  public void skip(long n) {
    delegate.skip(n);
  }

  @Override
  public RandomSource fork() {
    return new WatchedRandom(delegate.fork(), limit, calls);
  }
}
