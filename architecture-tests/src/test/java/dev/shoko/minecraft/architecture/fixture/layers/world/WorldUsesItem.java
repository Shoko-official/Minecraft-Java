package dev.shoko.minecraft.architecture.fixture.layers.world;

import dev.shoko.minecraft.architecture.fixture.layers.item.ItemLeaf;

/** Violation: world and item must not know each other. */
public final class WorldUsesItem {
  public int value() {
    return new ItemLeaf().value();
  }
}
