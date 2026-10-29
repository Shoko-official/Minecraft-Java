package dev.shoko.minecraft.loot.testing;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonReader;
import java.util.Optional;

/** Reads JSON written in Java strings: single quotes stand for double quotes. */
public final class Json {

  private static final FileOrigin ORIGIN = new FileOrigin("test", "data/test/x.json");

  private Json() {}

  /** Parses a document where a single quote replaces a double quote; the text holds no quotes. */
  public static JsonNode parse(String relaxed) {
    return parseStrict(relaxed.replace('\'', '"'));
  }

  /** Parses a strict JSON text; fails the test when the text is not valid JSON. */
  public static JsonNode parseStrict(String text) {
    return JsonReader.read(text, ORIGIN).value().orElseThrow();
  }

  /** Parses a strict JSON text; empty when the reader refuses it. */
  public static Optional<JsonNode> tryParse(String text) {
    return JsonReader.read(text, ORIGIN).value();
  }
}
