package dev.shoko.minecraft.architecture.fixture.good.data.json;

import com.google.gson.stream.JsonReader;
import java.io.StringReader;

/**
 * Gson confined to the json package: used in bodies, and in private or package-private members, but
 * absent from every public or protected signature.
 */
public final class GsonInsideJson {
  private final JsonReader reader = new JsonReader(new StringReader("{}"));

  public String text() {
    return reader.toString();
  }

  JsonReader reader() {
    return reader;
  }
}
