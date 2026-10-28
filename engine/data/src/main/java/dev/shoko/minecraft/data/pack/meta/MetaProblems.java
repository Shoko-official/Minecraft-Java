package dev.shoko.minecraft.data.pack.meta;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.json.JsonNode;
import java.util.ArrayList;
import java.util.List;

/** Collects the errors found while reading one {@code pack.mcmeta}, in text order. */
final class MetaProblems {

  private final FileOrigin origin;
  private final List<Diagnostic> found = new ArrayList<>();

  MetaProblems(FileOrigin origin) {
    this.origin = origin;
  }

  boolean any() {
    return !found.isEmpty();
  }

  List<Diagnostic> all() {
    return found;
  }

  void mismatch(JsonNode node, String expected) {
    add(
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        node.path(),
        "expected " + expected + " but found " + node.type());
  }

  void missing(JsonPath parent, String key) {
    add(DiagnosticCode.CODEC_MISSING_FIELD, parent.field(key), "missing field " + key);
  }

  void range(JsonNode node) {
    add(DiagnosticCode.CODEC_NUMBER_RANGE, node.path(), "number out of range");
  }

  void invalidIdentifier(JsonNode node) {
    add(DiagnosticCode.CODEC_INVALID_IDENTIFIER, node.path(), "invalid identifier");
  }

  private void add(DiagnosticCode code, JsonPath path, String message) {
    found.add(new Diagnostic(code, origin, path, message));
  }
}
