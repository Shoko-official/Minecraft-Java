package dev.shoko.minecraft.architecture.fixture.good.core;

import java.time.Duration;
import java.time.Instant;

/** Time arithmetic and math that do not read the clock or unseeded randomness. */
public final class AllowedTimeAndMath {
  public long value() {
    return Instant.ofEpochMilli(0).plus(Duration.ofSeconds(1)).toEpochMilli()
        + Math.floorMod(-1, 16);
  }
}
