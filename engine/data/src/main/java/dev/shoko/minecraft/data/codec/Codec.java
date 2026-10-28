package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import java.util.function.Function;

/**
 * Decodes a JSON node into a value of type {@code T}. Decoding only: M2 has no encoder (R65 a, c).
 *
 * <p>A codec is a value built from {@link Codecs}, not a function written by hand: the class cannot
 * be extended outside this package, so that an encoding capability can later be added to the
 * combinators without breaking the modules that use them (R65 b).
 *
 * <p>A failure is a diagnostic carrying the JSON path of the faulty node (R56, R61), never an
 * exception; the outcome has no value in that case. A decoder must not keep state between calls.
 *
 * @param <T> the decoded type
 */
public abstract class Codec<T> {

  Codec() {}

  /**
   * Decodes a node.
   *
   * @param node the node, of any type
   * @param access used to check references to registry entries and tags (K3, K4)
   * @return the value, or empty with error diagnostics when the node is not valid; the diagnostics
   *     of every faulty part are reported, in the order given by the combinator that found them
   */
  public abstract Outcome<T> decode(JsonNode node, RegistryAccess access);

  /**
   * Returns a codec that converts the decoded value.
   *
   * @param conversion the conversion; it must not throw
   * @param <R> the new type
   * @return a codec giving {@code conversion} applied to the value, with the same diagnostics
   */
  public final <R> Codec<R> map(Function<? super T, ? extends R> conversion) {
    return Skeleton.throwNotImplemented();
  }
}
