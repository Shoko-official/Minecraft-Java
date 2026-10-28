package dev.shoko.minecraft.architecture.fixture.good.data;

import dev.shoko.minecraft.architecture.fixture.good.core.CoreClass;

/** Depends on core, as allowed. */
public final class DataClass {
  public int value() {
    return new CoreClass().value();
  }
}
