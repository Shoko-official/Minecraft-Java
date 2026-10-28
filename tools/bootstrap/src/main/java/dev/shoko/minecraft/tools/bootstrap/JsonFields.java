package dev.shoko.minecraft.tools.bootstrap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;

/**
 * Typed reads of downloaded JSON documents. A missing or mistyped field is an {@link IOException}
 * naming the document and the field, never a {@code NullPointerException}.
 */
final class JsonFields {

  private JsonFields() {}

  static JsonObject parseObject(String text, String document) throws IOException {
    try {
      JsonElement root = JsonParser.parseString(text);
      if (!root.isJsonObject()) {
        throw new IOException(document + " is not a JSON object");
      }
      return root.getAsJsonObject();
    } catch (JsonParseException e) {
      throw new IOException(document + " is not valid JSON: " + e.getMessage(), e);
    }
  }

  static JsonObject object(JsonObject parent, String name, String document) throws IOException {
    JsonElement element = parent.get(name);
    if (element == null || !element.isJsonObject()) {
      throw missing(document, name, "an object");
    }
    return element.getAsJsonObject();
  }

  static String string(JsonObject parent, String name, String document) throws IOException {
    JsonPrimitive primitive = primitive(parent, name);
    if (primitive == null || !primitive.isString()) {
      throw missing(document, name, "a string");
    }
    return primitive.getAsString();
  }

  static long integer(JsonObject parent, String name, String document) throws IOException {
    JsonPrimitive primitive = primitive(parent, name);
    if (primitive == null || !primitive.isNumber()) {
      throw missing(document, name, "an integer");
    }
    try {
      return primitive.getAsBigDecimal().longValueExact();
    } catch (ArithmeticException | NumberFormatException e) {
      throw new IOException(document + ": field " + name + " is not an integer", e);
    }
  }

  private static JsonPrimitive primitive(JsonObject parent, String name) {
    JsonElement element = parent.get(name);
    return element != null && element.isJsonPrimitive() ? element.getAsJsonPrimitive() : null;
  }

  private static IOException missing(String document, String name, String type) {
    return new IOException(document + ": field " + name + " is not " + type);
  }
}
