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
   * true}. A key unknown to the file, or to an object entry, is ignored without diagnostic (R32,
   * R63 c).
   *
   * <p>The file is invalid, and counts as absent (R32, R54, provisional), when:
   *
   * <ul>
   *   <li>the root is not an object, {@code values} or {@code replace} has another type, an entry
   *       is neither a string nor an object, or an object entry has an {@code id} or a {@code
   *       required} of another type: {@code CODEC_TYPE_MISMATCH} at the path of the value;
   *   <li>{@code values} is absent, or an object entry has no {@code id}: {@code
   *       CODEC_MISSING_FIELD};
   *   <li>an identifier or {@code #} reference breaks R20: {@code CODEC_INVALID_IDENTIFIER} at
   *       {@code $.values[i]} or {@code $.values[i].id}.
   * </ul>
   *
   * All the errors of the file are collected, in text order (R62 g).
   *
   * @param root the parsed file
   * @return the tag file, or empty when it is invalid, with the diagnostics found
   */
  public static Outcome<TagFile> read(JsonNode root) {
    return Skeleton.throwNotImplemented();
  }
}
