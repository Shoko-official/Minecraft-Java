package dev.shoko.minecraft.architecture.fixture.layers.core;

import dev.shoko.minecraft.architecture.fixture.layers.world.WorldLeaf;

/** Violation: core depends on world, an upward dependency. */
public final class CoreUsesWorld {
  public int value() {
    return new WorldLeaf().value();
  }
}
