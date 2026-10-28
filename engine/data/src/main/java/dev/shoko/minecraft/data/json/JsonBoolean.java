package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;

/**
 * A JSON boolean.
 *
 * @param origin the file
 * @param path the place in the file
 * @param value the value
 */
public record JsonBoolean(FileOrigin origin, JsonPath path, boolean value) implements JsonNode {

  /**
   * Returns {@link JsonType#BOOLEAN}.
   *
   * @return the type
   */
  @Override
  public JsonType type() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Compares the value.
   *
   * @param other another node
   * @return whether {@code other} is a boolean with the same value
   */
  @Override
  public boolean sameValueAs(JsonNode other) {
    return Skeleton.throwNotImplemented();
  }
}
