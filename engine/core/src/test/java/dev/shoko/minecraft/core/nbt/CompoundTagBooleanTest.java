package dev.shoko.minecraft.core.nbt;

import static dev.shoko.minecraft.core.nbt.Tags.byteTag;
import static dev.shoko.minecraft.core.nbt.Tags.compound;
import static dev.shoko.minecraft.core.nbt.Tags.doubleTag;
import static dev.shoko.minecraft.core.nbt.Tags.intTag;
import static dev.shoko.minecraft.core.nbt.Tags.longTag;
import static dev.shoko.minecraft.core.nbt.Tags.shortTag;
import static dev.shoko.minecraft.core.nbt.Tags.stringTag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Boolean reading (R36, revised): true when the original numeric value is not 0, without a prior
 * conversion to Byte; a non-numeric value is false. Provisional until probe PS2; the public
 * documentation reads through Byte instead (vectors B2, B3, B4 and B8 would then be false).
 */
class CompoundTagBooleanTest {

  static Stream<Arguments> vectors() {
    return Stream.of(
        arguments("B1 Byte 2", (Supplier<NbtTag>) () -> byteTag(2), true),
        arguments("B2 Int 256", (Supplier<NbtTag>) () -> intTag(256), true),
        arguments("B3 Double 0.5", (Supplier<NbtTag>) () -> doubleTag(0.5), true),
        arguments("B4 Float NaN 7fc00000", (Supplier<NbtTag>) () -> new FloatTag(0x7fc00000), true),
        arguments("B5 Double 0.0", (Supplier<NbtTag>) () -> doubleTag(0.0), false),
        arguments(
            "B6 Double -0.0", (Supplier<NbtTag>) () -> new DoubleTag(0x8000000000000000L), false),
        arguments("B7 Byte 0", (Supplier<NbtTag>) () -> byteTag(0), false),
        arguments("B8 Short -256", (Supplier<NbtTag>) () -> shortTag(-256), true),
        arguments("B9 String true", (Supplier<NbtTag>) () -> stringTag("true"), false),
        arguments("B10 empty List", (Supplier<NbtTag>) () -> ListTag.empty(), false));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("vectors")
  void readsBooleanFromOriginalValue(String name, Supplier<NbtTag> value, boolean expected) {
    // spec: core-nbt R36, vectors B1 to B10, provisional
    assertThat(compound("v", value.get()).getBoolean("v")).isEqualTo(expected);
  }

  static Stream<Arguments> moreValues() {
    return Stream.of(
        arguments("Long 2^32", (Supplier<NbtTag>) () -> longTag(1L << 32), true),
        arguments("Int -1", (Supplier<NbtTag>) () -> intTag(-1), true),
        arguments("Double 1e-300", (Supplier<NbtTag>) () -> doubleTag(1.0e-300), true),
        arguments("Float -0.0", (Supplier<NbtTag>) () -> new FloatTag(0x80000000), false),
        arguments(
            "Double NaN with payload",
            (Supplier<NbtTag>) () -> new DoubleTag(0x7ff0000000000001L),
            true),
        arguments("Compound", (Supplier<NbtTag>) () -> CompoundTag.empty(), false),
        arguments("Int_Array [1]", (Supplier<NbtTag>) () -> IntArrayTag.of(1), false));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("moreValues")
  void readsBooleanOfOtherValues(String name, Supplier<NbtTag> value, boolean expected) {
    // spec: core-nbt R36 (Long 2^32 is not 0 although its low-order byte is; any NaN is true;
    // 0, 0.0 and -0.0 are false; non-numeric values give the default false of R38)
    assertThat(compound("v", value.get()).getBoolean("v")).isEqualTo(expected);
  }
}
