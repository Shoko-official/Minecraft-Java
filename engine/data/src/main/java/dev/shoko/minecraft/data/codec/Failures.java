package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import java.util.List;

/** Builds the error results of the codecs. */
final class Failures {

  private Failures() {}

  static Diagnostic diagnostic(DiagnosticCode code, JsonNode node, String message) {
    return new Diagnostic(code, node.origin(), node.path(), message);
  }

  static Diagnostic missingField(JsonNode object, String name) {
    JsonPath path = object.path().field(name);
    return new Diagnostic(
        DiagnosticCode.CODEC_MISSING_FIELD, object.origin(), path, "missing " + name);
  }

  static <T> Outcome<T> failure(DiagnosticCode code, JsonNode node, String message) {
    return Outcome.failure(List.of(diagnostic(code, node, message)));
  }

  static <T> Outcome<T> mismatch(JsonNode node, String expected) {
    return failure(
        DiagnosticCode.CODEC_TYPE_MISMATCH,
        node,
        "expected " + expected + " but found " + node.type());
  }
}
