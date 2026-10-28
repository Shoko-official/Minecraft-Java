package dev.shoko.minecraft.data.pack;

import dev.shoko.minecraft.data.pack.meta.FilterRule;
import dev.shoko.minecraft.data.pack.meta.OverlayEntry;
import dev.shoko.minecraft.data.pack.meta.PackCompatibility;
import dev.shoko.minecraft.data.pack.meta.PackFormat;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The files of one pack once its active overlays are applied (R48): each stacked path maps to the
 * physical path that provides it. Overlay directories are not part of the base tree.
 */
final class PackView {

  private static final String DATA_PREFIX = "data/";
  private static final Set<String> OVERLAY_IGNORED = Set.of("pack.mcmeta", "pack.png");

  private PackView() {}

  static Map<String, String> of(Pack pack, PackFormat target) throws IOException {
    List<String> paths = pack.files().paths();
    List<OverlayEntry> entries = pack.meta().overlays();
    Set<String> declared = new HashSet<>();
    for (OverlayEntry entry : entries) {
      declared.add(entry.directory() + "/");
    }
    Map<String, String> view = new LinkedHashMap<>();
    for (String path : paths) {
      if (!isInside(path, declared)) {
        view.put(path, path);
      }
    }
    for (OverlayEntry entry : entries) {
      if (entry.formats().compatibilityWith(target) == PackCompatibility.COMPATIBLE) {
        applyOverlay(view, paths, entry.directory() + "/");
      }
    }
    return view;
  }

  /** Removes from the lower packs the files that the block filters of a higher pack hide (R47). */
  static void hide(Map<String, String> lowerView, List<FilterRule> filters) {
    lowerView.keySet().removeIf(path -> isBlocked(path, filters));
  }

  private static void applyOverlay(Map<String, String> view, List<String> paths, String prefix) {
    for (String path : paths) {
      if (path.startsWith(prefix)) {
        String stacked = path.substring(prefix.length());
        if (!OVERLAY_IGNORED.contains(stacked)) {
          view.put(stacked, path);
        }
      }
    }
  }

  private static boolean isInside(String path, Set<String> directories) {
    for (String directory : directories) {
      if (path.startsWith(directory)) {
        return true;
      }
    }
    return false;
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
