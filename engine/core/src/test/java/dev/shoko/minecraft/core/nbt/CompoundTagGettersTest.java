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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Lenient typed reading of compound values (R36, R38), provisional until probe PS2. */
class CompoundTagGettersTest {

  private static CompoundTag sample() {
    return compound(
        "byte", byteTag(7),
        "short", shortTag(300),
        "int", intTag(0x01020304),
        "long", longTag(3000000000L),
        "float", floatTag(7.9f),
        "double", doubleTag(-7.9),
        "huge", doubleTag(1.0e10),
        "nan", new DoubleTag(0x7ff8000000000000L),
        "text", stringTag("5"),
        "bytes", ByteArrayTag.of((byte) 1, (byte) -1),
        "ints", IntArrayTag.of(1, 2),
        "longs", LongArrayTag.of(3L),
        "list", list(intTag(1)),
        "child", compound("a", byteTag(1)));
  }

  @Test
  void readsNumbersOfTheRequestedType() {
    // spec: core-nbt R38
    var tag = sample();
    assertThat(tag.getByte("byte")).isEqualTo((byte) 7);
    assertThat(tag.getShort("short")).isEqualTo((short) 300);
    assertThat(tag.getInt("int")).isEqualTo(0x01020304);
    assertThat(tag.getLong("long")).isEqualTo(3000000000L);
    assertThat(tag.getFloat("float")).isEqualTo(7.9f);
    assertThat(tag.getDouble("double")).isEqualTo(-7.9);
  }

  @Test
  void convertsOtherNumericTypesAsProbePs2Expects() {
    // spec: core-nbt R38, probe PS2 (7.9d -> 7, -7.9d -> -7, 7b -> 7, 3000000000L -> -1294967296)
    var tag =
        compound(
            "a", doubleTag(7.9), "b", doubleTag(-7.9), "c", byteTag(7), "d", longTag(3000000000L));
    assertThat(tag.getInt("a")).isEqualTo(7);
    assertThat(tag.getInt("b")).isEqualTo(-7);
    assertThat(tag.getInt("c")).isEqualTo(7);
    assertThat(tag.getInt("d")).isEqualTo(-1294967296);
  }

  @Test
  void stringIsNotConvertedToNumber() {
    // spec: core-nbt R38, probe PS2 ("5" gives 0)
    var tag = sample();
    assertThat(tag.getInt("text")).isZero();
    assertThat(tag.getDouble("text")).isZero();
  }

  @Test
  void convertsBetweenNumericTypes() {
    // spec: core-nbt R38 (Java primitive conversions), provisional
    var tag = sample();
    assertThat(tag.getByte("short")).isEqualTo((byte) 44);
    assertThat(tag.getShort("int")).isEqualTo((short) 0x0304);
    assertThat(tag.getLong("int")).isEqualTo(0x01020304L);
    assertThat(tag.getInt("float")).isEqualTo(7);
    assertThat(tag.getLong("double")).isEqualTo(-7L);
    assertThat(tag.getInt("huge")).isEqualTo(Integer.MAX_VALUE);
    assertThat(tag.getLong("huge")).isEqualTo(10000000000L);
    assertThat(tag.getInt("nan")).isZero();
    assertThat(tag.getDouble("float")).isEqualTo((double) 7.9f);
    assertThat(tag.getFloat("double")).isEqualTo(-7.9f);
    assertThat(tag.getDouble("long")).isEqualTo(3.0e9);
  }

  @ParameterizedTest(name = "key {0}")
  @ValueSource(strings = {"missing", "text", "bytes", "ints", "longs", "list", "child"})
  void absentOrNonNumericValueReadsAsZero(String key) {
    // spec: core-nbt R38 (default value 0)
    var tag = sample();
    assertThat(tag.getByte(key)).isZero();
    assertThat(tag.getShort(key)).isZero();
    assertThat(tag.getInt(key)).isZero();
    assertThat(tag.getLong(key)).isZero();
    assertThat(tag.getFloat(key)).isZero();
    assertThat(tag.getDouble(key)).isZero();
  }

  @Test
  void booleanIsTrueForAnyNonZeroByte() {
    // spec: core-nbt R36 (any non-zero value is true), probe PS2 (NoGravity:2b), provisional
    var tag =
        compound("zero", byteTag(0), "one", byteTag(1), "two", byteTag(2), "neg", byteTag(-1));
    assertThat(tag.getBoolean("zero")).isFalse();
    assertThat(tag.getBoolean("one")).isTrue();
    assertThat(tag.getBoolean("two")).isTrue();
    assertThat(tag.getBoolean("neg")).isTrue();
    assertThat(tag.getBoolean("missing")).isFalse();
  }

  @Test
  void booleanOfOtherNumericTypeTestsItsOriginalValue() {
    // spec: core-nbt R36 (revised: original value non-zero, no conversion to Byte first; Int 256
    // was false under the former getByte(key) != 0), provisional; vectors in CompoundTagBooleanTest
    var tag = compound("int256", intTag(256), "int1", intTag(1), "text", stringTag("true"));
    assertThat(tag.getBoolean("int256")).isTrue();
    assertThat(tag.getBoolean("int1")).isTrue();
    assertThat(tag.getBoolean("text")).isFalse();
  }

  @Test
  void stringIsReadOnlyFromStringValue() {
    // spec: core-nbt R38 (default: empty string; no conversion to string)
    var tag = sample();
    assertThat(tag.getString("text")).isEqualTo("5");
    assertThat(tag.getString("int")).isEmpty();
    assertThat(tag.getString("missing")).isEmpty();
  }

  @Test
  void arraysAreReadOnlyFromArrayOfSameType() {
    // spec: core-nbt R38 (default: empty array)
    var tag = sample();
    assertThat(tag.getByteArray("bytes")).containsExactly(1, -1);
    assertThat(tag.getIntArray("ints")).containsExactly(1, 2);
    assertThat(tag.getLongArray("longs")).containsExactly(3L);
    assertThat(tag.getByteArray("ints")).isEmpty();
    assertThat(tag.getIntArray("list")).isEmpty();
    assertThat(tag.getLongArray("ints")).isEmpty();
    assertThat(tag.getIntArray("missing")).isEmpty();
  }

  @Test
  void arrayGettersReturnCopies() {
    // API contract: a copy of the elements
    var tag = sample();
    int[] ints = tag.getIntArray("ints");
    ints[0] = 9;
    assertThat(tag.getIntArray("ints")).containsExactly(1, 2);
  }

  @Test
  void listAndCompoundDefaultToEmpty() {
    // spec: core-nbt R38 (default: empty list or compound)
    var tag = sample();
    assertThat(tag.getList("list")).isEqualTo(list(intTag(1)));
    assertThat(tag.getList("child")).isEqualTo(ListTag.empty());
    assertThat(tag.getList("missing")).isEqualTo(ListTag.empty());
    assertThat(tag.getCompound("child")).isEqualTo(compound("a", byteTag(1)));
    assertThat(tag.getCompound("list")).isEqualTo(CompoundTag.empty());
    assertThat(tag.getCompound("missing")).isEqualTo(CompoundTag.empty());
  }
}
