package dev.shoko.minecraft.architecture.fixture.determinism.core;

/** Violation: System.currentTimeMillis. */
public final class UsesCurrentTimeMillis {
  public long value() {
    return System.currentTimeMillis();
  }
}
