package dev.shoko.minecraft.architecture.fixture.layers.item;

import dev.shoko.minecraft.architecture.fixture.layers.world.WorldLeaf;

/** Violation: item and world must not know each other. */
public final class ItemUsesWorld {
  public int value() {
    return new WorldLeaf().value();
  }
}
