package dev.shoko.minecraft.data.catalog;

import dev.shoko.minecraft.data.codec.Codec;
import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import java.util.List;
import java.util.Optional;

/** Reads single fields of the objects of the extracted reports, collecting the errors. */
final class ReportFields {

  private ReportFields() {}

  static <T> Optional<T> required(
      JsonObject object, String key, Codec<T> codec, List<Diagnostic> errors) {
    JsonNode value = object.members().get(key);
    if (value == null) {
      errors.add(
          new Diagnostic(
              DiagnosticCode.CODEC_MISSING_FIELD,
              object.origin(),
              object.path().field(key),
              "missing " + key));
      return Optional.empty();
    }
    return decode(value, codec, errors);
  }

  static <T> Optional<T> optional(
      JsonObject object, String key, Codec<T> codec, List<Diagnostic> errors) {
    JsonNode value = object.members().get(key);
    return value == null ? Optional.empty() : decode(value, codec, errors);
  }

  private static <T> Optional<T> decode(JsonNode value, Codec<T> codec, List<Diagnostic> errors) {
    Outcome<T> outcome = codec.decode(value, RegistryAccess.empty());
    errors.addAll(outcome.diagnostics());
    return outcome.value();
  }
}
