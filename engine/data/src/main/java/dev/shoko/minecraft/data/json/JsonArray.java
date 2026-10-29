package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;
import java.util.List;

/**
 * A JSON array.
 *
 * @param origin the file
 * @param path the place in the file
 * @param elements the elements in file order, none {@code null}
 */
public record JsonArray(FileOrigin origin, JsonPath path, List<JsonNode> elements)
    implements JsonNode {

  /**
   * Returns {@link JsonType#ARRAY}.
   *
   * @return the type
   */
  @Override
  public JsonType type() {
    return JsonType.ARRAY;
  }

  /**
   * Compares elements in order.
   *
   * @param other another node
   * @return whether {@code other} is an array with the same values at the same positions
   */
  @Override
  public boolean sameValueAs(JsonNode other) {
    if (!(other instanceof JsonArray that) || elements.size() != that.elements.size()) {
      return false;
    }
    for (int i = 0; i < elements.size(); i++) {
      if (!elements.get(i).sameValueAs(that.elements.get(i))) {
        return false;
      }
    }
    return true;
  }
}
