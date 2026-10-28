package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.time.ZoneOffset;
import java.time.chrono.HijrahDate;

/** Violation: HijrahDate.now, in a sub-package of java.time. */
public final class UsesHijrahDateNow {
  public long value() {
    return HijrahDate.now(ZoneOffset.UTC).toEpochDay();
  }
}
