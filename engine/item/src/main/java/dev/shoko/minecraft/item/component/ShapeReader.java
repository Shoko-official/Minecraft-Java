package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.FindingCode;
import dev.shoko.minecraft.item.FindingException;
import dev.shoko.minecraft.item.component.Shape.Field;
import dev.shoko.minecraft.item.component.ShapeInput.Kind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * Checks a value against a {@link Shape} and builds its canonical NBT (IC-R12, IC-R13). The first
 * problem found refuses the whole value, with the code that tells why. An accepted value read again
 * from its own canonical NBT gives the same value.
 *
 * @param <T> the type of the value read: a JSON node or an NBT tag
 */
final class ShapeReader<T> {

  private final ShapeInput<T> input;

  ShapeReader(ShapeInput<T> input) {
    this.input = input;
  }

  /**
   * Reads a value.
   *
   * @param shape the shape the value must have
   * @param value the value
   * @param path where the value is, for the findings
   * @return the canonical NBT
   * @throws FindingException when the value does not have the shape
   */
  NbtTag read(Shape shape, T value, String path) {
    return switch (shape) {
      case Shape.Str _ -> new StringTag(text(value, path));
      case Shape.Ident _ -> new StringTag(identifier(text(value, path), path).toString());
      case Shape.IdSet _ -> idSet(value, path);
      case Shape.IntS range -> new IntTag(intValue(range, value, path));
      case Shape.FloatS _ -> new FloatTag(floatBits(value, path));
      case Shape.DoubleS _ -> DoubleTag.of(number(value, path));
      case Shape.BoolS _ -> new ByteTag((byte) (bool(value, path) ? 1 : 0));
      case Shape.ListOf list -> list(list, value, path);
      case Shape.Obj obj -> object(obj, value, path);
      case Shape.MapOf map -> map(map, value, path);
      case Shape.AnyValue _ -> input.any(value, path);
      case Shape.AnyCompound _ -> {
        expect(value, Kind.OBJECT, "an object", path);
        yield input.any(value, path);
      }
    };
  }

  private void expect(T value, Kind kind, String what, String path) {
    if (input.kind(value) != kind) {
      throw mismatch(what, path);
    }
  }

  private static FindingException mismatch(String what, String path) {
    return new FindingException(FindingCode.TYPE_MISMATCH, path, "expected " + what);
  }

  private String text(T value, String path) {
    expect(value, Kind.TEXT, "a string", path);
    return input.text(value);
  }

  private static Identifier identifier(String text, String path) {
    Optional<Identifier> id = Identifier.tryParse(text);
    if (id.isEmpty()) {
      throw new FindingException(
          FindingCode.INVALID_IDENTIFIER, path, "not an identifier: " + text);
    }
    return id.get();
  }

  private NbtTag idSet(T value, String path) {
    if (input.kind(value) == Kind.TEXT) {
      String text = input.text(value);
      boolean tag = text.startsWith("#");
      Identifier id = identifier(tag ? text.substring(1) : text, path);
      return new StringTag((tag ? "#" : "") + id);
    }
    expect(value, Kind.LIST, "an identifier, a tag or a list of identifiers", path);
    List<NbtTag> ids = new ArrayList<>();
    List<T> elements = input.elements(value);
    for (int i = 0; i < elements.size(); i++) {
      String at = path + "[" + i + "]";
      ids.add(new StringTag(identifier(text(elements.get(i), at), at).toString()));
    }
    return ListTag.of(ids);
  }

  private double number(T value, String path) {
    Kind kind = input.kind(value);
    if (kind != Kind.INTEGER && kind != Kind.DECIMAL) {
      throw mismatch("a number", path);
    }
    return input.decimal(value);
  }

  private int intValue(Shape.IntS range, T value, String path) {
    expect(value, Kind.INTEGER, "an integer", path);
    long integer = input.integer(value);
    if (integer < range.min() || integer > range.max()) {
      throw new FindingException(FindingCode.VALUE_RANGE, path, "integer out of range: " + integer);
    }
    return (int) integer;
  }

  /** The bits of the float, kept as they are when the tag already is a float (NaN payloads). */
  private int floatBits(T value, String path) {
    double number = number(value, path);
    int bits = input.floatBits(value);
    if (Float.isInfinite(Float.intBitsToFloat(bits)) && !Double.isInfinite(number)) {
      throw new FindingException(FindingCode.VALUE_RANGE, path, "float out of range: " + number);
    }
    return bits;
  }

  private boolean bool(T value, String path) {
    Kind kind = input.kind(value);
    if (kind == Kind.BOOLEAN) {
      return input.bool(value);
    }
    if (kind == Kind.INTEGER && input.integersAreBooleans()) {
      return input.integer(value) != 0;
    }
    throw mismatch("a boolean", path);
  }

  private NbtTag list(Shape.ListOf shape, T value, String path) {
    expect(value, Kind.LIST, "a list", path);
    List<T> elements = input.elements(value);
    List<NbtTag> read = new ArrayList<>(elements.size());
    for (int i = 0; i < elements.size(); i++) {
      read.add(read(shape.element(), elements.get(i), path + "[" + i + "]"));
    }
    return ListTag.of(read);
  }

  private NbtTag map(Shape.MapOf shape, T value, String path) {
    expect(value, Kind.OBJECT, "an object", path);
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    input
        .members(value)
        .forEach(
            (key, child) -> {
              String at = path + "." + key;
              String id = identifier(key, at).toString();
              if (entries.containsKey(id)) {
                throw new FindingException(
                    FindingCode.DUPLICATE_KEY, at, "the key " + id + " is given twice");
              }
              entries.put(id, read(shape.value(), child, at));
            });
    return CompoundTag.of(entries);
  }

  private NbtTag object(Shape.Obj shape, T value, String path) {
    expect(value, Kind.OBJECT, "an object", path);
    SequencedMap<String, NbtTag> entries = new LinkedHashMap<>();
    input
        .members(value)
        .forEach(
            (key, child) -> {
              String at = path + "." + key;
              Field field = field(shape, key, at);
              entries.put(key, read(field.shape(), child, at));
            });
    for (Field field : shape.fields()) {
      if (field.required() && !entries.containsKey(field.name())) {
        throw new FindingException(
            FindingCode.MISSING_FIELD, path + "." + field.name(), "missing field " + field.name());
      }
    }
    return CompoundTag.of(entries);
  }

  private static Field field(Shape.Obj shape, String key, String path) {
    for (Field field : shape.fields()) {
      if (field.name().equals(key)) {
        return field;
      }
    }
    throw new FindingException(FindingCode.UNKNOWN_FIELD, path, "unknown field " + key);
  }
}
