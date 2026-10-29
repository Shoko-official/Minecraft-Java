package dev.shoko.minecraft.architecture.fixture.gson.world;

import com.google.gson.JsonObject;

/** Violation: Gson used in a module other than data. */
public final class GsonOutsideData {
  public int size() {
    return new JsonObject().size();
  }
}
