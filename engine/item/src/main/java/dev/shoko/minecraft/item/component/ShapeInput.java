package dev.shoko.minecraft.item.component;

import dev.shoko.minecraft.core.nbt.NbtTag;
import java.util.List;
import java.util.SequencedMap;

/**
 * A view of a decoded value, JSON or NBT, that the {@link ShapeReader} can walk. One reader serves
 * both forms, so that a value accepted from JSON is accepted from its own NBT.
 *
 * @param <T> the type of the value: a JSON node or an NBT tag
 */
interface ShapeInput<T> {

  /** The kind of a value, as far as the shapes distinguish them. */
  enum Kind {
    OBJECT,
    LIST,
    TEXT,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    OTHER
  }

  Kind kind(T value);

  /** The members of an {@link Kind#OBJECT}, in order. */
  SequencedMap<String, T> members(T object);

  /** The elements of a {@link Kind#LIST}, in order. */
  List<T> elements(T list);

  String text(T value);

  /** The value of an {@link Kind#INTEGER}. */
  long integer(T value);

  /** The value of an {@link Kind#INTEGER} or {@link Kind#DECIMAL}. */
  double decimal(T value);

  /**
   * The bits of the float that a number stands for: those of a float tag as they are, the nearest
   * float for any other number.
   */
  int floatBits(T value);

  /** The value of a {@link Kind#BOOLEAN}. */
  boolean bool(T value);

  /** Whether a {@link Kind#INTEGER} also stands for a boolean (NBT has no boolean tag). */
  boolean integersAreBooleans();

  /**
   * The canonical NBT of a value with no shape.
   *
   * @param value the value
   * @param path where the value is, for the finding of a value NBT cannot hold
   * @return the tag
   */
  NbtTag any(T value, String path);
}
