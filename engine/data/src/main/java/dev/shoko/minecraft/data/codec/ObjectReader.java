package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import java.util.Optional;

/**
 * Reads the fields of one JSON object for {@link Codecs#object(java.util.function.Function)}. Every
 * method that fails throws {@link DecodeException}, which that method turns into an error outcome
 * with the path of the field. Not thread-safe; it lives for one decode call.
 */
public final class ObjectReader {

  private ObjectReader() {}

  /**
   * Returns the object being read.
   *
   * @return the node
   */
  public JsonObject node() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the access given to the decode call, for codecs built on the fly.
   *
   * @return the access
   */
  public RegistryAccess access() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether the object has a key.
   *
   * @param name the key
   * @return whether it is present, whatever its value, {@code null} included
   */
  public boolean has(String name) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a required field (R56).
   *
   * @param name the key
   * @param codec the decoder of the value
   * @param <F> the type of the field
   * @return the decoded value
   * @throws DecodeException with {@code MISSING_FIELD} when the key is absent, or with the
   *     diagnostics of {@code codec} when the value is not valid
   */
  public <F> F required(String name, Codec<F> codec) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads an optional field with a default (R56).
   *
   * @param name the key
   * @param codec the decoder of the value
   * @param defaultValue the value when the key is absent; an invalid value present under the key is
   *     an error, never replaced by the default
   * @param <F> the type of the field
   * @return the decoded value, or {@code defaultValue} when the key is absent
   * @throws DecodeException with the diagnostics of {@code codec} when a present value is not valid
   */
  public <F> F optional(String name, Codec<F> codec, F defaultValue) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads an optional field without default (R56).
   *
   * @param name the key
   * @param codec the decoder of the value
   * @param <F> the type of the field
   * @return the decoded value, or empty when the key is absent
   * @throws DecodeException with the diagnostics of {@code codec} when a present value is not valid
   */
  public <F> Optional<F> optional(String name, Codec<F> codec) {
    return Skeleton.throwNotImplemented();
  }
}
