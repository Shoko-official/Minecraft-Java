package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.util.random.RandomGenerator;

/** Violation: RandomGenerator.getDefault. */
public final class UsesDefaultRandomGenerator {
  public long value() {
    return RandomGenerator.getDefault().nextLong();
  }
}
