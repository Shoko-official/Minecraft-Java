package dev.shoko.minecraft.architecture.fixture.gson.data.json;

import com.google.gson.JsonElement;
import java.util.List;

/** Violation: a public method returns a collection of a Gson type. */
public final class GsonInGenericReturn {
  public List<JsonElement> elements() {
    return List.of();
  }
}
