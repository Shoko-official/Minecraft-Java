package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Reads the fields of one JSON object for {@link Codecs#object(java.util.function.Function)}.
 *
 * <p>Reading never stops at the first problem (R62 g): a method that finds an invalid or missing
 * field records its error diagnostics and returns a handle that cannot be read; the object codec
 * calls the handles, through the supplier returned by the reading function, only when no field
 * failed. All diagnostics of the object are therefore reported. Diagnostics are ordered by the
 * order of the calls on this reader, then, in strict mode, the unknown fields in text order.
 *
 * <p>A key is known as soon as one method of this class is called with it, {@link #has(String)}
 * included; the others are unknown (R63). Not thread-safe; it lives for one decode call.
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
   * Returns the handling of unknown fields chosen for this object (R63).
   *
   * @return {@link UnknownFields#IGNORE} unless the codec was built as strict
   */
  public UnknownFields unknownFields() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Tells whether the object has a key, and marks the key as known.
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
   * @return a handle giving the decoded value; reading it after a failure of this field throws
   *     {@link IllegalStateException}. An absent key records {@code CODEC_MISSING_FIELD} at the
   *     path of the member that should have been there; an invalid value records the diagnostics of
   *     {@code codec}. A present {@code null} is {@code CODEC_TYPE_MISMATCH}.
   */
  public <F> Supplier<F> required(String name, Codec<F> codec) {
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
   * @return a handle giving the decoded value, or {@code defaultValue} when the key is absent; an
   *     invalid present value records the diagnostics of {@code codec}
   */
  public <F> Supplier<F> optional(String name, Codec<F> codec, F defaultValue) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads an optional field without default (R56).
   *
   * @param name the key
   * @param codec the decoder of the value
   * @param <F> the type of the field
   * @return a handle giving the decoded value, or empty when the key is absent; an invalid present
   *     value records the diagnostics of {@code codec}
   */
  public <F> Supplier<Optional<F>> optional(String name, Codec<F> codec) {
    return Skeleton.throwNotImplemented();
  }
}
