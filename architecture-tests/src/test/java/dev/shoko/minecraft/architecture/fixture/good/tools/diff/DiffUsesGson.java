package dev.shoko.minecraft.architecture.fixture.good.tools.diff;

import com.google.gson.JsonObject;

/** The diff tool is a program of its own and may use Gson anywhere. */
public final class DiffUsesGson {
  public JsonObject empty() {
    return new JsonObject();
  }
}
