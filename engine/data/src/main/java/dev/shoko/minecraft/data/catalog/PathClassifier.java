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
   *   <li>Another extension than the folder takes is {@link FileAttribution.Skipped}, with no
   *       diagnostic (R12).
   *   <li>Otherwise {@link FileAttribution.Ignored}, reported as {@code FILE_IGNORED}: an unknown
   *       folder or a file in a static registry folder (R15); a tag under a registry that takes
   *       none, such as {@code tags/structure} (R16); a namespace or path that breaks the
   *       identifier syntax, such as a capital letter, a space or {@code ..} (R24).
   * </ul>
   *
   * @param path the path in the stack, with {@code /} separators
   * @return the attribution
   */
  public FileAttribution classify(String path) {
    return Skeleton.throwNotImplemented();
  }
}
