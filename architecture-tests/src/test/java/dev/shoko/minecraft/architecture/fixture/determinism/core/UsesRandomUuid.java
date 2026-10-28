package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.util.UUID;

/** Violation: UUID.randomUUID. */
public final class UsesRandomUuid {
  public long value() {
    return UUID.randomUUID().getMostSignificantBits();
  }
}
