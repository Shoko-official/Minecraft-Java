package dev.shoko.minecraft.architecture.fixture.gson.data.codec;

import com.google.gson.Gson;

/** Violation: Gson held in a private field of a data package other than json. */
public final class GsonInPrivateField {
  private final Gson gson = new Gson();

  public String name() {
    return gson.getClass().getSimpleName();
  }
}
