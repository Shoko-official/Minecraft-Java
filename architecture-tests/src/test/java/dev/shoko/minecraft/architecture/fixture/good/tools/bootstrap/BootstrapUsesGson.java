package dev.shoko.minecraft.architecture.fixture.good.tools.bootstrap;

import com.google.gson.JsonObject;

/** A tool is a program of its own and may use Gson anywhere. */
public final class BootstrapUsesGson {
  public JsonObject empty() {
    return new JsonObject();
  }
}
