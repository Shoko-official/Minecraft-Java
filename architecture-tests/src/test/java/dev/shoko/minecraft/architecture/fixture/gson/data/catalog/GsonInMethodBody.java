package dev.shoko.minecraft.architecture.fixture.gson.data.catalog;

import com.google.gson.JsonObject;

/** Violation: Gson used in a method body of a data package other than json. */
public final class GsonInMethodBody {
  public int size() {
    return new JsonObject().size();
  }
}
