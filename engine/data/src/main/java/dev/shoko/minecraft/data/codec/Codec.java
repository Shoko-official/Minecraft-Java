package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import java.util.function.Function;

/**
 * Decodes a JSON node into a value of type {@code T}.
 *
 * <p>A failure is a diagnostic carrying the JSON path of the faulty node (R56), never an exception;
 * the outcome has no value in that case. A decoder must not keep state between calls.
 *
 * @param <T> the decoded type
 */
@FunctionalInterface
public interface Codec<T> {

  /**
   * Decodes a node.
   *
   * @param node the node, of any type
   * @param access used to check references to registry entries and tags (K3, K4)
   * @return the value, or empty with error diagnostics when the node is not valid
   */
  Outcome<T> decode(JsonNode node, RegistryAccess access);

  /**
   * Returns a codec that converts the decoded value.
   *
   * @param conversion the conversion; it must not throw
   * @param <R> the new type
   * @return a codec giving {@code conversion} applied to the value, with the same diagnostics
   */
  default <R> Codec<R> map(Function<? super T, ? extends R> conversion) {
    return Skeleton.throwNotImplemented();
  }
}
