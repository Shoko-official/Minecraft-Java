package dev.shoko.minecraft.data.tag;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;

/** Reads the JSON of a tag file into a {@link TagFile} (R32). */
public final class TagFileReader {

  private TagFileReader() {}

  /**
   * Reads a tag file.
   *
   * <p>The root is an object with an optional boolean {@code replace} (default {@code false}) and a
   * {@code values} array. An entry is a string, an identifier or {@code #tag} (R22, R23), or an
   * object {@code {"id": string, "required": boolean}} where {@code required} defaults to {@code
   * true}. A missing {@code values}, an entry of another type, an unknown key or an invalid
   * identifier makes the file invalid: the result has no value and an {@code INVALID_TAG_FILE} or
   * {@code INVALID_IDENTIFIER} error, and the file counts as absent (R32, R54, provisional).
   *
   * @param root the parsed file
   * @return the tag file, or empty when it is invalid, with the diagnostics found
   */
  public static Outcome<TagFile> read(JsonNode root) {
    return Skeleton.throwNotImplemented();
  }
}
