package dev.shoko.minecraft.data.testing;

import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNull;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/**
 * Builds JSON nodes directly through the record constructors, without the reader, so that the
 * comparison methods of the nodes can be tested on their own. Every node sits at the root path.
 */
public final class Nodes {

  private Nodes() {}

  public static JsonNumber num(String literal) {
    return new JsonNumber(Jx.ORIGIN, JsonPath.root(), literal);
  }

  public static JsonString str(String value) {
    return new JsonString(Jx.ORIGIN, JsonPath.root(), value);
  }

  public static JsonBoolean bool(boolean value) {
    return new JsonBoolean(Jx.ORIGIN, JsonPath.root(), value);
  }

  public static JsonNull nul() {
    return new JsonNull(Jx.ORIGIN, JsonPath.root());
  }

  public static JsonArray arr(JsonNode... elements) {
    return new JsonArray(Jx.ORIGIN, JsonPath.root(), List.of(elements));
  }

  /** Builds an object from alternating keys and nodes, keeping the given order. */
  public static JsonObject obj(Object... keysAndValues) {
    SequencedMap<String, JsonNode> members = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      members.put((String) keysAndValues[i], (JsonNode) keysAndValues[i + 1]);
    }
    return new JsonObject(Jx.ORIGIN, JsonPath.root(), members);
  }
}
