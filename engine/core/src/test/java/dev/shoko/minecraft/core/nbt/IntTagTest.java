package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IntTagTest {

  @Test
  void typeIsInt() {
    // spec: core-nbt R1
    assertThat(new IntTag(1).type()).isEqualTo(TagType.INT);
  }

  @Test
  void narrowsByKeepingLowOrderBits() {
    // spec: core-nbt R38 (integer to narrower integer keeps the low-order bits), provisional
    var tag = new IntTag(0x01020304);
    assertThat(tag.byteValue()).isEqualTo((byte) 0x04);
    assertThat(tag.shortValue()).isEqualTo((short) 0x0304);
    assertThat(new IntTag(Integer.MIN_VALUE).shortValue()).isEqualTo((short) 0);
    assertThat(new IntTag(Integer.MIN_VALUE).byteValue()).isEqualTo((byte) 0);
    assertThat(new IntTag(70000).shortValue()).isEqualTo((short) 4464);
  }

  @Test
  void widensExactlyToLongAndDouble() {
    // spec: core-nbt R38
    assertThat(new IntTag(Integer.MIN_VALUE).longValue()).isEqualTo(-2147483648L);
    assertThat(new IntTag(16909060).doubleValue()).isEqualTo(16909060.0);
  }

  @Test
  void convertsToNearestFloat() {
    // spec: core-nbt R38 (integer to float: nearest value); 2^24 + 1 is not a float
    assertThat(new IntTag(16777217).floatValue()).isEqualTo(1.6777216E7f);
    assertThat(new IntTag(-7).floatValue()).isEqualTo(-7.0f);
  }

  @Test
  void equalsIntOfSameValueOnly() {
    // spec: core-nbt R37
    assertThat(new IntTag(1)).isEqualTo(new IntTag(1));
    assertThat(new IntTag(1)).hasSameHashCodeAs(new IntTag(1));
    assertThat(new IntTag(1)).isNotEqualTo(new LongTag(1L));
  }
}
