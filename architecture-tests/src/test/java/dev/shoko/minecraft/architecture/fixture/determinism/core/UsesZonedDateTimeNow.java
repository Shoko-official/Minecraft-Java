package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/** Violation: ZonedDateTime.now with a zone. */
public final class UsesZonedDateTimeNow {
  public long value() {
    return ZonedDateTime.now(ZoneOffset.UTC).getYear();
  }
}
