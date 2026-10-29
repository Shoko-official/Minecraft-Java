package dev.shoko.minecraft.architecture.fixture.gson.data.json;

import com.google.gson.JsonElement;

/** Violation: a public method takes an array of a Gson type. */
public final class GsonInArrayParameter {
  public int count(JsonElement[] elements) {
    return elements.length;
  }
}
