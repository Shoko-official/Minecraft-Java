package dev.shoko.minecraft.architecture.fixture.cycle.b;

import dev.shoko.minecraft.architecture.fixture.cycle.a.CycleA;

/** Half of a package cycle with CycleA. */
public final class CycleB {
  public CycleA next() {
    return new CycleA();
  }
}
