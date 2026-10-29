package dev.shoko.minecraft.architecture.fixture.gson.data.json;

import com.google.gson.JsonObject;

/** Violation: a protected method returns a Gson type. */
public class GsonProtectedMethod {
  protected JsonObject tree() {
    return new JsonObject();
  }
}
