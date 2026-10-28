package dev.shoko.minecraft.architecture.fixture.bad.core;

import java.util.Random;

/** Violation: engine code using the clock and unseeded randomness. */
public final class ClockUser {
  public double roll() {
    return Math.random();
  }

  public long now() {
    return System.nanoTime();
  }

  public Random rng() {
    return new Random();
  }
}
