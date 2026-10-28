package dev.shoko.minecraft.data.testing;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonReader;
import java.nio.charset.StandardCharsets;

/** Short helpers to read JSON text in tests; the text is always UTF-8 encoded before reading. */
public final class Jx {

  /** Origin given to every document read through this class. */
  public static final FileOrigin ORIGIN = new FileOrigin("test", "data/test/x.json");

  private Jx() {}

  public static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  /** Builds a byte array from the low 8 bits of each value, so that 0xED can be written as is. */
  public static byte[] bytes(int... values) {
    byte[] result = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = (byte) values[i];
    }
    return result;
  }

  public static byte[] concat(byte[]... parts) {
    int length = 0;
    for (byte[] part : parts) {
      length += part.length;
    }
    byte[] result = new byte[length];
    int offset = 0;
    for (byte[] part : parts) {
      System.arraycopy(part, 0, result, offset, part.length);
      offset += part.length;
    }
    return result;
  }

  public static Outcome<JsonNode> read(String text) {
    return JsonReader.read(utf8(text), ORIGIN);
  }

  public static Outcome<JsonNode> read(byte[] utf8) {
    return JsonReader.read(utf8, ORIGIN);
  }

  /** Reads a document that must be valid and free of any diagnostic. */
  public static JsonNode parse(String text) {
    Outcome<JsonNode> outcome = read(text);
    assertThat(outcome.diagnostics()).as("diagnostics of %s", text).isEmpty();
    return outcome.value().orElseThrow();
  }

  /** Reads a document that must be valid; warnings are allowed. */
  public static JsonNode parseLenient(String text) {
    Outcome<JsonNode> outcome = read(text);
    assertThat(outcome.hasErrors()).as("errors of %s", text).isFalse();
    return outcome.value().orElseThrow();
  }
}
