package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.time.Instant;

/** Violation: Instant.now. */
public final class UsesInstantNow {
  public long value() {
    return Instant.now().getEpochSecond();
  }
}
