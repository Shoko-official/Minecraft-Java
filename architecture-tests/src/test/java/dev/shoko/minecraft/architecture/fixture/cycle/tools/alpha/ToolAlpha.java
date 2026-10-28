package dev.shoko.minecraft.architecture.fixture.cycle.tools.alpha;

import dev.shoko.minecraft.architecture.fixture.cycle.tools.beta.ToolBeta;

/** Half of a cycle between two tools. */
public final class ToolAlpha {
  public ToolBeta next() {
    return new ToolBeta();
  }
}
