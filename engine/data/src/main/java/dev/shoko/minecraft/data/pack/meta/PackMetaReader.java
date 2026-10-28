package dev.shoko.minecraft.data.pack.meta;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;

/** Reads a {@code pack.mcmeta} tree into a {@link PackMeta} (R2, R4, R5, R7, R47, R48). */
public final class PackMetaReader {

  private PackMetaReader() {}

  /**
   * Reads the metadata.
   *
   * <p>A format value is an integer {@code N}, {@code [N]} or {@code [major, minor]} (R5). A root
   * that is not an object with a {@code pack} object is not a pack: the result has no value and an
   * {@code INVALID_PACK_META} error (R7, provisional). An unreadable filter or overlay entry is
   * dropped with an {@code INVALID_PACK_FILTER} error. Unusable format fields do not fail the read:
   * they give {@link PackMeta#formats()} empty and an {@code INCOMPATIBLE_PACK} warning (R5, R6).
   *
   * @param root the parsed file
   * @return the metadata, or empty when the file is not a pack, with the diagnostics found
   */
  public static Outcome<PackMeta> read(JsonNode root) {
    return Skeleton.throwNotImplemented();
  }
}
