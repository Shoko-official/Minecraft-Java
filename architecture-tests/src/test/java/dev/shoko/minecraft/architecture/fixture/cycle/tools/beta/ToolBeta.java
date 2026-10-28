package dev.shoko.minecraft.architecture.fixture.cycle.tools.beta;

import dev.shoko.minecraft.architecture.fixture.cycle.tools.alpha.ToolAlpha;

/** Half of a cycle between two tools. */
public final class ToolBeta {
  public ToolAlpha next() {
    return new ToolAlpha();
  }
}
