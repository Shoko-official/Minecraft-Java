package dev.shoko.minecraft.data.pack.meta;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/** Reads the optional sections of {@code pack.mcmeta}: features, filter and overlays. */
final class MetaSections {

  private final MetaProblems problems;
  private final List<Identifier> flags = new ArrayList<>();
  private final List<FilterRule> filters = new ArrayList<>();
  private final List<OverlayEntry> overlays = new ArrayList<>();

  MetaSections(MetaProblems problems) {
    this.problems = problems;
  }

  FeatureFlags flags() {
    return FeatureFlags.of(flags);
  }

  List<FilterRule> filters() {
    return filters;
  }

  List<OverlayEntry> overlays() {
    return overlays;
  }

  void features(JsonNode node) {
    JsonArray list = listAt(node, "enabled");
    if (list == null) {
      return;
    }
    for (JsonNode entry : list.elements()) {
      if (!(entry instanceof JsonString text)) {
        problems.mismatch(entry, "a string");
      } else {
        Identifier.tryParse(text.value())
            .ifPresentOrElse(flags::add, () -> problems.invalidIdentifier(entry));
      }
    }
  }

  void filter(JsonNode node) {
    JsonArray list = listAt(node, "block");
    if (list == null) {
      return;
    }
    for (JsonNode entry : list.elements()) {
      JsonObject rule = object(entry);
      if (rule != null) {
        filterRule(rule);
      }
    }
  }

  void overlays(JsonNode node) {
    JsonArray list = listAt(node, "entries");
    if (list == null) {
      return;
    }
    for (JsonNode entry : list.elements()) {
      JsonObject overlay = object(entry);
      if (overlay != null) {
        overlayEntry(overlay);
      }
    }
  }

  private void filterRule(JsonObject rule) {
    String namespace = pattern(rule, "namespace");
    String path = pattern(rule, "path");
    if (namespace != null && path != null) {
      filters.add(FilterRule.of(namespace, path));
    }
  }

  private String pattern(JsonObject rule, String key) {
    JsonNode value = rule.members().get(key);
    if (value == null) {
      return FilterRule.MATCH_ALL;
    }
    if (!(value instanceof JsonString text)) {
      problems.mismatch(value, "a string");
      return null;
    }
    try {
      FilterRule.compile(text.value());
    } catch (PatternSyntaxException e) {
      problems.mismatch(value, "a valid regular expression");
      return null;
    }
    return text.value();
  }

  private void overlayEntry(JsonObject entry) {
    String directory = directory(entry);
    PackFormatRange formats = overlayFormats(entry);
    if (directory != null && formats != null) {
      overlays.add(new OverlayEntry(directory, formats));
    }
  }

  private String directory(JsonObject entry) {
    JsonNode value = entry.members().get("directory");
    if (value == null) {
      problems.missing(entry.path(), "directory");
      return null;
    }
    if (!(value instanceof JsonString text)) {
      problems.mismatch(value, "a string");
      return null;
    }
    if (!validDirectory(text.value())) {
      problems.mismatch(value, "a directory name using a-z 0-9 _ - .");
      return null;
    }
    return text.value();
  }

  private PackFormatRange overlayFormats(JsonObject entry) {
    JsonNode formats = entry.members().get("formats");
    if (formats != null) {
      return FormatValues.range(formats, problems);
    }
    FormatValues.Bound min = bound(entry, "min_format");
    FormatValues.Bound max = bound(entry, "max_format");
    return min == null || max == null ? null : new PackFormatRange(min.asMin(), max.asMax());
  }

  private FormatValues.Bound bound(JsonObject entry, String key) {
    JsonNode value = entry.members().get(key);
    if (value == null) {
      problems.missing(entry.path(), key);
      return null;
    }
    return FormatValues.bound(value, problems);
  }

  private static boolean validDirectory(String name) {
    if (name.isEmpty()) {
      return false;
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      boolean allowed =
          (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.';
      if (!allowed) {
        return false;
      }
    }
    return true;
  }

  /** Returns the list stored under {@code key} of the object {@code node}, or null. */
  private JsonArray listAt(JsonNode node, String key) {
    JsonObject object = object(node);
    JsonNode value = object == null ? null : object.members().get(key);
    if (value == null) {
      return null;
    }
    if (value instanceof JsonArray array) {
      return array;
    }
    problems.mismatch(value, "a list");
    return null;
  }

  private JsonObject object(JsonNode node) {
    if (node instanceof JsonObject object) {
      return object;
    }
    problems.mismatch(node, "an object");
    return null;
  }
}
