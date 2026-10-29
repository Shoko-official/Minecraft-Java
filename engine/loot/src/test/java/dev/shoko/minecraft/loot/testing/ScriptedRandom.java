package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.core.random.RandomSource;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * A random source that replays a fixed script. Each scripted value is a fraction in {@code [0, 1)}:
 * {@code nextFloat} and {@code nextDouble} return it, {@code nextInt(bound)} returns {@code
 * floor(value * bound)}, {@code nextBoolean} returns {@code value >= 0.5}. Every draw takes one
 * value and counts one step, so a test reads how many decisions a consumer made whatever method it
 * used. An exhausted script fails loudly instead of inventing numbers.
 */
public final class ScriptedRandom implements RandomSource {

  private final Deque<Double> script = new ArrayDeque<>();
  private long steps;

  private ScriptedRandom(double[] values) {
    for (double value : values) {
      script.add(value);
    }
  }

  /** A source that replays these fractions, each in {@code [0, 1)}. */
  public static ScriptedRandom of(double... fractions) {
    return new ScriptedRandom(fractions);
  }

  /** A source that returns the same fraction {@code times} times. */
  public static ScriptedRandom repeating(double fraction, int times) {
    double[] values = new double[times];
    Arrays.fill(values, fraction);
    return new ScriptedRandom(values);
  }

  /** The number of scripted values not yet consumed. */
  public int remaining() {
    return script.size();
  }

  private double take() {
    Double value = script.pollFirst();
    if (value == null) {
      throw new IllegalStateException("scripted random exhausted after " + steps + " draws");
    }
    steps++;
    return value;
  }

  @Override
  public int nextInt() {
    return (int) (take() * 4_294_967_296.0 - 2_147_483_648.0);
  }

  @Override
  public int nextInt(int bound) {
    if (bound <= 0) {
      throw new IllegalArgumentException("bound must be positive: " + bound);
    }
    return (int) Math.floor(take() * bound);
  }

  @Override
  public long nextLong() {
    return (long) (take() * 9.0e18);
  }

  @Override
  public boolean nextBoolean() {
    return take() >= 0.5;
  }

  @Override
  public float nextFloat() {
    return (float) take();
  }

  @Override
  public double nextDouble() {
    return take();
  }

  @Override
  public double nextGaussian() {
    return take();
  }

  @Override
  public void setSeed(long seed) {
    throw new UnsupportedOperationException("a script has no seed");
  }

  @Override
  public long steps() {
    return steps;
  }

  @Override
  public void skip(long n) {
    for (long i = 0; i < n; i++) {
      take();
    }
  }

  @Override
  public RandomSource fork() {
    throw new UnsupportedOperationException("a script cannot be forked");
  }
}
