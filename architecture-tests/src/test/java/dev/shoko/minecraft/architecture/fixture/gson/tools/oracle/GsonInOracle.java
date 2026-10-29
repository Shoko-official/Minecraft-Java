package dev.shoko.minecraft.architecture.fixture.gson.tools.oracle;

import com.google.gson.JsonObject;

/** Violation: Gson used in a tool that is not one of the Gson users. */
public final class GsonInOracle {
  public int size() {
    return new JsonObject().size();
  }
}
