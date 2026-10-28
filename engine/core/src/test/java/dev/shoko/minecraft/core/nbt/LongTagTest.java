package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LongTagTest {

  @Test
  void typeIsLong() {
    // spec: core-nbt R1
    assertThat(new LongTag(1L).type()).isEqualTo(TagType.LONG);
  }

  @Test
  void longOutsideIntRangeKeepsLowOrderBitsAsInt() {
    // spec: core-nbt R38 example (Long 3000000000 read as Int gives -1294967296), probe PS2,
    // provisional
    assertThat(new LongTag(3000000000L).intValue()).isEqualTo(-1294967296);
  }

  @Test
  void narrowsToEveryIntegerTypeByKeepingLowOrderBits() {
    // spec: core-nbt R38, provisional
    var tag = new LongTag(0x1_0000_00FFL);
    assertThat(tag.intValue()).isEqualTo(255);
    assertThat(tag.shortValue()).isEqualTo((short) 255);
    assertThat(tag.byteValue()).isEqualTo((byte) -1);
    var min = new LongTag(Long.MIN_VALUE);
    assertThat(min.intValue()).isZero();
    assertThat(min.shortValue()).isZero();
    assertThat(min.byteValue()).isZero();
  }

  @Test
  void convertsToNearestFloatAndDouble() {
    // spec: core-nbt R38 (integer to float or double: nearest value); 2^53 + 1 is not a double
    assertThat(new LongTag(9007199254740993L).doubleValue()).isEqualTo(9.007199254740992E15);
    assertThat(new LongTag(3000000000L).floatValue()).isEqualTo(3.0E9f);
    assertThat(new LongTag(Long.MIN_VALUE).longValue()).isEqualTo(Long.MIN_VALUE);
  }

  @Test
  void equalsLongOfSameValueOnly() {
    // spec: core-nbt R37
    assertThat(new LongTag(1L)).isEqualTo(new LongTag(1L));
    assertThat(new LongTag(1L)).isNotEqualTo(new IntTag(1));
  }
}
