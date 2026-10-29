package dev.shoko.minecraft.data.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNull;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.data.testing.Jx;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: data-datapack-loading R49 to R60. Differential test of the reader against a strict RFC 8259
// parser written here, on generated documents that glue long numbers to every kind of delimiter,
// then damaged by one character. Fixed seed.
class JsonGrammarHuntTest {

  private static final int DOCUMENTS = 5_000;
  private static final int MAX_DEPTH = 255;

  /** Strict reference parser; answers a canonical text, or null when the text is invalid. */
  private static final class Reference {
    private final String s;
    private int p;
    private int depth;
    private String lastString;

    private Reference(String text) {
      this.s = text;
    }

    static String parse(String text) {
      Reference r = new Reference(text);
      try {
        r.blanks();
        String value = r.value();
        r.blanks();
        return r.p == text.length() ? value : null;
      } catch (IllegalStateException e) {
        return null;
      }
    }

    private static IllegalStateException invalid() {
      return new IllegalStateException();
    }

    private boolean at(char c) {
      return p < s.length() && s.charAt(p) == c;
    }

    private void blanks() {
      while (p < s.length() && " \t\n\r".indexOf(s.charAt(p)) >= 0) {
        p++;
      }
    }

    private String value() {
      if (p >= s.length()) {
        throw invalid();
      }
      if (at('{')) {
        return object();
      }
      if (at('[')) {
        return array();
      }
      if (at('"')) {
        return "S" + string().length() + ":" + lastString;
      }
      for (String word : new String[] {"true", "false", "null"}) {
        if (s.startsWith(word, p)) {
          p += word.length();
          return word.substring(0, 1).toUpperCase(Locale.ROOT);
        }
      }
      return number();
    }

    private String string() {
      StringBuilder out = new StringBuilder();
      p++;
      while (true) {
        if (p >= s.length()) {
          throw invalid();
        }
        char c = s.charAt(p++);
        if (c == '"') {
          break;
        }
        if (c < 0x20) {
          throw invalid();
        }
        out.append(c == '\\' ? escape() : c);
      }
      lastString = out.toString();
      return lastString;
    }

    private char escape() {
      if (p >= s.length()) {
        throw invalid();
      }
      char e = s.charAt(p++);
      return switch (e) {
        case '"', '\\', '/' -> e;
        case 'b' -> '\b';
        case 'f' -> '\f';
        case 'n' -> '\n';
        case 'r' -> '\r';
        case 't' -> '\t';
        case 'u' -> unicode();
        default -> throw invalid();
      };
    }

    private char unicode() {
      if (p + 4 > s.length()) {
        throw invalid();
      }
      int v = 0;
      for (int i = 0; i < 4; i++) {
        int d = "0123456789abcdefABCDEF".indexOf(s.charAt(p + i));
        if (d < 0) {
          throw invalid();
        }
        v = v * 16 + (d > 15 ? d - 6 : d);
      }
      p += 4;
      return (char) v;
    }

    private String object() {
      enter();
      Map<String, String> members = new LinkedHashMap<>();
      blanks();
      if (at('}')) {
        p++;
        depth--;
        return "{}";
      }
      while (true) {
        blanks();
        if (!at('"')) {
          throw invalid();
        }
        String key = string();
        blanks();
        if (!at(':')) {
          throw invalid();
        }
        p++;
        blanks();
        members.put(key, value());
        blanks();
        if (p >= s.length()) {
          throw invalid();
        }
        char c = s.charAt(p++);
        if (c == '}') {
          break;
        }
        if (c != ',') {
          throw invalid();
        }
      }
      depth--;
      StringBuilder out = new StringBuilder("{");
      members.forEach((k, v) -> out.append(k.length()).append('=').append(k).append(':').append(v));
      return out.append('}').toString();
    }

    private String array() {
      enter();
      StringBuilder out = new StringBuilder("[");
      blanks();
      if (at(']')) {
        p++;
        depth--;
        return "[]";
      }
      while (true) {
        blanks();
        out.append(value()).append(';');
        blanks();
        if (p >= s.length()) {
          throw invalid();
        }
        char c = s.charAt(p++);
        if (c == ']') {
          break;
        }
        if (c != ',') {
          throw invalid();
        }
      }
      depth--;
      return out.append(']').toString();
    }

    private void enter() {
      if (++depth > MAX_DEPTH) {
        throw invalid();
      }
      p++;
    }

    private int digits() {
      int start = p;
      while (p < s.length() && s.charAt(p) >= '0' && s.charAt(p) <= '9') {
        p++;
      }
      return p - start;
    }

    private String number() {
      int start = p;
      if (at('-')) {
        p++;
      }
      if (p >= s.length() || s.charAt(p) < '0' || s.charAt(p) > '9') {
        throw invalid();
      }
      if (at('0')) {
        p++;
      } else {
        digits();
      }
      boolean integer = true;
      if (at('.')) {
        integer = false;
        p++;
        if (digits() == 0) {
          throw invalid();
        }
      }
      if (at('e') || at('E')) {
        integer = false;
        p++;
        if (at('+') || at('-')) {
          p++;
        }
        if (digits() == 0) {
          throw invalid();
        }
      }
      String literal = s.substring(start, p);
      if (integer) {
        BigInteger v = new BigInteger(literal);
        if (v.bitLength() > 63) {
          throw invalid();
        }
        return "I" + v;
      }
      double d = Double.parseDouble(literal);
      if (Double.isInfinite(d)) {
        throw invalid();
      }
      return "D" + Long.toHexString(Double.doubleToRawLongBits(d));
    }
  }

  private static String canonical(JsonNode n) {
    if (n instanceof JsonObject o) {
      StringBuilder out = new StringBuilder("{");
      o.members()
          .forEach(
              (k, v) ->
                  out.append(k.length()).append('=').append(k).append(':').append(canonical(v)));
      return out.append('}').toString();
    }
    if (n instanceof JsonArray a) {
      if (a.elements().isEmpty()) {
        return "[]";
      }
      StringBuilder out = new StringBuilder("[");
      a.elements().forEach(e -> out.append(canonical(e)).append(';'));
      return out.append(']').toString();
    }
    if (n instanceof JsonString t) {
      return "S" + t.value().length() + ":" + t.value();
    }
    if (n instanceof JsonBoolean b) {
      return b.value() ? "T" : "F";
    }
    if (n instanceof JsonNull) {
      return "N";
    }
    JsonNumber number = (JsonNumber) n;
    return number.isInteger()
        ? "I" + number.longValue()
        : "D" + Long.toHexString(Double.doubleToRawLongBits(number.doubleValue()));
  }

  private static String digits(Random r, int n) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < n; i++) {
      b.append((char) ('0' + r.nextInt(10)));
    }
    return b.toString();
  }

  private static String number(Random r) {
    StringBuilder b = new StringBuilder();
    if (r.nextInt(3) == 0) {
      b.append('-');
    }
    switch (r.nextInt(8)) {
      case 0 -> b.append('0');
      case 1 -> b.append('0').append(digits(r, 1 + r.nextInt(3)));
      default -> b.append(1 + r.nextInt(9)).append(digits(r, r.nextInt(r.nextBoolean() ? 25 : 8)));
    }
    if (r.nextInt(3) == 0) {
      b.append('.').append(digits(r, r.nextInt(3) == 0 ? 0 : 1 + r.nextInt(22)));
    }
    if (r.nextInt(4) == 0) {
      b.append(r.nextBoolean() ? 'e' : 'E');
      b.append(new String[] {"", "+", "-"}[r.nextInt(3)]);
      b.append(digits(r, r.nextInt(5)));
    }
    return b.toString();
  }

  private static String string(Random r) {
    StringBuilder b = new StringBuilder("\"");
    int n = r.nextInt(8);
    for (int i = 0; i < n; i++) {
      switch (r.nextInt(9)) {
        case 0 -> b.append("\\\"");
        case 1 -> b.append("\\\\");
        case 2 -> b.append("\\u").append(String.format("%04x", r.nextInt(0x10000)));
        case 3 -> b.append(digits(r, 1 + r.nextInt(30)));
        case 4 -> b.append('-').append(digits(r, 20));
        case 5 -> b.append(r.nextBoolean() ? ",:{}[] " : "e+1.5");
        case 6 -> b.append('\\').append("/bfnrt".charAt(r.nextInt(6)));
        case 7 -> b.append(r.nextInt(30) == 0 ? "\\x" : "ab");
        default -> b.append(r.nextInt(20) == 0 ? "\n" : "z");
      }
    }
    return b.append('"').toString();
  }

  private static void blank(Random r, StringBuilder out) {
    if (r.nextInt(4) == 0) {
      out.append(" \t\n\r".charAt(r.nextInt(4)));
    }
  }

  private static void value(Random r, int depth, StringBuilder out) {
    switch (r.nextInt(depth > 4 ? 5 : 8)) {
      case 0, 1, 2 -> out.append(number(r));
      case 3 -> out.append(string(r));
      case 4 -> out.append(new String[] {"true", "false", "null"}[r.nextInt(3)]);
      case 5, 6 -> {
        out.append('[');
        int n = r.nextInt(4);
        for (int i = 0; i < n; i++) {
          out.append(i > 0 ? (r.nextInt(25) == 0 ? ",," : ",") : "");
          blank(r, out);
          value(r, depth + 1, out);
          blank(r, out);
        }
        out.append(r.nextInt(30) == 0 ? ",]" : "]");
      }
      default -> {
        out.append('{');
        int n = r.nextInt(4);
        for (int i = 0; i < n; i++) {
          out.append(i > 0 ? "," : "");
          out.append(r.nextInt(4) == 0 ? "\"" + digits(r, 1 + r.nextInt(22)) + "\"" : string(r));
          blank(r, out);
          out.append(':');
          blank(r, out);
          value(r, depth + 1, out);
          blank(r, out);
        }
        out.append('}');
      }
    }
  }

  @Test
  @Timeout(60)
  void theReaderAgreesWithAReferenceParserOnGeneratedAndDamagedDocuments() {
    Random random = new Random(0x5EEDL);
    String junk = "\"\\0-.eE+,:[]{} x";
    int valid = 0;
    for (int i = 0; i < DOCUMENTS; i++) {
      StringBuilder text = new StringBuilder();
      value(random, 0, text);
      String document = text.toString();
      if (random.nextInt(3) == 0 && !document.isEmpty()) {
        int at = random.nextInt(document.length());
        document =
            document.substring(0, at)
                + junk.charAt(random.nextInt(junk.length()))
                + document.substring(at + 1);
      }
      String expected = Reference.parse(document);
      Outcome<JsonNode> actual = Jx.read(document);
      if (expected == null) {
        assertThat(actual.value()).as("must be invalid: %s", document).isEmpty();
      } else {
        valid++;
        assertThat(actual.value()).as("must be valid: %s", document).isPresent();
        assertThat(canonical(actual.value().get())).as("tree of %s", document).isEqualTo(expected);
      }
    }
    assertThat(valid).as("valid documents among the generated ones").isGreaterThan(DOCUMENTS / 5);
  }
}
