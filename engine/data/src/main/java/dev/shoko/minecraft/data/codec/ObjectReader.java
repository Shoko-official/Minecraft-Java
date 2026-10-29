package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Reads the fields of one JSON object for {@link Codecs#object(java.util.function.Function)}.
 *
 * <p>Reading never stops at the first problem (R62 g): a method that finds an invalid or missing
 * field records its error diagnostics and returns a handle that cannot be read; the object codec
 * calls the handles, through the supplier returned by the reading function, only when no field
 * failed. All diagnostics of the object are therefore reported, in the order of the text (R62 g,
 * R63 b): those of a member sit where the member is in the object, whatever the order of the calls
 * on this reader, and in strict mode the unknown fields are interleaved by their own place. The
 * diagnostics of a required field that is absent have no place in the text and come last, in the
 * order of the calls.
 *
 * <p>A key is known as soon as one method of this class is called with it, {@link #has(String)}
 * included; the others are unknown (R63). Not thread-safe; it lives for one decode call.
 */
public final class ObjectReader {

  private final JsonObject node;
  private final RegistryAccess access;
  private final UnknownFields unknownFields;
  // Membership tests only: never iterated, so its order is not observable.
  private final Set<String> known = new HashSet<>();
  private final List<Found> found = new ArrayList<>();
  private boolean failed;

  ObjectReader(JsonObject node, RegistryAccess access, UnknownFields unknownFields) {
    this.node = node;
    this.access = access;
    this.unknownFields = unknownFields;
  }

  /**
   * Returns the object being read.
   *
   * @return the node
   */
  public JsonObject node() {
    return node;
  }

  /**
   * Returns the access given to the decode call, for codecs built on the fly.
   *
   * @return the access
   */
  public RegistryAccess access() {
    return access;
  }

  /**
   * Returns the handling of unknown fields chosen for this object (R63).
   *
   * @return {@link UnknownFields#IGNORE} unless the codec was built as strict
   */
  public UnknownFields unknownFields() {
    return unknownFields;
  }

  /**
   * Tells whether the object has a key, and marks the key as known.
   *
   * @param name the key
   * @return whether it is present, whatever its value, {@code null} included
   */
  public boolean has(String name) {
    known.add(name);
    return node.members().containsKey(name);
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
    known.add(name);
    JsonNode value = node.members().get(name);
    if (value == null) {
      found.add(new Found(null, List.of(Failures.missingField(node, name))));
      return failedHandle();
    }
    return decoded(name, codec.decode(value, access));
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
    known.add(name);
    JsonNode value = node.members().get(name);
    if (value == null) {
      return () -> defaultValue;
    }
    return decoded(name, codec.decode(value, access));
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
    return optional(name, codec.map(Optional::of), Optional.empty());
  }

  /** The diagnostics met for one member, or for an absent one when {@code member} is null. */
  record Found(String member, List<Diagnostic> diagnostics) {}

  boolean failed() {
    return failed;
  }

  List<Found> found() {
    return found;
  }

  boolean isKnown(String name) {
    return known.contains(name);
  }

  private <F> Supplier<F> decoded(String name, Outcome<F> outcome) {
    found.add(new Found(name, outcome.diagnostics()));
    if (outcome.value().isEmpty()) {
      return failedHandle();
    }
    F value = outcome.value().get();
    return () -> value;
  }

  private <F> Supplier<F> failedHandle() {
    failed = true;
    return () -> {
      throw new IllegalStateException("the field failed to decode");
    };
  }
}
