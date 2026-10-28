package dev.shoko.minecraft.architecture.fixture.bad.core;

import dev.shoko.minecraft.architecture.fixture.bad.world.WorldClass;

/** Violation: core depends on world, an upward dependency. */
public final class CoreClass {
  public WorldClass world() {
    return new WorldClass();
  }
}
