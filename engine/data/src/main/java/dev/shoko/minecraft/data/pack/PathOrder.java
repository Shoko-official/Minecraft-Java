package dev.shoko.minecraft.data.pack;

import java.util.Comparator;

/** Orders strings by Unicode code point, whatever the locale or the platform (determinism). */
final class PathOrder {

  static final Comparator<String> BY_CODE_POINT = PathOrder::compare;

  private PathOrder() {}

  static int compare(String a, String b) {
    int i = 0;
    int j = 0;
    while (i < a.length() && j < b.length()) {
      int ca = a.codePointAt(i);
      int cb = b.codePointAt(j);
      if (ca != cb) {
        return Integer.compare(ca, cb);
      }
      i += Character.charCount(ca);
      j += Character.charCount(cb);
    }
    return Integer.compare(a.length() - i, b.length() - j);
  }

  /**
   * Tells whether a pack path is relative, uses {@code /} and has no empty, {@code .} or {@code ..}
   * step.
   */
  static boolean isPlainRelative(String path) {
    if (path.isEmpty() || path.startsWith("/") || path.indexOf('\\') >= 0) {
      return false;
    }
    for (String segment : path.split("/", -1)) {
      if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
        return false;
      }
    }
    return true;
  }
}
