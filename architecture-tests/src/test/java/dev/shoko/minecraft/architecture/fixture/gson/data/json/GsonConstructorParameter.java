package dev.shoko.minecraft.architecture.fixture.gson.data.json;

import com.google.gson.stream.JsonReader;

/** Violation: a public constructor takes a Gson type. */
public final class GsonConstructorParameter {
  private final String name;

  public GsonConstructorParameter(JsonReader reader) {
    this.name = reader.toString();
  }

  public String name() {
    return name;
  }
}
