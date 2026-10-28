package dev.shoko.minecraft.data.json;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.Outcome;

/**
 * Reads one JSON document into a {@link JsonNode} tree, strictly (R49 to R54).
 *
 * <p>Behavior common to every read method:
 *
 * <ul>
 *   <li>The text is RFC 8259 JSON and nothing else (R52). Comments, trailing commas, single quotes,
 *       unquoted keys or strings, {@code NaN}, {@code Infinity} and numbers such as {@code +1},
 *       {@code .5} or {@code 01} make the file invalid (provisional, probe P12).
 *   <li>A byte order mark, an empty or blank file, content after the root value and bytes that are
 *       not UTF-8 make the file invalid (R53, provisional).
 *   <li>Any value is a valid root: object, array, string, number, boolean or {@code null} (R29).
 *   <li>A duplicate key keeps the last value and adds a warning with {@code DUPLICATE_KEY} (R51,
 *       provisional); the position of the member in the object is not specified.
 *   <li>Numbers keep their written form and exact value (R50); a decimal that overflows to infinity
 *       or an integer beyond 64 bits is invalid ({@code NUMBER_OUT_OF_RANGE}).
 *   <li>Nesting is limited to {@link #MAX_DEPTH} arrays and objects, root included; deeper input is
 *       invalid ({@code JSON_TOO_DEEP}).
 *   <li>An invalid file gives an empty value and an error diagnostic, never an exception (R54).
 * </ul>
 */
public final class JsonReader {

  /**
   * Maximum number of nested arrays and objects, root included. Provisional: no spec rule yet; the
   * value is the default of the library (ADR 0006), far above the 17 levels of the target data
   * (R49).
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
   * Reads already decoded text; same rules as {@link #read(byte[], FileOrigin)}, a leading {@code
   * U+FEFF} included.
   *
   * @param text the text
   * @param origin the file, attached to every node and diagnostic
   * @return the root, or empty when the text is invalid, with the diagnostics found
   */
  public static Outcome<JsonNode> read(String text, FileOrigin origin) {
    return Skeleton.throwNotImplemented();
  }
}
