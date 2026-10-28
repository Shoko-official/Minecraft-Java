package dev.shoko.minecraft.architecture.fixture.bad.tools.oracle;

import dev.shoko.minecraft.architecture.fixture.bad.world.WorldClass;

/** Violation: the oracle must not use the engine. */
public final class OracleClass {
  public WorldClass world() {
    return new WorldClass();
  }
}
