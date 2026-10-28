package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;

/**
 * A node of a JSON document. Any type may be the root of a registry element (R29). The nodes are
 * records; their {@code equals} compares every component, location included, so use {@link
 * #sameValueAs(JsonNode)} to compare values.
 */
public sealed interface JsonNode
    permits JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {

  /**
   * Returns the kind of this value.
   *
   * @return the type
   */
  JsonType type();

  /**
   * Returns the file this node was read from.
   *
   * @return the origin
   */
  FileOrigin origin();

  /**
   * Returns the place of this node in its file.
   *
   * @return the path, {@link JsonPath#root()} for the root
   */
  JsonPath path();

  /**
   * Compares values, ignoring file, path and the order of object members. Numbers are equal when
   * they have the same form and the same exact value (R50): {@code 1} differs from {@code 1.0}.
   *
   * @param other another node
   * @return whether both trees hold the same value
   */
  boolean sameValueAs(JsonNode other);
}
