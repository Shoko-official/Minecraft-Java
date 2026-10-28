package dev.shoko.minecraft.data.tag;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import java.util.ArrayList;
import java.util.List;

/** Reads the JSON of a tag file into a {@link TagFile} (R32). */
public final class TagFileReader {

  private final JsonNode root;
  private final List<Diagnostic> errors = new ArrayList<>();

  private TagFileReader(JsonNode root) {
    this.root = root;
  }

  /**
   * Reads a tag file.
   *
   * <p>The root is an object with an optional boolean {@code replace} (default {@code false}) and a
   * {@code values} array. An entry is a string, an identifier or {@code #tag} (R22, R23), or an
   * object {@code {"id": string, "required": boolean}} where {@code required} defaults to {@code
   * true}. A key unknown to the file, or to an object entry, is ignored without diagnostic (R32,
   * R63 c).
   *
   * <p>The file is invalid, and counts as absent (R32, R54, provisional), when:
   *
   * <ul>
   *   <li>the root is not an object, {@code values} or {@code replace} has another type, an entry
   *       is neither a string nor an object, or an object entry has an {@code id} or a {@code
   *       required} of another type: {@code CODEC_TYPE_MISMATCH} at the path of the value;
   *   <li>{@code values} is absent, or an object entry has no {@code id}: {@code
   *       CODEC_MISSING_FIELD};
   *   <li>an identifier or {@code #} reference breaks R20: {@code CODEC_INVALID_IDENTIFIER} at
   *       {@code $.values[i]} or {@code $.values[i].id}.
   * </ul>
   *
   * All the errors of the file are collected, in text order (R62 g).
   *
   * @param root the parsed file
   * @return the tag file, or empty when it is invalid, with the diagnostics found
   */
  public static Outcome<TagFile> read(JsonNode root) {
    return new TagFileReader(root).run();
  }

  private Outcome<TagFile> run() {
    if (!(root instanceof JsonObject object)) {
      error(DiagnosticCode.CODEC_TYPE_MISMATCH, root.path(), "expected an object");
      return Outcome.failure(errors);
    }
    boolean replace = false;
    List<TagEntry> entries = new ArrayList<>();
    for (var member : object.members().entrySet()) {
      JsonNode value = member.getValue();
      switch (member.getKey()) {
        case "replace" -> {
          if (value instanceof JsonBoolean flag) {
            replace = flag.value();
          } else {
            error(DiagnosticCode.CODEC_TYPE_MISMATCH, value.path(), "expected a boolean");
          }
        }
        case "values" -> readValues(value, entries);
        default -> {
          // unknown keys are ignored (R32, R63 c)
        }
      }
    }
    if (!object.members().containsKey("values")) {
      error(DiagnosticCode.CODEC_MISSING_FIELD, object.path().field("values"), "missing values");
    }
    return errors.isEmpty()
        ? Outcome.success(new TagFile(replace, List.copyOf(entries)))
        : Outcome.failure(errors);
  }

  private void readValues(JsonNode values, List<TagEntry> entries) {
    if (!(values instanceof JsonArray array)) {
      error(DiagnosticCode.CODEC_TYPE_MISMATCH, values.path(), "expected a list");
      return;
    }
    for (JsonNode node : array.elements()) {
      TagEntry entry = readEntry(node);
      if (entry != null) {
        entries.add(entry);
      }
    }
  }

  /** Returns the entry, or null after reporting why it is invalid. */
  private TagEntry readEntry(JsonNode node) {
    if (node instanceof JsonString text) {
      return reference(text, true);
    }
    if (!(node instanceof JsonObject object)) {
      error(DiagnosticCode.CODEC_TYPE_MISMATCH, node.path(), "expected a string or an object");
      return null;
    }
    JsonNode id = object.members().get("id");
    TagEntry entry = null;
    boolean required = true;
    if (id == null) {
      error(DiagnosticCode.CODEC_MISSING_FIELD, object.path().field("id"), "missing id");
    }
    JsonNode flag = object.members().get("required");
    if (flag instanceof JsonBoolean bool) {
      required = bool.value();
    } else if (flag != null) {
      error(DiagnosticCode.CODEC_TYPE_MISMATCH, flag.path(), "expected a boolean");
    }
    if (id instanceof JsonString text) {
      entry = reference(text, required);
    } else if (id != null) {
      error(DiagnosticCode.CODEC_TYPE_MISMATCH, id.path(), "expected a string");
    }
    return errors.isEmpty() ? entry : null;
  }

  private TagEntry reference(JsonString text, boolean required) {
    String value = text.value();
    boolean tag = value.startsWith("#");
    var id = Identifier.tryParse(tag ? value.substring(1) : value);
    if (id.isEmpty()) {
      error(DiagnosticCode.CODEC_INVALID_IDENTIFIER, text.path(), "invalid identifier");
      return null;
    }
    return new TagEntry(id.get(), tag, required);
  }

  private void error(DiagnosticCode code, JsonPath path, String message) {
    errors.add(new Diagnostic(code, root.origin(), path, message));
  }
}
