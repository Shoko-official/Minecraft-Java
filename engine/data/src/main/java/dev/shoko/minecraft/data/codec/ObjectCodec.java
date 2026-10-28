package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.registry.RegistryAccess;
import java.util.List;
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
    List<Diagnostic> diagnostics = reader.diagnostics();
    if (unknownFields == UnknownFields.REJECT) {
      for (String key : object.members().keySet()) {
        if (!reader.isKnown(key)) {
          diagnostics.add(
              new Diagnostic(
                  DiagnosticCode.CODEC_UNKNOWN_FIELD,
                  object.origin(),
                  object.path().field(key),
                  "unknown field " + key));
        }
      }
    }
    if (reader.failed()) {
      return Outcome.failure(diagnostics);
    }
    return Outcome.success(builder.get(), diagnostics);
  }
}
