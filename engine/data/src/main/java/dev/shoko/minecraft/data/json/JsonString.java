package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;

/**
 * A JSON string, unescaped: a sequence of UTF-16 units. An unpaired surrogate written as an escape
 * (a high or low surrogate alone) is accepted and kept as that unit (R60 c); a valid pair forms one
 * code point. Code that writes or shows such a string must escape the isolated unit (R60 f).
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
    return JsonType.STRING;
  }

  /**
   * Compares the text.
   *
   * @param other another node
   * @return whether {@code other} is a string with the same text
   */
  @Override
  public boolean sameValueAs(JsonNode other) {
    return other instanceof JsonString that && value.equals(that.value);
  }
}
