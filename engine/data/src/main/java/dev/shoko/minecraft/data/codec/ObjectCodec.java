package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/** The codec of an object read field by field (R56, R62 g, R63). */
final class ObjectCodec<T> extends Codec<T> {

  private final UnknownFields unknownFields;
  private final Function<ObjectReader, Supplier<T>> reading;

  ObjectCodec(UnknownFields unknownFields, Function<ObjectReader, Supplier<T>> reading) {
    this.unknownFields = unknownFields;
    this.reading = reading;
  }

  @Override
  public Outcome<T> decode(JsonNode node, RegistryAccess access) {
    if (!(node instanceof JsonObject object)) {
      return Failures.mismatch(node, "an object");
    }
    ObjectReader reader = new ObjectReader(object, access, unknownFields);
    Supplier<T> builder = reading.apply(reader);
    List<Diagnostic> diagnostics = inTextOrder(object, reader);
    if (reader.failed()) {
      return Outcome.failure(diagnostics);
    }
    return Outcome.success(builder.get(), diagnostics);
  }

  /** Orders the diagnostics by the place of their member in the object, absent members last. */
  private List<Diagnostic> inTextOrder(JsonObject object, ObjectReader reader) {
    List<ObjectReader.Found> found = new ArrayList<>(reader.found());
    if (unknownFields == UnknownFields.REJECT) {
      for (String key : object.members().keySet()) {
        if (!reader.isKnown(key)) {
          Diagnostic unknown =
              new Diagnostic(
                  DiagnosticCode.CODEC_UNKNOWN_FIELD,
                  object.origin(),
                  object.path().field(key),
                  "unknown field " + key);
          found.add(new ObjectReader.Found(key, List.of(unknown)));
        }
      }
    }
    Map<String, Integer> places = new HashMap<>();
    for (String key : object.members().keySet()) {
      places.put(key, places.size());
    }
    // The sort is stable: members with the same place, or absent, keep the order of the calls.
    found.sort(
        Comparator.comparingInt(
            f -> f.member() == null ? Integer.MAX_VALUE : places.get(f.member())));
    List<Diagnostic> ordered = new ArrayList<>();
    found.forEach(f -> ordered.addAll(f.diagnostics()));
    return ordered;
  }
}
