package dev.shoko.minecraft.data.codec;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonString;

/** The codecs of strings, booleans, numbers and identifiers (R62). */
final class ScalarCodecs {

  static final Codec<String> STRING =
      new FunctionCodec<>(
          (node, access) ->
              node instanceof JsonString text
                  ? Outcome.success(text.value())
                  : Failures.mismatch(node, "a string"));

  static final Codec<Boolean> BOOL =
      new FunctionCodec<>(
          (node, access) ->
              node instanceof JsonBoolean flag
                  ? Outcome.success(flag.value())
                  : Failures.mismatch(node, "a boolean"));

  static final Codec<Integer> INTEGER =
      new FunctionCodec<>(
          (node, access) -> {
            Outcome<Long> wide = integerForm(node);
            if (wide.value().isEmpty()) {
              return Outcome.failure(wide.diagnostics());
            }
            long value = wide.value().get();
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
              return outOfRange(node);
            }
            return Outcome.success((int) value);
          });

  static final Codec<Long> LONG = new FunctionCodec<>((node, access) -> integerForm(node));

  static final Codec<Double> DECIMAL =
      new FunctionCodec<>(
          (node, access) ->
              node instanceof JsonNumber number
                  ? Outcome.success(number.doubleValue())
                  : Failures.mismatch(node, "a number"));

  static final Codec<Float> FLOAT =
      new FunctionCodec<>(
          (node, access) -> {
            if (!(node instanceof JsonNumber number)) {
              return Failures.mismatch(node, "a number");
            }
            float value = (float) number.doubleValue();
            return Float.isInfinite(value) ? outOfRange(node) : Outcome.success(value);
          });

  static final Codec<Identifier> IDENTIFIER =
      new FunctionCodec<>(
          (node, access) -> {
            if (!(node instanceof JsonString text)) {
              return Failures.mismatch(node, "an identifier");
            }
            return Identifier.tryParse(text.value())
                .map(Outcome::success)
                .orElseGet(() -> invalidIdentifier(node));
          });

  private ScalarCodecs() {}

  static <T> Outcome<T> invalidIdentifier(JsonNode node) {
    return Failures.failure(DiagnosticCode.CODEC_INVALID_IDENTIFIER, node, "invalid identifier");
  }

  private static <T> Outcome<T> outOfRange(JsonNode node) {
    return Failures.failure(DiagnosticCode.CODEC_NUMBER_RANGE, node, "number out of range");
  }

  private static Outcome<Long> integerForm(JsonNode node) {
    if (!(node instanceof JsonNumber number)) {
      return Failures.mismatch(node, "an integer");
    }
    if (!number.isInteger()) {
      return Failures.mismatch(node, "an integer, not a decimal");
    }
    try {
      return Outcome.success(number.longValue());
    } catch (NumberFormatException e) {
      return outOfRange(node);
    }
  }
}
