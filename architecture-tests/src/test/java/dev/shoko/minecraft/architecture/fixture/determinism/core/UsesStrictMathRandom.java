package dev.shoko.minecraft.architecture.fixture.determinism.core;

/** Violation: StrictMath.random. */
public final class UsesStrictMathRandom {
  public double value() {
    return StrictMath.random();
  }
}
