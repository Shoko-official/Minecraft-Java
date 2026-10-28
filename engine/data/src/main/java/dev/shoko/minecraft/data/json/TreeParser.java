package dev.shoko.minecraft.data.json;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonToken;
import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.diag.Outcome;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Builds the tree of one already decoded document with the streaming reader of the library, in
 * strict mode (ADR 0006). The only class that touches the library; every library exception becomes
 * a diagnostic. Not thread-safe: one instance per document.
 */
final class TreeParser {

  private final com.google.gson.stream.JsonReader in;
  private final FileOrigin origin;
  private final List<Diagnostic> warnings = new ArrayList<>();
  // Where the reader is, for the indicative path of a syntax error.
  private JsonPath where = JsonPath.root();
  private final LongNumbers longNumbers;
  // Rank of the next number of the text, to match the long literals taken out of it.
  private int numbers;

  private TreeParser(String text, FileOrigin origin) {
    this.longNumbers = LongNumbers.scan(text);
    this.in = new com.google.gson.stream.JsonReader(new StringReader(longNumbers.libraryText()));
    this.in.setStrictness(Strictness.STRICT);
    this.in.setNestingLimit(JsonReader.MAX_DEPTH);
    this.origin = origin;
  }

  static Outcome<JsonNode> parse(String text, FileOrigin origin) {
    TreeParser parser = new TreeParser(text, origin);
    try {
      JsonNode root = parser.value(JsonPath.root(), 0);
      parser.expectEnd();
      return Outcome.success(root, parser.warnings);
    } catch (ParseFailure failure) {
      return parser.invalid(failure.code(), failure.path());
    } catch (IOException | RuntimeException e) {
      return parser.invalid(DiagnosticCode.JSON_SYNTAX, parser.where);
    }
  }

  private Outcome<JsonNode> invalid(DiagnosticCode code, JsonPath path) {
    String message = "invalid JSON (" + code + ") at " + path;
    return Outcome.failure(List.of(new Diagnostic(code, origin, path, message)));
  }

  private void expectEnd() throws ParseFailure {
    try {
      if (in.peek() == JsonToken.END_DOCUMENT) {
        return;
      }
    } catch (IOException | RuntimeException e) {
      // Fall through: whatever the reader finds after the root, it is not the end of the file.
    }
    throw new ParseFailure(DiagnosticCode.JSON_TRAILING, JsonPath.root());
  }

  private JsonNode value(JsonPath path, int depth) throws IOException, ParseFailure {
    where = path;
    return switch (in.peek()) {
      case BEGIN_OBJECT -> object(path, depth);
      case BEGIN_ARRAY -> array(path, depth);
      case STRING -> new JsonString(origin, path, in.nextString());
      case NUMBER -> number(path);
      case BOOLEAN -> new JsonBoolean(origin, path, in.nextBoolean());
      case NULL -> {
        in.nextNull();
        yield new JsonNull(origin, path);
      }
      default -> throw new ParseFailure(DiagnosticCode.JSON_SYNTAX, path);
    };
  }

  private JsonObject object(JsonPath path, int depth) throws IOException, ParseFailure {
    checkDepth(path, depth);
    in.beginObject();
    LinkedHashMap<String, JsonNode> members = new LinkedHashMap<>();
    while (in.hasNext()) {
      String key = in.nextName();
      JsonPath child = path.field(key);
      if (members.containsKey(key)) {
        String message = "duplicate key at " + child;
        warnings.add(new Diagnostic(DiagnosticCode.JSON_DUPLICATE_KEY, origin, child, message));
      }
      // put keeps the place of the first occurrence and stores the last value (R58 b)
      members.put(key, value(child, depth + 1));
    }
    in.endObject();
    return new JsonObject(origin, path, Collections.unmodifiableSequencedMap(members));
  }

  private JsonArray array(JsonPath path, int depth) throws IOException, ParseFailure {
    checkDepth(path, depth);
    in.beginArray();
    List<JsonNode> elements = new ArrayList<>();
    while (in.hasNext()) {
      elements.add(value(path.index(elements.size()), depth + 1));
    }
    in.endArray();
    return new JsonArray(origin, path, Collections.unmodifiableList(elements));
  }

  private static void checkDepth(JsonPath path, int depth) throws ParseFailure {
    if (depth >= JsonReader.MAX_DEPTH) {
      throw new ParseFailure(DiagnosticCode.JSON_TOO_DEEP, path);
    }
  }

  private JsonNumber number(JsonPath path) throws IOException, ParseFailure {
    String read = in.nextString();
    String longLiteral = longNumbers.literalAt(numbers++);
    if (longLiteral != null && !LongNumbers.isNumber(longLiteral)) {
      throw new ParseFailure(DiagnosticCode.JSON_SYNTAX, path);
    }
    JsonNumber number = new JsonNumber(origin, path, longLiteral != null ? longLiteral : read);
    try {
      if (number.isInteger()) {
        Long.parseLong(number.literal());
      } else if (Double.isInfinite(number.doubleValue())) {
        throw new ParseFailure(DiagnosticCode.JSON_NUMBER_RANGE, path);
      }
    } catch (NumberFormatException e) {
      throw new ParseFailure(DiagnosticCode.JSON_NUMBER_RANGE, path);
    }
    return number;
  }
}
