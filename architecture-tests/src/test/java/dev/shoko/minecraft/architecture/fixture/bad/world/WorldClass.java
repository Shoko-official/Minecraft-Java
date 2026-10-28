package dev.shoko.minecraft.architecture.fixture.bad.world;

import dev.shoko.minecraft.architecture.fixture.bad.core.CoreClass;

/** Allowed direction, but closes a cycle with core. */
public final class WorldClass {
  public CoreClass core() {
    return new CoreClass();
  }
}
