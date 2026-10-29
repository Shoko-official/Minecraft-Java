package dev.shoko.minecraft.architecture.fixture.gson.net;

import com.google.gson.JsonObject;

/** Violation: Gson used in the net module. */
public final class GsonInNet {
  public int size() {
    return new JsonObject().size();
  }
}
