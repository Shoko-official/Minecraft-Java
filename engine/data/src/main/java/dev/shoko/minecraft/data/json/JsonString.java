package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;

/**
 * A JSON string, unescaped. Any Java string; whether an unpaired surrogate written as an escape is
 * accepted is an open question of the skeleton.
 *
 * @param origin the file
 * @param path the place in the file
 * @param value the text
 */
public record JsonString(FileOrigin origin, JsonPath path, String value) implements JsonNode {

  /**
   * Returns {@link JsonType#STRING}.
   *
   * @return the type
   */
  @Override
  public JsonType type() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Compares the text.
   *
   * @param other another node
   * @return whether {@code other} is a string with the same text
   */
  @Override
  public boolean sameValueAs(JsonNode other) {
    return Skeleton.throwNotImplemented();
  }
}
