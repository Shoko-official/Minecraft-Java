package dev.shoko.minecraft.architecture.fixture.good.world;

import dev.shoko.minecraft.architecture.fixture.good.core.CoreClass;
import dev.shoko.minecraft.architecture.fixture.good.data.DataClass;

/** Depends on data and, transitively, core. */
public final class WorldClass {
  public int value() {
    return new DataClass().value() + new CoreClass().value();
  }
}
