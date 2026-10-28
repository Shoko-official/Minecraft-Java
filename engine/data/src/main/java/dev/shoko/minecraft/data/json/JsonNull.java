package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;

/**
 * A JSON {@code null}. The reader accepts it; the codecs refuse it as a wrong type (R56).
 *
 * @param origin the file
 * @param path the place in the file
 */
public record JsonNull(FileOrigin origin, JsonPath path) implements JsonNode {

  /**
   * Returns {@link JsonType#NULL}.
   *
   * @return the type
   */
  @Override
  public JsonType type() {
    return JsonType.NULL;
  }

  /**
   * Compares the type.
   *
   * @param other another node
   * @return whether {@code other} is a {@code null}
   */
  @Override
  public boolean sameValueAs(JsonNode other) {
    return other instanceof JsonNull;
  }
}
