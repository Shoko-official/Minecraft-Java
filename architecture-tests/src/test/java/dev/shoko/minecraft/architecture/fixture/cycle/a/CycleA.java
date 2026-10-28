package dev.shoko.minecraft.architecture.fixture.cycle.a;

import dev.shoko.minecraft.architecture.fixture.cycle.b.CycleB;

/** Half of a package cycle with CycleB. */
public final class CycleA {
  public CycleB next() {
    return new CycleB();
  }
}
