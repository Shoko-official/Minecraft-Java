package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.json.JsonArray;
import dev.shoko.minecraft.data.json.JsonBoolean;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonNumber;
import dev.shoko.minecraft.data.json.JsonObject;
import dev.shoko.minecraft.data.json.JsonString;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

/**
 * JSON values for the {@link ShapeReader}. A value with no shape becomes the NBT that carries the
 * same information: an integer is an {@code Int} (a {@code Long} when it does not fit), a decimal a
 * {@code Double}, a boolean a {@code Byte}.
 */
final class JsonInput implements ShapeInput<JsonNode> {

  static final JsonInput INSTANCE = new JsonInput();

  private JsonInput() {}

  @Override
  public Kind kind(JsonNode value) {
    return switch (value) {
      case JsonObject _ -> Kind.OBJECT;
      case JsonArray _ -> Kind.LIST;
      case JsonString _ -> Kind.TEXT;
      case JsonNumber number -> number.isInteger() ? Kind.INTEGER : Kind.DECIMAL;
      case JsonBoolean _ -> Kind.BOOLEAN;
      default -> Kind.OTHER;
    };
  }

  @Override
  public SequencedMap<String, JsonNode> members(JsonNode object) {
    return ((JsonObject) object).members();
  }

  @Override
  public List<JsonNode> elements(JsonNode list) {
    return ((JsonArray) list).elements();
  }

  @Override
  public String text(JsonNode value) {
    return ((JsonString) value).value();
  }

  @Override
  public long integer(JsonNode value) {
    return ((JsonNumber) value).longValue();
  }

  @Override
  public double decimal(JsonNode value) {
    return ((JsonNumber) value).doubleValue();
  }

  @Override
  public boolean bool(JsonNode value) {
    return ((JsonBoolean) value).value();
  }

  @Override
  public boolean integersAreBooleans() {
    return false;
  }

  @Override
  public NbtTag any(JsonNode value, String path) {
    return switch (value) {
      case JsonObject object -> compound(object, path);
      case JsonArray array -> list(array, path);
      case JsonString string -> new StringTag(string.value());
      case JsonBoolean bool -> new ByteTag((byte) (bool.value() ? 1 : 0));
      case JsonNumber number -> number(number);
      default ->
          throw new FindingException(FindingCode.TYPE_MISMATCH, path, "null has no NBT form");
    };
  }

  private NbtTag compound(JsonObject object, String path) {
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    object.members().forEach((key, child) -> entries.put(key, any(child, path + "." + key)));
    return CompoundTag.of(entries);
  }

  private NbtTag list(JsonArray array, String path) {
    List<NbtTag> elements = new ArrayList<>(array.elements().size());
    for (int i = 0; i < array.elements().size(); i++) {
      elements.add(any(array.elements().get(i), path + "[" + i + "]"));
    }
    return ListTag.of(elements);
  }

  private static NbtTag number(JsonNumber number) {
    if (!number.isInteger()) {
      return DoubleTag.of(number.doubleValue());
    }
    long value = number.longValue();
    return value == (int) value ? new IntTag((int) value) : new LongTag(value);
  }
}
