package dev.shoko.minecraft.tools.diff;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Builds hierarchical keys of canonical states, such as {@code
 * chunk(2,-1)/section(4)/block(34,70,-12)}: segments joined by {@value #SEPARATOR}, each a name
 * with optional integer coordinates.
 */
public final class StateKey {

  public static final String SEPARATOR = "/";

  private StateKey() {}

  /** One segment, as {@code block(34,70,-12)}, or the bare name when there are no coordinates. */
  public static String segment(String name, int... coordinates) {
    checkSegment(name);
    if (coordinates.length == 0) {
      return name;
    }
    return Arrays.stream(coordinates)
        .mapToObj(Integer::toString)
        .collect(Collectors.joining(",", name + "(", ")"));
  }

  /** Joins segments, or keys made of segments, into one key. */
  public static String of(String... parts) {
    if (parts.length == 0) {
      throw new IllegalArgumentException("a key needs at least one segment");
    }
    for (String part : parts) {
      checkKey(part);
    }
    return String.join(SEPARATOR, parts);
  }

  /** The first {@code depth} segments of a key, or the whole key when it is shorter. */
  static String prefix(String key, int depth) {
    int end = -1;
    for (int i = 0; i < depth; i++) {
      end = key.indexOf(SEPARATOR, end + 1);
      if (end < 0) {
        return key;
      }
    }
    return key.substring(0, end);
  }

  /** True when {@code key} is {@code prefix} or lies under it, segment by segment. */
  static boolean isUnder(String key, String prefix) {
    return key.equals(prefix) || key.startsWith(prefix + SEPARATOR);
  }

  static void checkKey(String key) {
    for (String segment : key.split(SEPARATOR, -1)) {
      checkSegment(segment);
    }
  }

  private static void checkSegment(String segment) {
    if (segment.isEmpty() || segment.contains(SEPARATOR) || segment.contains("\n")) {
      throw new IllegalArgumentException("invalid key segment: '" + segment + "'");
    }
  }
}
