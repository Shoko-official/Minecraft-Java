package dev.shoko.minecraft.data.tag;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.registry.RegistryKey;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;

/** Merges the tag files of the packs and resolves the result (R33 to R37). */
public final class TagResolver {

  private TagResolver() {}

  /**
   * Merges and resolves the tags of one registry.
   *
   * <ul>
   *   <li>Merge (R34): for a tag, the layers go from the lowest to the highest priority; a layer
   *       with {@code replace} drops everything accumulated before it, any other adds its entries
   *       after the previous ones.
   *   <li>Resolution (R35): entries are visited in order; a {@code #tag} entry is replaced by the
   *       resolved content of that tag, depth first; an element already present is not repeated.
   *   <li>Failure (R36): a required element missing from the universe, or a required tag that does
   *       not exist or failed, makes the tag fail. Each such entry gives one {@code
   *       CODEC_UNRESOLVED_REFERENCE} error, in the file of the layer that holds the entry, at
   *       {@code $.values[i]} with {@code i} the position of the entry in that file. An entry with
   *       {@code required} false is simply skipped. The other tags keep loading.
   *   <li>Cycles (R37): every tag of a cycle, and every tag that requires one, fails. The entry
   *       that requires a tag of the cycle, or a tag that failed, gives the same error as above;
   *       the R61 table has no code of its own for a cycle (provisional).
   * </ul>
   *
   * The result is always present: failures are in {@link TagResolution#failed()} and in the
   * diagnostics.
   *
   * @param registry the registry the tags belong to, used in messages
   * @param layers the layers of each tag identifier, from the lowest to the highest priority; a tag
   *     file that was invalid is simply absent from its list (R54)
   * @param universe the identifiers of the elements a tag may contain (R33)
   * @return the resolution and its diagnostics
   */
  public static Outcome<TagResolution> resolve(
      RegistryKey<?> registry,
      SequencedMap<Identifier, List<TagSource>> layers,
      Set<Identifier> universe) {
    return Skeleton.throwNotImplemented();
  }
}
