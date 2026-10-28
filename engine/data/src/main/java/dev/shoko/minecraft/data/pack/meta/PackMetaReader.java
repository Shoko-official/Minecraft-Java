package dev.shoko.minecraft.data.pack.meta;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;

/** Reads a {@code pack.mcmeta} tree into a {@link PackMeta} (R2, R4, R5, R7, R47, R48). */
public final class PackMetaReader {

  private PackMetaReader() {}

  /**
   * Reads the metadata.
   *
   * <p>A format value is an integer {@code N}, {@code [N]} or {@code [major, minor]} (R5). A key
   * unknown at any level ({@code pack}, {@code features}, {@code filter}, an entry of {@code
   * overlays}, the root) is ignored without diagnostic (R4, R63 c).
   *
   * <p>A root that is not an object with a {@code pack} object is not a pack: the result has no
   * value and a {@code PACK_INVALID} warning (R7, provisional); the caller reads the JSON itself
   * first, so an unreadable file has its own {@code JSON_*} error.
   *
   * <p>A known field of the wrong type does not fail the read but breaks the pack (R4, R5): the
   * result has a value reporting {@link PackCompatibility#BROKEN}, with a {@code
   * CODEC_TYPE_MISMATCH} error at the path of the field. The same holds for an absent {@code
   * min_format} or {@code max_format}, with {@code CODEC_MISSING_FIELD} at {@code
   * $.pack.min_format} or {@code $.pack.max_format}, and for a {@code features.enabled} entry, a
   * filter expression that is not a valid regular expression, or an overlay directory name outside
   * {@code a-z 0-9 _ - .} (reported as {@code CODEC_TYPE_MISMATCH}, provisional: R61 has no better
   * code). {@code min_format} above {@code max_format} is broken without diagnostic. All the errors
   * of the file are collected, in text order.
   *
   * @param root the parsed file
   * @return the metadata, or empty when the file is not a pack, with the diagnostics found
   */
  public static Outcome<PackMeta> read(JsonNode root) {
    return Skeleton.throwNotImplemented();
  }
}
