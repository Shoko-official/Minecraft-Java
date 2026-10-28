package dev.shoko.minecraft.data.catalog;

/** Decides what each file path of a pack stack is (R8 to R16, R24). Immutable. */
public final class PathClassifier {

  private PathClassifier() {}

  /**
   * Builds a classifier for a catalog.
   *
   * @param catalog the loadable registries
   * @return the classifier
   */
  public static PathClassifier of(RegistryCatalog catalog) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Classifies one stacked path.
   *
   * <ul>
   *   <li>{@code data/<ns>/<folder>/<path>.json} with a registry that takes elements is an {@link
   *       FileAttribution.Element} with identifier {@code <ns>:<path>}; the folder is the longest
   *       known registry folder prefix (R8, R11). The path may contain sub-folders.
   *   <li>{@code data/<ns>/tags/<folder>/<path>.json} is a {@link FileAttribution.Tag} when the
   *       registry takes tags (R9, R16).
   *   <li>{@code .mcfunction} under {@code function} and {@code .nbt} under {@code structure} are
   *       {@link FileAttribution.Content} (R10).
   *   <li>{@code pack.mcmeta}, {@code pack.png}, {@code .mcassetsroot} and anything outside {@code
   *       data/} are {@link FileAttribution.Metadata} (R12, R13, R18).
   *   <li>Otherwise {@link FileAttribution.Ignored}: an unknown folder or a file in a static
   *       registry folder ({@code UNKNOWN_FOLDER}, R15); a tag under a registry that takes none
   *       ({@code TAGS_NOT_ACCEPTED}, R16); another extension in a registry folder ({@code
   *       UNSUPPORTED_EXTENSION}, R12); a namespace or path that breaks the identifier syntax, such
   *       as a capital letter, a space or {@code ..} ({@code INVALID_IDENTIFIER}, R24).
   * </ul>
   *
   * @param path the path in the stack, with {@code /} separators
   * @return the attribution
   */
  public FileAttribution classify(String path) {
    return Skeleton.throwNotImplemented();
  }
}
