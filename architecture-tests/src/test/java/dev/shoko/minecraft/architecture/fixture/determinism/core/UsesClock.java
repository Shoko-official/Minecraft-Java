package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.time.Clock;

/** Violation: Clock. */
public final class UsesClock {
  public long value() {
    return Clock.systemUTC().millis();
  }
}
