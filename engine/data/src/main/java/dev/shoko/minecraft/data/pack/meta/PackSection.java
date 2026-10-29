package dev.shoko.minecraft.data.pack.meta;

import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;

/** Reads the {@code pack} object of {@code pack.mcmeta}: description and format range. */
final class PackSection {

  private final MetaProblems problems;
  private JsonNode description;
  private PackFormatRange formats;

  PackSection(MetaProblems problems) {
    this.problems = problems;
  }

  JsonNode description() {
    return description;
  }

  PackFormatRange formats() {
    return formats;
  }

  void read(JsonObject pack) {
    FormatValues.Bound min = null;
    FormatValues.Bound max = null;
    for (var member : pack.members().entrySet()) {
      JsonNode value = member.getValue();
      switch (member.getKey()) {
        case "description" -> description = value;
        case "min_format" -> min = FormatValues.bound(value, problems);
        case "max_format" -> max = FormatValues.bound(value, problems);
        case "pack_format" -> FormatValues.bound(value, problems);
        case "supported_formats" -> FormatValues.range(value, problems);
        default -> {
          // unknown keys are ignored (R4, R63 c)
        }
      }
    }
    if (!pack.members().containsKey("min_format")) {
      problems.missing(pack.path(), "min_format");
    }
    if (!pack.members().containsKey("max_format")) {
      problems.missing(pack.path(), "max_format");
    }
    if (min != null && max != null) {
      formats = new PackFormatRange(min.asMin(), max.asMax());
    }
  }
}
