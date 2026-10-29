package dev.shoko.minecraft.architecture.fixture.gson.data.json;

import com.google.gson.JsonParseException;

/** Violation: a public method declares a Gson exception. */
public final class GsonThrown {
  public void parse() throws JsonParseException {}
}
