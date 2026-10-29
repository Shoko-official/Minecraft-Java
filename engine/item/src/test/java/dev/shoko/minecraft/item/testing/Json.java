package dev.shoko.minecraft.item.testing;

import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonReader;

/** Reads JSON written in Java strings: single quotes stand for double quotes. */
public final class Json {

  public static final FileOrigin ORIGIN = new FileOrigin("test", "data/test/x.json");

  private Json() {}

  /** Parses a document where {@code '} replaces {@code "}; the text itself must not hold quotes. */
  public static JsonNode parse(String relaxed) {
    return parseStrict(relaxed.replace('\'', '"'));
  }

  public static JsonNode parseStrict(String text) {
    return JsonReader.read(text, ORIGIN).value().orElseThrow();
  }
}
