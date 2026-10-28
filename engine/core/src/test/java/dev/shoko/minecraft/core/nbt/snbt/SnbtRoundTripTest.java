package dev.shoko.minecraft.core.nbt.snbt;

import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.doubleTag;
import static dev.shoko.minecraft.core.nbt.Tags.floatTag;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.list;
import static dev.shoko.minecraft.core.nbt.Tags.longTag;
import static dev.shoko.minecraft.core.nbt.Tags.shortTag;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static dev.shoko.minecraft.core.nbt.Tags.text;
import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.ByteArrayTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.DoubleTag;
import dev.shoko.minecraft.core.nbt.FloatTag;
import dev.shoko.minecraft.core.nbt.IntArrayTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.LongArrayTag;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.snbt.SnbtVectors.Accepted;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Parsing the text written by {@link SnbtWriter} gives the value back (R41 to R56). NaN and
 * infinities are excluded: their text ({@code NaNd}, {@code Infinityf}) reads as an unquoted string
 * by R43 and R44.
 */
class SnbtRoundTripTest {

  static List<Accepted> accepted() {
    return SnbtVectors.accepted();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("accepted")
  void parsedTableEntryIsWrittenAsExpectedText(Accepted vector) throws NbtException {
    // spec: core-nbt table T (probe PS1 then /data get, probe PW1), provisional
    assertThat(SnbtWriter.write(SnbtParser.parse(vector.input()))).isEqualTo(vector.text());
  }

  private static Named<Supplier<NbtTag>> value(String name, Supplier<NbtTag> factory) {
    return Named.of(name, factory);
  }

  static Stream<Named<Supplier<NbtTag>>> values() {
    return Stream.of(
        value("byte bounds", () -> list(byteTag(-128), byteTag(127))),
        value("short and int", () -> list(shortTag(-32768), shortTag(1))),
        value("int bounds", () -> list(intTag(Integer.MIN_VALUE), intTag(Integer.MAX_VALUE))),
        value("long bounds", () -> list(longTag(Long.MIN_VALUE), longTag(Long.MAX_VALUE))),
        value("floats", () -> list(floatTag(0.1f), floatTag(1.0e10f), new FloatTag(0x80000000))),
        value(
            "doubles",
            () -> list(doubleTag(1.0e-4), doubleTag(Double.MAX_VALUE), new DoubleTag(1L << 63))),
        value("smallest double", () -> doubleTag(Double.MIN_VALUE)),
        value("tricky strings", () -> list(stringTag(""), stringTag("a\"b'c\\d"), stringTag("'"))),
        value("non-ASCII string", () -> stringTag(text(0xE9, 0x20AC, 0x1F600))),
        value("word-like strings", () -> list(stringTag("true"), stringTag("1"), stringTag("NaN"))),
        value("arrays", () -> list(ByteArrayTag.of((byte) -1), ByteArrayTag.of())),
        value(
            "int and long arrays",
            () -> compound("i", IntArrayTag.of(), "l", LongArrayTag.of(-1L))),
        value(
            "heterogeneous list", () -> list(intTag(1), stringTag("a"), compound("b", byteTag(1)))),
        value("empty containers", () -> list(ListTag.empty(), ListTag.empty())),
        value(
            "keys",
            () ->
                compound(
                    "plain", intTag(1),
                    "with space", intTag(2),
                    "minecraft:id", intTag(3),
                    "q\"uote", intTag(4))),
        value("nested", () -> compound("a", compound("b", list(CompoundTag.empty())))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("values")
  void writtenTextParsesBackToSameValue(Supplier<NbtTag> factory) throws NbtException {
    // spec: core-nbt R41 to R56
    NbtTag value = factory.get();
    assertThat(SnbtParser.parse(SnbtWriter.write(value))).isEqualTo(value);
  }
}
