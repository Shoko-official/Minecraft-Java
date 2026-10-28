package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * A JSON object. Keys are unique (R58): a key written twice keeps the place of its first occurrence
 * and the value of its last, so {@code {"a":1,"b":2,"a":3}} has the members {@code a=3}, {@code
 * b=2} in that order. Keys are equal when their decoded strings are equal unit by unit, with no
 * case folding (R58 a).
 *
 * @param origin the file
 * @param path the place in the file
 * @param members the members in file order, none {@code null}
 */
public record JsonObject(FileOrigin origin, JsonPath path, SequencedMap<String, JsonNode> members)
    implements JsonNode {

  /**
   * Returns the member stored under a key.
   *
   * @param key the key
   * @return the value, or empty when the key is absent
   */
  public Optional<JsonNode> get(String key) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns {@link JsonType#OBJECT}.
   *
   * @return the type
   */
  @Override
  public JsonType type() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Compares members by key, whatever their order.
   *
   * @param other another node
   * @return whether {@code other} is an object with the same keys mapped to the same values
   */
  @Override
  public boolean sameValueAs(JsonNode other) {
    return Skeleton.throwNotImplemented();
  }
}
