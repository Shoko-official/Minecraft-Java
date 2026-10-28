package dev.shoko.minecraft.data.pack.meta;

import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import java.util.List;

/**
 * Reads a written format version: an integer {@code N}, {@code [N]} or {@code [major, minor]} (R5).
 */
final class FormatValues {

  /** A written bound: the minor number is absent for {@code N} and {@code [N]}. */
  record Bound(int major, int minor, boolean hasMinor) {

    PackFormat asMin() {
      return new PackFormat(major, hasMinor ? minor : 0);
    }

    PackFormat asMax() {
      return new PackFormat(major, hasMinor ? minor : Integer.MAX_VALUE);
    }
  }

  private FormatValues() {}

  /** Returns the bound, or {@code null} after reporting the problem. */
  static Bound bound(JsonNode node, MetaProblems problems) {
    if (node instanceof JsonNumber) {
      Integer major = integer(node, problems);
      return major == null ? null : new Bound(major, 0, false);
    }
    if (!(node instanceof JsonArray array)) {
      problems.mismatch(node, "an integer or a list of integers");
      return null;
    }
    List<JsonNode> elements = array.elements();
    if (elements.isEmpty() || elements.size() > 2) {
      problems.mismatch(node, "a list of one or two integers");
      return null;
    }
    Integer major = integer(elements.get(0), problems);
    Integer minor = elements.size() == 2 ? integer(elements.get(1), problems) : Integer.valueOf(0);
    if (major == null || minor == null) {
      return null;
    }
    return new Bound(major, minor, elements.size() == 2);
  }

  /**
   * Reads the {@code formats} field of an overlay entry: a single bound, a list of two bounds, or
   * an object with {@code min_inclusive} and {@code max_inclusive}. Returns {@code null} after
   * reporting the problem.
   */
  static PackFormatRange range(JsonNode node, MetaProblems problems) {
    if (node instanceof JsonObject object) {
      Bound min = member(object, "min_inclusive", problems);
      Bound max = member(object, "max_inclusive", problems);
      return min == null || max == null ? null : new PackFormatRange(min.asMin(), max.asMax());
    }
    if (node instanceof JsonArray array && array.elements().size() == 2) {
      Bound min = bound(array.elements().get(0), problems);
      Bound max = bound(array.elements().get(1), problems);
      return min == null || max == null ? null : new PackFormatRange(min.asMin(), max.asMax());
    }
    Bound single = bound(node, problems);
    return single == null ? null : new PackFormatRange(single.asMin(), single.asMax());
  }

  private static Bound member(JsonObject object, String key, MetaProblems problems) {
    JsonNode value = object.members().get(key);
    if (value == null) {
      problems.missing(object.path(), key);
      return null;
    }
    return bound(value, problems);
  }

  private static Integer integer(JsonNode node, MetaProblems problems) {
    if (!(node instanceof JsonNumber number) || !number.isInteger()) {
      problems.mismatch(node, "an integer");
      return null;
    }
    long value = number.longValue();
    if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
      problems.range(node);
      return null;
    }
    return (int) value;
  }
}
