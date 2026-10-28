package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.util.Random;

/** Violation: Random. */
public final class UsesRandom {
  public long value() {
    return new Random().nextLong();
  }
}
