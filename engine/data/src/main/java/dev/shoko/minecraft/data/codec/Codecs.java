package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.registry.Holder;
import dev.shoko.minecraft.data.registry.HolderSet;
import dev.shoko.minecraft.data.registry.RegistryKey;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The basic codecs and combinators the modules assemble their codecs from (K1), decoding only
 * (R65). Every codec reports a JSON node of another type than the one it expects, {@code null}
 * included, as {@code CODEC_TYPE_MISMATCH} at the path of that node (R56, R62 f); no type is
 * promoted, except an integer read as a decimal (R62 a) and a single identifier read by the
 * reference sets (R64). Diagnostic codes are those of R61.
 */
public final class Codecs {

  private Codecs() {}

  /**
   * Returns the codec of strings (R62 d).
   *
   * @return a codec accepting only a JSON string
   */
  public static Codec<String> string() {
    return ScalarCodecs.STRING;
  }

  /**
   * Returns the codec of booleans (R62 c).
   *
   * @return a codec accepting only {@code true} and {@code false}; {@code 0}, {@code 1} and {@code
   *     "true"} are {@code CODEC_TYPE_MISMATCH}
   */
  public static Codec<Boolean> bool() {
    return ScalarCodecs.BOOL;
  }

  /**
   * Returns the codec of 32-bit integers (R62 b).
   *
   * @return a codec accepting only a number in integer form (R59 a), {@code -0} giving 0; a decimal
   *     form, even of integral value ({@code 1.0}, {@code 1e0}, {@code -0.0}), is {@code
   *     CODEC_TYPE_MISMATCH}, and an integer outside {@code [-2^31, 2^31-1]} is {@code
   *     CODEC_NUMBER_RANGE}
   */
  public static Codec<Integer> integer() {
    return ScalarCodecs.INTEGER;
  }

  /**
   * Returns the codec of 64-bit integers (R62 b).
   *
   * @return a codec accepting only a number in integer form; a decimal form is {@code
   *     CODEC_TYPE_MISMATCH}. The reader already refuses an integer beyond 64 bits, so {@code
   *     CODEC_NUMBER_RANGE} cannot occur on a tree read from a file
   */
  public static Codec<Long> longInteger() {
    return ScalarCodecs.LONG;
  }

  /**
   * Returns the codec of decimal numbers read as a binary64 (R62 a).
   *
   * @return a codec accepting an integer or a decimal form; an integer is converted to the nearest
   *     double ({@code 9007199254740993} gives {@code 9007199254740992.0})
   */
  public static Codec<Double> decimal() {
    return ScalarCodecs.DECIMAL;
  }

  /**
   * Returns the codec of decimal numbers read as a binary32 (R62 a).
   *
   * @return a codec accepting an integer or a decimal form, converted from the nearest binary64 to
   *     the nearest float; a result that is infinite ({@code 1e40}) is {@code CODEC_NUMBER_RANGE}
   */
  public static Codec<Float> floatDecimal() {
    return ScalarCodecs.FLOAT;
  }

  /**
   * Returns the codec of identifiers.
   *
   * @return a codec accepting a string read by {@link Identifier#parse(String)} (R22); an invalid
   *     text, a {@code #tag} included, is {@code CODEC_INVALID_IDENTIFIER}
   */
  public static Codec<Identifier> identifier() {
    return ScalarCodecs.IDENTIFIER;
  }

  /**
   * Returns a codec that hands the node over untouched, for content decoded later.
   *
   * @return a codec accepting any node, {@code null} included
   */
  public static Codec<JsonNode> raw() {
    return StructureCodecs.raw();
  }

  /**
   * Returns the codec of lists (R62 e).
   *
   * @param element the codec of the elements
   * @param <T> the type of the elements
   * @return a codec accepting only a JSON array, a lone value not being promoted ({@code 1} and
   *     {@code {"0":1}} are {@code CODEC_TYPE_MISMATCH}); an empty array is valid. The diagnostics
   *     of every faulty element are reported, each with its own path, and the list has no value if
   *     one element failed
   */
  public static <T> Codec<List<T>> list(Codec<T> element) {
    return StructureCodecs.list(element);
  }

  /**
   * Returns the codec of a reference to one entry of a registry, written as an identifier (R64).
   *
   * @param registry the registry the entry belongs to
   * @param <T> the type of the entries
   * @return a codec giving {@link Holder#reference} for a string read by R22; a {@code #tag} is
   *     {@code CODEC_INVALID_IDENTIFIER}, and an entry that {@code RegistryAccess#contains} does
   *     not find is {@code CODEC_UNRESOLVED_REFERENCE} at the path of the value (K3)
   */
  public static <T> Codec<Holder<T>> registryReference(RegistryKey<T> registry) {
    return ReferenceCodecs.reference(registry);
  }

  /**
   * Returns the codec of sets of entries of a registry (R64). By JSON type:
   *
   * <ul>
   *   <li>a string without {@code #}: one entry, read by R22; the set is {@link HolderSet#list}
   *       with that holder;
   *   <li>a string starting with {@code #}: a tag of the same registry, read by R23; the set is
   *       {@link HolderSet#tag}, and a tag that does not exist or failed (R36) is {@code
   *       CODEC_UNRESOLVED_REFERENCE} at the path of the value;
   *   <li>an array of strings: an inline list of entries, never of tags (a {@code #} gives {@code
   *       CODEC_INVALID_IDENTIFIER} at {@code $[i]}); an empty array is valid and gives an empty
   *       set; a repeated entry is kept once, at the place of its first occurrence, without
   *       diagnostic; an entry the access does not find is {@code CODEC_UNRESOLVED_REFERENCE} at
   *       {@code $[i]}; an element that is not a string is {@code CODEC_TYPE_MISMATCH};
   *   <li>any other type (object, number, boolean, {@code null}): {@code CODEC_TYPE_MISMATCH}.
   * </ul>
   *
   * Unlike a tag file (R32), an array here cannot hold {@code #tag} entries or objects.
   *
   * @param registry the registry the entries belong to
   * @param <T> the type of the entries
   * @return the codec
   */
  public static <T> Codec<HolderSet<T>> holderSet(RegistryKey<T> registry) {
    return ReferenceCodecs.set(registry);
  }

  /**
   * Returns the codec of a union selected by a field.
   *
   * @param typeField the key holding the variant name, such as {@code type}
   * @param variants the codec of each variant by the identifier written in the field; the codec
   *     receives the whole object, the type field included: a variant built with {@link
   *     UnknownFields#REJECT} must declare that field itself (for instance with {@code
   *     reader.has(typeField)}), or it is rejected as unknown
   * @param <T> the common type
   * @return a codec reading the field as an identifier (R22) and delegating to the matching codec;
   *     an absent field is {@code CODEC_MISSING_FIELD}, a field that is not a valid identifier
   *     {@code CODEC_INVALID_IDENTIFIER}, and a name with no variant {@code
   *     CODEC_UNRESOLVED_REFERENCE} (the closed list of R61 has no code for it; provisional)
   */
  public static <T> Codec<T> byType(
      String typeField, Map<Identifier, Codec<? extends T>> variants) {
    return StructureCodecs.byType(typeField, variants);
  }

  /**
   * Returns the codec of an object that ignores the fields it does not read (R63 a).
   *
   * @param reading declares the fields with an {@link ObjectReader} and returns the supplier that
   *     builds the value from the handles it got
   * @param <T> the type of the value
   * @return the codec; see {@link #object(UnknownFields, Function)}
   */
  public static <T> Codec<T> object(Function<ObjectReader, Supplier<T>> reading) {
    return object(UnknownFields.IGNORE, reading);
  }

  /**
   * Returns the codec of an object read field by field (R56, R62 g, R63).
   *
   * <p>The codec accepts only a JSON object. It calls {@code reading} once per decode, which must
   * be free of side effects; every declaration made on the reader is decoded and its diagnostics
   * are collected, whether or not an earlier one failed. If no declared field failed, the supplier
   * returned by {@code reading} is called to build the value. With {@link UnknownFields#REJECT},
   * every key never declared then gives {@code CODEC_UNKNOWN_FIELD} at {@code $.name}, in text
   * order; those errors do not remove the value, which is built from the declared fields, so the
   * caller must test {@code hasErrors()} on the outcome.
   *
   * @param unknownFields what to do with the keys that are never declared
   * @param reading declares the fields with an {@link ObjectReader} and returns the supplier that
   *     builds the value from the handles it got
   * @param <T> the type of the value
   * @return the codec
   */
  public static <T> Codec<T> object(
      UnknownFields unknownFields, Function<ObjectReader, Supplier<T>> reading) {
    return new ObjectCodec<>(unknownFields, reading);
  }
}
