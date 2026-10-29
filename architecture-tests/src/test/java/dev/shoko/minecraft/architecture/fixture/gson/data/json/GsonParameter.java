package dev.shoko.minecraft.architecture.fixture.gson.data.json;

import com.google.gson.JsonElement;

/** Violation: a public method takes a Gson type. */
public final class GsonParameter {
  public boolean isNull(JsonElement element) {
    return element == null;
  }
}
