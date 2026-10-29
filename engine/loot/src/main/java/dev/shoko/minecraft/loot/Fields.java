package dev.shoko.minecraft.loot;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the members of one JSON object and reports what is wrong with it: a member that is required
 * and absent, of the wrong kind or out of range, and, once {@link #finish()} is called, a member
 * nobody asked for. A getter that fails records the finding and returns a neutral value; the caller
 * checks {@link Findings#failed()} before it builds its result.
 */
public final class Fields {

  private final JsonObject object;
  private final Findings findings;
  private final Set<String> read = new HashSet<>();

  private Fields(JsonObject object, Findings findings) {
    this.object = object;
    this.findings = findings;
  }

  /**
   * Starts reading an object.
   *
   * @param node any JSON value
   * @param findings where the problems go
   * @return the reader; when the node is not an object a {@code TYPE_MISMATCH} is recorded and
   *     {@link #valid()} is false
   */
  public static Fields of(JsonNode node, Findings findings) {
    if (node instanceof JsonObject json) {
      return new Fields(json, findings);
    }
    findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected an object");
    return new Fields(null, findings);
  }

  /**
   * Tells whether the node was an object.
   *
   * @return {@code true} for an object
   */
  public boolean valid() {
    return object != null;
  }

  /**
   * Returns the node the fields come from.
   *
   * @return the object, {@code null} when the node was not an object
   */
  public JsonObject node() {
    return object;
  }

  /**
   * Tells whether a key is present, without marking it as read.
   *
   * @param key a member name
   * @return {@code true} when the object has it
   */
  public boolean has(String key) {
    return object != null && object.members().containsKey(key);
  }

  /**
   * Reads an optional member.
   *
   * @param key the member name
   * @return the value, {@code null} when absent
   */
  public JsonNode optional(String key) {
    if (object == null) {
      return null;
    }
    read.add(key);
    return object.members().get(key);
  }

  /**
   * Reads a required member.
   *
   * @param key the member name
   * @return the value, {@code null} after a {@code MISSING_FIELD}
   */
  public JsonNode required(String key) {
    JsonNode value = optional(key);
    if (value == null && object != null) {
      findings.add(LootFindingCode.MISSING_FIELD, object, "missing field " + key);
    }
    return value;
  }

  /**
   * Reads a required string.
   *
   * @param key the member name
   * @return the text, empty after a finding
   */
  public String requiredString(String key) {
    JsonNode value = required(key);
    return value == null ? "" : stringOf(value, findings);
  }

  /**
   * Reads an optional string.
   *
   * @param key the member name
   * @return the text, {@code null} when absent
   */
  public String optionalString(String key) {
    JsonNode value = optional(key);
    return value == null ? null : stringOf(value, findings);
  }

  /**
   * Reads a required identifier.
   *
   * @param key the member name
   * @return the identifier, {@code null} after a finding
   */
  public Identifier requiredId(String key) {
    JsonNode value = required(key);
    return value == null ? null : idOf(value, findings);
  }

  /**
   * Reads an optional identifier.
   *
   * @param key the member name
   * @return the identifier, {@code null} when absent or after a finding
   */
  public Identifier optionalId(String key) {
    JsonNode value = optional(key);
    return value == null ? null : idOf(value, findings);
  }

  /**
   * Reads a required integer.
   *
   * @param key the member name
   * @param min the smallest accepted value
   * @param max the largest accepted value
   * @return the value, {@code min} after a finding
   */
  public int requiredInt(String key, int min, int max) {
    JsonNode value = required(key);
    return value == null ? min : intOf(value, findings, min, max);
  }

  /**
   * Reads an optional integer.
   *
   * @param key the member name
   * @param fallback the value when the member is absent
   * @param min the smallest accepted value
   * @param max the largest accepted value
   * @return the value, the fallback when absent
   */
  public int optionalInt(String key, int fallback, int min, int max) {
    JsonNode value = optional(key);
    return value == null ? fallback : intOf(value, findings, min, max);
  }

  /**
   * Reads a required float.
   *
   * @param key the member name
   * @return the value, 0 after a finding
   */
  public float requiredFloat(String key) {
    JsonNode value = required(key);
    return value == null ? 0f : floatOf(value, findings);
  }

  /**
   * Reads an optional float.
   *
   * @param key the member name
   * @param fallback the value when absent
   * @return the value or the fallback
   */
  public float optionalFloat(String key, float fallback) {
    JsonNode value = optional(key);
    return value == null ? fallback : floatOf(value, findings);
  }

  /**
   * Reads an optional boolean.
   *
   * @param key the member name
   * @param fallback the value when absent
   * @return the value or the fallback
   */
  public boolean optionalBool(String key, boolean fallback) {
    JsonNode value = optional(key);
    return value == null ? fallback : boolOf(value, findings);
  }

  /**
   * Reads a required array.
   *
   * @param key the member name
   * @return the array, {@code null} after a finding
   */
  public JsonArray requiredArray(String key) {
    JsonNode value = required(key);
    return value == null ? null : arrayOf(value, findings);
  }

  /**
   * Reads an optional array.
   *
   * @param key the member name
   * @return the array, {@code null} when absent or after a finding
   */
  public JsonArray optionalArray(String key) {
    JsonNode value = optional(key);
    return value == null ? null : arrayOf(value, findings);
  }

  /**
   * Reads an optional object.
   *
   * @param key the member name
   * @return the object, {@code null} when absent or after a finding
   */
  public JsonObject optionalObject(String key) {
    JsonNode value = optional(key);
    if (value == null) {
      return null;
    }
    if (value instanceof JsonObject json) {
      return json;
    }
    findings.add(LootFindingCode.TYPE_MISMATCH, value, "expected an object");
    return null;
  }

  /**
   * Reports every member nobody read as {@code UNKNOWN_FIELD}. The decoding is strict (LT-R75): the
   * table that holds the key is refused, one finding per key. Whether the game ignores unknown keys
   * instead (H-unknown-ignore) is open, probe PL24; the vanilla data has none.
   */
  public void finish() {
    if (object == null) {
      return;
    }
    for (String key : object.members().keySet()) {
      if (!read.contains(key)) {
        findings.add(
            LootFindingCode.UNKNOWN_FIELD, object.members().get(key), "unknown field " + key);
      }
    }
  }

  /**
   * Reads a string value.
   *
   * @param node a JSON value
   * @param findings where a mismatch goes
   * @return the text, empty after a {@code TYPE_MISMATCH}
   */
  public static String stringOf(JsonNode node, Findings findings) {
    if (node instanceof JsonString string) {
      return string.value();
    }
    findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected a string");
    return "";
  }

  /**
   * Reads an identifier value.
   *
   * @param node a JSON value
   * @param findings where a problem goes
   * @return the identifier, {@code null} after a finding
   */
  public static Identifier idOf(JsonNode node, Findings findings) {
    if (!(node instanceof JsonString string)) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected an identifier string");
      return null;
    }
    Optional<Identifier> parsed = Identifier.tryParse(string.value());
    if (parsed.isEmpty()) {
      findings.add(LootFindingCode.INVALID_IDENTIFIER, node, "invalid identifier");
      return null;
    }
    return parsed.get();
  }

  /**
   * Reads an integer value; a decimal with an integral value is accepted.
   *
   * @param node a JSON value
   * @param findings where a problem goes
   * @param min the smallest accepted value
   * @param max the largest accepted value
   * @return the value, {@code min} after a finding
   */
  public static int intOf(JsonNode node, Findings findings, int min, int max) {
    if (!(node instanceof JsonNumber number)) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected a number");
      return min;
    }
    double value = number.doubleValue();
    if (!Double.isFinite(value) || value != Math.rint(value) || value < min || value > max) {
      findings.add(LootFindingCode.VALUE_RANGE, node, "integer out of range");
      return min;
    }
    return (int) value;
  }

  /**
   * Reads a float value.
   *
   * @param node a JSON value
   * @param findings where a problem goes
   * @return the value, 0 after a finding
   */
  public static float floatOf(JsonNode node, Findings findings) {
    if (!(node instanceof JsonNumber number)) {
      findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected a number");
      return 0f;
    }
    float value = (float) number.doubleValue();
    if (!Float.isFinite(value)) {
      findings.add(LootFindingCode.VALUE_RANGE, node, "float out of range");
      return 0f;
    }
    return value;
  }

  /**
   * Reads a boolean value.
   *
   * @param node a JSON value
   * @param findings where a mismatch goes
   * @return the value, false after a {@code TYPE_MISMATCH}
   */
  public static boolean boolOf(JsonNode node, Findings findings) {
    if (node instanceof JsonBoolean bool) {
      return bool.value();
    }
    findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected a boolean");
    return false;
  }

  /**
   * Reads an array value.
   *
   * @param node a JSON value
   * @param findings where a mismatch goes
   * @return the array, {@code null} after a {@code TYPE_MISMATCH}
   */
  public static JsonArray arrayOf(JsonNode node, Findings findings) {
    if (node instanceof JsonArray array) {
      return array;
    }
    findings.add(LootFindingCode.TYPE_MISMATCH, node, "expected an array");
    return null;
  }
}
