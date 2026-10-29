package dev.shoko.minecraft.data.pack;

import dev.shoko.minecraft.data.pack.meta.FilterRule;
import dev.shoko.minecraft.data.pack.meta.OverlayEntry;
import dev.shoko.minecraft.data.pack.meta.PackCompatibility;
import dev.shoko.minecraft.data.pack.meta.PackFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The files of one pack once its active overlays are applied (R48): each stacked path maps to the
 * physical path that provides it. Overlay directories are not part of the base tree. Overlay
 * directories are single path steps, so the files are grouped by their first step and the cost
 * stays linear in the number of files and overlays.
 */
final class PackView {

  private static final String DATA_PREFIX = "data/";
  private static final Set<String> OVERLAY_IGNORED = Set.of("pack.mcmeta", "pack.png");

  private PackView() {}

  static Map<String, String> of(Pack pack, PackFormat target) throws IOException {
    List<String> paths = pack.files().paths();
    List<OverlayEntry> entries = pack.meta().overlays();
    Map<String, String> view = new LinkedHashMap<>();
    if (entries.isEmpty()) {
      for (String path : paths) {
        view.put(path, path);
      }
      return view;
    }
    Set<String> declared = new HashSet<>();
    for (OverlayEntry entry : entries) {
      declared.add(entry.directory());
    }
    Map<String, List<String>> inOverlays = new HashMap<>();
    for (String path : paths) {
      String first = firstStep(path);
      if (declared.contains(first) && path.length() > first.length()) {
        inOverlays.computeIfAbsent(first, key -> new ArrayList<>()).add(path);
      } else {
        view.put(path, path);
      }
    }
    for (OverlayEntry entry : entries) {
      if (entry.formats().compatibilityWith(target) == PackCompatibility.COMPATIBLE) {
        applyOverlay(
            view, inOverlays.getOrDefault(entry.directory(), List.of()), entry.directory());
      }
    }
    return view;
  }

  /**
   * Removes from the lower packs the files that the block filters of a higher pack hide (R47).
   *
   * @throws dev.shoko.minecraft.data.pack.meta.FilterBudgetException when a filter is too costly
   */
  static void hide(Map<String, String> lowerView, List<FilterRule> filters) {
    // Identical rules are evaluated once; the order of the rules does not change the result.
    List<FilterRule> distinct = List.copyOf(new LinkedHashSet<>(filters));
    lowerView.keySet().removeIf(path -> isBlocked(path, distinct));
  }

  private static void applyOverlay(
      Map<String, String> view, List<String> overlayPaths, String directory) {
    int prefix = directory.length() + 1;
    for (String path : overlayPaths) {
      String stacked = path.substring(prefix);
      if (!OVERLAY_IGNORED.contains(stacked)) {
        view.put(stacked, path);
      }
    }
  }

  private static String firstStep(String path) {
    int slash = path.indexOf('/');
    return slash < 0 ? path : path.substring(0, slash);
  }

  private static boolean isBlocked(String path, List<FilterRule> filters) {
    if (!path.startsWith(DATA_PREFIX)) {
      return false;
    }
    int slash = path.indexOf('/', DATA_PREFIX.length());
    if (slash < 0) {
      return false;
    }
    String namespace = path.substring(DATA_PREFIX.length(), slash);
    String rest = path.substring(slash + 1);
    for (FilterRule rule : filters) {
      if (rule.matches(namespace, rest)) {
        return true;
      }
    }
    return false;
  }
}
