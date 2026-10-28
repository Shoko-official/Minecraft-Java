package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.Holder;
import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.data.registry.RegistryKey;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The basic codecs and combinators the modules assemble their codecs from. Every codec refuses a
 * JSON {@code null} and a node of another type with a {@code WRONG_TYPE} error (R56).
 */
public final class Codecs {

  private Codecs() {}

  /**
   * Returns the codec of strings.
   *
   * @return a codec accepting a JSON string
   */
  public static Codec<String> string() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the codec of booleans.
   *
   * @return a codec accepting {@code true} and {@code false}
   */
  public static Codec<Boolean> bool() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the codec of 32-bit integers.
   *
   * @return a codec accepting a number in integer form (R50) that fits an {@code int}; a decimal
   *     such as {@code 1.0} is a {@code WRONG_TYPE} error and a larger integer an {@code
   *     INVALID_VALUE} error
   */
  public static Codec<Integer> integer() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the codec of 64-bit integers.
   *
   * @return a codec accepting a number in integer form (R50)
   */
  public static Codec<Long> longInteger() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the codec of decimal numbers.
   *
   * @return a codec accepting any JSON number, integer or decimal form, as a double (provisional:
   *     whether the game accepts {@code 1} where a decimal is expected is an open question)
   */
  public static Codec<Double> number() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the codec of identifiers.
   *
   * @return a codec accepting a string read by {@link Identifier#parse(String)} (R22); an invalid
   *     text is an {@code INVALID_IDENTIFIER} error
   */
  public static Codec<Identifier> identifier() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a codec that hands the node over untouched, for content decoded later.
   *
   * @return a codec accepting any node
   */
  public static Codec<JsonNode> raw() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the codec of lists.
   *
   * @param element the codec of the elements
   * @param <T> the type of the elements
   * @return a codec accepting a JSON array; the diagnostics of every faulty element are reported,
   *     each with its own path, and the list has no value if one element failed
   */
  public static <T> Codec<List<T>> list(Codec<T> element) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the codec of references to the entries of a registry, written as identifiers.
   *
   * @param registry the registry the entry belongs to
   * @param <T> the type of the entries
   * @return a codec giving {@link Holder#reference} for an identifier (R22); the entry must exist
   *     in the registry according to {@code RegistryAccess#contains}, otherwise the error is {@code
   *     UNRESOLVED_REFERENCE} (K3)
   */
  public static <T> Codec<Holder<T>> registryReference(RegistryKey<T> registry) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the codec of sets of entries, written as a tag or an inline list.
   *
   * @param registry the registry the entries belong to
   * @param <T> the type of the entries
   * @return a codec accepting a string {@code #namespace:path} (R23), which gives {@link
   *     HolderSet#tag} and requires the tag to exist, or an array of identifiers, which gives
   *     {@link HolderSet#list}; a single identifier string is not accepted (provisional, open
   *     question)
   */
  public static <T> Codec<HolderSet<T>> holderSet(RegistryKey<T> registry) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the codec of a union selected by a field.
   *
   * @param typeField the key holding the variant name, such as {@code type}
   * @param variants the codec of each variant by the identifier written in the field; the codec
   *     receives the whole object, the type field included
   * @param <T> the common type
   * @return a codec reading the field as an identifier (R22) and delegating to the matching codec;
   *     an absent field is {@code MISSING_FIELD} and an unknown name {@code UNKNOWN_VARIANT}
   */
  public static <T> Codec<T> byType(
      String typeField, Map<Identifier, Codec<? extends T>> variants) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the codec of an object read field by field.
   *
   * @param reading builds the value from the fields with an {@link ObjectReader}
   * @param <T> the type of the value
   * @return a codec accepting a JSON object, giving the result of {@code reading}, or the
   *     diagnostic of the first field that failed (R56); the function must be free of side effects
   *     since it runs once per decode
   */
  public static <T> Codec<T> object(Function<ObjectReader, T> reading) {
    return Skeleton.throwNotImplemented();
  }
}
