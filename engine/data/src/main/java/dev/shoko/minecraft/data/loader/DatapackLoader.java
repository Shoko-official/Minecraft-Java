package dev.shoko.minecraft.data.loader;

import dev.shoko.minecraft.data.diag.Outcome;
import java.io.IOException;

/** Loads a stack of datapacks into registries of JSON trees and resolved tags (K2 of the spec). */
public final class DatapackLoader {

  private DatapackLoader() {}

  /**
   * Loads the packs of a request. In order:
   *
   * <ol>
   *   <li>Every pack gets a {@link PackReport}: format compatibility (R5, R6), required flags, the
   *       flags among them that are not active, and the experimental mark (R17). An incompatible
   *       pack is still loaded: activating it is the caller's choice (R6).
   *   <li>A pack whose required flags are not all in {@link LoadRequest#activeFlags()} is discarded
   *       before anything else, as if it were not in the stack: none of its files, filters or
   *       overlays take part in the load. One {@code PACK_FLAGS_INACTIVE} error is reported on the
   *       origin {@code (pack, pack.mcmeta)} at the root path; the other packs load normally. The
   *       loader never changes the flags and knows no list of valid flags (R66).
   *   <li>Every stacked path is attributed by a {@code PathClassifier} (R8 to R16, R24); ignored
   *       files give their {@code FILE_IGNORED} diagnostic (none for a wrong extension, R12) and
   *       the load goes on.
   *   <li>Every element file and tag file is read as JSON (R49 to R54, R57 to R61). An invalid file
   *       is reported with its single error and counts as absent, without hiding the version of a
   *       pack of lower priority (R54); the warnings of a valid file are kept.
   *   <li>Elements are kept as trees, the version of the highest priority winning (R41), without
   *       checking their schema: any JSON type is a valid element (R29).
   *   <li>Tags are merged and resolved registry by registry, against the entries of static
   *       registries, the elements loaded, or the functions loaded (R33 to R37).
   * </ol>
   *
   * The result is present as soon as the packs could be listed. Errors in the data are diagnostics,
   * never exceptions; with the data of the target and no extra pack there is no error diagnostic
   * (exit criterion of M2).
   *
   * @param request the packs, catalog and flags
   * @return the loaded data and the diagnostics of the whole load, in the order of the pack stack
   *     then of the sorted paths
   * @throws IOException when a pack cannot be listed or a file cannot be read
   */
  public static Outcome<LoadedData> load(LoadRequest request) throws IOException {
    return Skeleton.throwNotImplemented();
  }
}
