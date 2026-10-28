package dev.shoko.minecraft.data.pack.meta;

/**
 * The range of format versions a pack supports (R5). Both ends are inclusive. The constructor does
 * not check the order: {@code min > max} is a broken pack, reported by {@link
 * #compatibilityWith(PackFormat)}.
 *
 * @param min the oldest supported version; a written integer {@code N} or {@code [N]} gives {@code
 *     (N, 0)}
 * @param max the newest supported version; a written integer {@code N} or {@code [N]} gives {@code
 *     (N, 2147483647)}, so it covers every minor number
 */
public record PackFormatRange(PackFormat min, PackFormat max) {

  /**
   * Classifies the range against a target version (R5).
   *
   * @param target the version the game expects, such as {@link PackType#targetFormat()}
   * @return {@link PackCompatibility#BROKEN} when {@code min > max}, {@link
   *     PackCompatibility#TOO_OLD} when {@code max < target}, {@link PackCompatibility#TOO_NEW}
   *     when {@code min > target}, otherwise {@link PackCompatibility#COMPATIBLE}
   */
  public PackCompatibility compatibilityWith(PackFormat target) {
    return Skeleton.throwNotImplemented();
  }
}
