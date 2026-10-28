package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.util.SplittableRandom;

/** Violation: SplittableRandom. */
public final class UsesSplittableRandom {
  public long value() {
    return new SplittableRandom().nextLong();
  }
}
