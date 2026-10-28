package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.util.concurrent.ThreadLocalRandom;

/** Violation: ThreadLocalRandom. */
public final class UsesThreadLocalRandom {
  public long value() {
    return ThreadLocalRandom.current().nextLong();
  }
}
