package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The codecs that assemble others: lists and unions selected by a field. */
final class StructureCodecs {

  private StructureCodecs() {}

  static <T> Codec<List<T>> list(Codec<T> element) {
    return new FunctionCodec<>(
        (node, access) -> {
          if (!(node instanceof JsonArray array)) {
            return Failures.mismatch(node, "a list");
          }
          List<T> values = new ArrayList<>(array.elements().size());
          List<Diagnostic> diagnostics = new ArrayList<>();
          boolean failed = false;
          for (JsonNode item : array.elements()) {
            Outcome<T> decoded = element.decode(item, access);
            diagnostics.addAll(decoded.diagnostics());
            if (decoded.value().isPresent()) {
              values.add(decoded.value().get());
            } else {
              failed = true;
            }
          }
          return failed
              ? Outcome.failure(diagnostics)
              : Outcome.success(List.copyOf(values), diagnostics);
        });
  }

  static <T> Codec<T> byType(String typeField, Map<Identifier, Codec<? extends T>> variants) {
    Map<Identifier, Codec<? extends T>> byName = Map.copyOf(variants);
    return new FunctionCodec<>(
        (node, access) -> {
          if (!(node instanceof JsonObject object)) {
            return Failures.mismatch(node, "an object");
          }
          JsonNode typeNode = object.members().get(typeField);
          if (typeNode == null) {
            return Outcome.failure(List.of(Failures.missingField(object, typeField)));
          }
          Outcome<Identifier> type = ScalarCodecs.IDENTIFIER.decode(typeNode, access);
          if (type.value().isEmpty()) {
            return Outcome.failure(type.diagnostics());
          }
          Codec<? extends T> variant = byName.get(type.value().get());
          if (variant == null) {
            return Failures.failure(
                DiagnosticCode.CODEC_UNRESOLVED_REFERENCE,
                typeNode,
                "unknown variant " + type.value().get());
          }
          return widen(variant.decode(node, access));
        });
  }

  static Codec<JsonNode> raw() {
    return new FunctionCodec<>((node, access) -> Outcome.success(node));
  }

  private static <T> Outcome<T> widen(Outcome<? extends T> decoded) {
    return decoded
        .value()
        .<Outcome<T>>map(value -> Outcome.success(value, decoded.diagnostics()))
        .orElseGet(() -> Outcome.failure(decoded.diagnostics()));
  }
}
