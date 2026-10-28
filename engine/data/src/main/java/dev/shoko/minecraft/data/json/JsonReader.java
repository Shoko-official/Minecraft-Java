package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.Outcome;

/**
 * Reads one JSON document into a {@link JsonNode} tree, strictly (R49 to R54, R57 to R61).
 *
 * <p>Behavior common to every read method:
 *
 * <ul>
 *   <li>The text is RFC 8259 JSON and nothing else (R52): comments, trailing commas, single quotes,
 *       unquoted keys or strings, {@code NaN}, {@code Infinity}, numbers such as {@code +1}, {@code
 *       .5}, {@code 01} or {@code 1.}, and whitespace other than space, tab, line feed and carriage
 *       return make the file invalid with {@code JSON_SYNTAX} (provisional, probe P12).
 *   <li>A byte order mark ({@code JSON_BOM}), an empty or blank file ({@code JSON_EMPTY}), content
 *       after the root value ({@code JSON_TRAILING}) and bytes that are not UTF-8, surrogates and
 *       overlong forms included ({@code JSON_UTF8}), make the file invalid (R53, R60 e).
 *   <li>Any value is a valid root: object, array, string, number, boolean or {@code null} (R29).
 *   <li>Strings accept the escapes {@code \" \\ \/ \b \f \n \r \t} and {@code \}{@code uXXXX}; any
 *       other escape, a unicode escape with fewer than four hexadecimal digits, and a raw control
 *       character are {@code JSON_SYNTAX}. An escaped unpaired surrogate is accepted and kept
 *       (R60); keys follow the same rules.
 *   <li>A key written twice keeps the place of its first occurrence and the value of its last, with
 *       one {@code JSON_DUPLICATE_KEY} warning per extra occurrence at the path of the member, in
 *       reading order, duplicates inside a discarded value included (R58). The file stays valid.
 *   <li>Numbers keep their written form and exact value (R50, R59): an integer beyond 64 bits and a
 *       decimal that overflows to infinity are {@code JSON_NUMBER_RANGE}; {@code -0} is a valid
 *       integer of value 0; {@code -0.0} is a valid decimal that keeps its sign; a decimal that
 *       underflows is a valid zero.
 *   <li>Nesting is limited to {@link #MAX_DEPTH} arrays and objects, root included, empty ones
 *       included: the opening of the next level is {@code JSON_TOO_DEEP}, reported at the path of
 *       that container, without reading further and without using the call stack in proportion to
 *       the depth of the text (R57).
 *   <li>An invalid file gives an empty value and exactly one error diagnostic, the first met with
 *       the precedence BOM, then invalid UTF-8 anywhere in the file, then reading errors in text
 *       order; the duplicate-key warnings are not reported for it (R58 e, R61 c). It never throws
 *       (R54).
 * </ul>
 */
public final class JsonReader {

  /**
   * Maximum number of nested arrays and objects, root included (R57). Provisional: 255 is the
   * default of the library (ADR 0006), not a measure of the reference game (probe P18); the target
   * data nests 17 levels (R49).
   */
  public static final int MAX_DEPTH = 255;

  private JsonReader() {}

  /**
   * Reads the bytes of a file.
   *
   * @param utf8 the file content
   * @param origin the file, attached to every node and diagnostic
   * @return the root, or empty when the file is invalid, with the diagnostics found
   */
  public static Outcome<JsonNode> read(byte[] utf8, FileOrigin origin) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads already decoded text; same rules as {@link #read(byte[], FileOrigin)}. A leading {@code
   * U+FEFF} is {@code JSON_BOM}, and an unpaired surrogate in the text itself is {@code JSON_UTF8},
   * as the bytes that would encode it are (provisional: the spec only describes bytes).
   *
   * @param text the text
   * @param origin the file, attached to every node and diagnostic
   * @return the root, or empty when the text is invalid, with the diagnostics found
   */
  public static Outcome<JsonNode> read(String text, FileOrigin origin) {
    return Skeleton.throwNotImplemented();
  }
}
