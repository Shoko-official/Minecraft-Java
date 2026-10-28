package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.time.InstantSource;

/** Violation: InstantSource. */
public final class UsesInstantSource {
  public long value() {
    return InstantSource.system().millis();
  }
}
