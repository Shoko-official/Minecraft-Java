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
   *   <li>Every pack gets a {@link PackReport}: format compatibility (R5, R6), required flags and
   *       the experimental mark (R17). An incompatible pack is still loaded: activating it is the
   *       caller's choice (R6).
   *   <li>Every stacked path is attributed by a {@code PathClassifier} (R8 to R16, R24); ignored
   *       files give their diagnostic and the load goes on.
   *   <li>Every element file and tag file is read as JSON (R49 to R54). An invalid file is reported
   *       and counts as absent, without hiding the version of a pack of lower priority (R54).
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
