package dev.shoko.minecraft.architecture.fixture.gson.data.json;

import com.google.gson.JsonObject;

/** Violation: a public method returns a Gson type. */
public final class GsonReturned {
  public JsonObject tree() {
    return new JsonObject();
  }
}
