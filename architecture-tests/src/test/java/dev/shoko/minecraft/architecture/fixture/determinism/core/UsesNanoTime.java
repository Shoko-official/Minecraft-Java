package dev.shoko.minecraft.architecture.fixture.determinism.core;

/** Violation: System.nanoTime. */
public final class UsesNanoTime {
  public long value() {
    return System.nanoTime();
  }
}
