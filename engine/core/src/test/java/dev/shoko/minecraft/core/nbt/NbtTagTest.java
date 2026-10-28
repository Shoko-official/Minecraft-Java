package dev.shoko.minecraft.core.nbt;

import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.doubleTag;
import static dev.shoko.minecraft.core.nbt.Tags.floatTag;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.longTag;
import static dev.shoko.minecraft.core.nbt.Tags.shortTag;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Contract shared by every value type: its binary type (R1, R2) and equality across types (R37).
 */
class NbtTagTest {

  private static Named<Supplier<NbtTag>> value(String name, Supplier<NbtTag> factory) {
    return Named.of(name, factory);
  }

  static Stream<Arguments> valuesAndTypes() {
    return Stream.of(
        arguments(value("Byte", () -> byteTag(1)), TagType.BYTE),
        arguments(value("Short", () -> shortTag(1)), TagType.SHORT),
        arguments(value("Int", () -> intTag(1)), TagType.INT),
        arguments(value("Long", () -> longTag(1L)), TagType.LONG),
        arguments(value("Float", () -> floatTag(1.0f)), TagType.FLOAT),
        arguments(value("Double", () -> doubleTag(1.0)), TagType.DOUBLE),
        arguments(value("Byte_Array", () -> ByteArrayTag.of((byte) 1)), TagType.BYTE_ARRAY),
        arguments(value("String", () -> stringTag("1")), TagType.STRING),
        arguments(value("List", () -> list(intTag(1))), TagType.LIST),
        arguments(value("Compound", () -> compound("a", intTag(1))), TagType.COMPOUND),
        arguments(value("Int_Array", () -> IntArrayTag.of(1)), TagType.INT_ARRAY),
        arguments(value("Long_Array", () -> LongArrayTag.of(1L)), TagType.LONG_ARRAY));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("valuesAndTypes")
  void valueReportsItsBinaryType(Supplier<NbtTag> factory, TagType type) {
    // spec: core-nbt R1, R2 (never END for a value)
    assertThat(factory.get().type()).isEqualTo(type);
  }

  @Test
  void valuesOfDifferentTypesAreNeverEqual() {
    // spec: core-nbt R37 (same type required, e.g. Int 1 differs from Byte 1)
    List<NbtTag> values =
        List.of(
            byteTag(1),
            shortTag(1),
            intTag(1),
            longTag(1L),
            floatTag(1.0f),
            doubleTag(1.0),
            ByteArrayTag.of((byte) 1),
            stringTag("1"),
            list(intTag(1)),
            compound("", intTag(1)),
            IntArrayTag.of(1),
            LongArrayTag.of(1L));
    for (int i = 0; i < values.size(); i++) {
      for (int j = 0; j < values.size(); j++) {
        if (i != j) {
          assertThat(values.get(i)).as("%s against %s", i, j).isNotEqualTo(values.get(j));
        }
      }
    }
  }
}
