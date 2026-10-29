package dev.shoko.minecraft.architecture.fixture.gson.data.json;

import com.google.gson.JsonObject;

/** Violation: a public field has a Gson type. */
public final class GsonPublicField {
  public final JsonObject tree = new JsonObject();
}
