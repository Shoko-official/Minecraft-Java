package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.security.SecureRandom;

/** Violation: SecureRandom. */
public final class UsesSecureRandom {
  public long value() {
    return new SecureRandom().nextLong();
  }
}
