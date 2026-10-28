package dev.shoko.minecraft.architecture.fixture.determinism.core;

/** Violation: Math.random. */
public final class UsesMathRandom {
  public double value() {
    return Math.random();
  }
}
