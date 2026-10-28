package dev.shoko.minecraft.architecture.fixture.layers.tools.oracle;

import dev.shoko.minecraft.architecture.fixture.layers.world.WorldLeaf;

/** Violation: the oracle must not use the engine. */
public final class OracleUsesWorld {
  public int value() {
    return new WorldLeaf().value();
  }
}
