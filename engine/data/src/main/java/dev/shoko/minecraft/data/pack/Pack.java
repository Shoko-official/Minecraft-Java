package dev.shoko.minecraft.data.pack;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.pack.meta.PackMeta;
import dev.shoko.minecraft.data.pack.meta.PackType;
import java.io.IOException;

/** One pack: an identifier, an origin, its files and its metadata. Immutable. */
public final class Pack {

  private Pack() {}

  /**
   * Opens a pack by reading its {@code pack.mcmeta} (R4, R7).
   *
   * @param id the pack identifier shown by the game, such as {@code minecart_improvements} or
   *     {@code file/mypack} (R40)
   * @param origin where the pack comes from
   * @param type the kind of pack
   * @param files the files of the pack
   * @return the pack, or empty when there is no usable {@code pack.mcmeta} (R7: the pack is
   *     ignored, with an {@code INVALID_PACK_META} diagnostic)
   * @throws IOException when {@code pack.mcmeta} cannot be read from the files
   */
  public static Outcome<Pack> open(String id, PackOrigin origin, PackType type, PackFiles files)
      throws IOException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Builds a pack that has no {@code pack.mcmeta}, such as the implicit vanilla pack (R3), with
   * {@link PackMeta#implicit(String, PackType)} as metadata.
   *
   * @param id the pack identifier
   * @param origin where the pack comes from
   * @param type the kind of pack
   * @param files the files of the pack
   * @return the pack
   */
  public static Pack implicit(String id, PackOrigin origin, PackType type, PackFiles files) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the identifier.
   *
   * @return the identifier
   */
  public String id() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns where the pack comes from.
   *
   * @return the origin
   */
  public PackOrigin origin() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the kind of pack.
   *
   * @return the type
   */
  public PackType type() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the metadata.
   *
   * @return the content of {@code pack.mcmeta}, or the implicit metadata
   */
  public PackMeta meta() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the files, root of the pack.
   *
   * @return the file access
   */
  public PackFiles files() {
    return Skeleton.throwNotImplemented();
  }
}
