package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShortTagTest {

  @Test
  void typeIsShort() {
    // spec: core-nbt R1
    assertThat(new ShortTag((short) 1).type()).isEqualTo(TagType.SHORT);
  }

  @Test
  void narrowsToByteByKeepingLowOrderBits() {
    // spec: core-nbt R38 (integer to narrower integer keeps the low-order bits), provisional
    assertThat(new ShortTag((short) 300).byteValue()).isEqualTo((byte) 44);
    assertThat(new ShortTag((short) -1).byteValue()).isEqualTo((byte) -1);
    assertThat(new ShortTag(Short.MIN_VALUE).byteValue()).isEqualTo((byte) 0);
    assertThat(new ShortTag((short) 0x80).byteValue()).isEqualTo((byte) -128);
  }

  @Test
  void widensExactlyToWiderTypes() {
    // spec: core-nbt R38
    var tag = new ShortTag(Short.MIN_VALUE);
    assertThat(tag.shortValue()).isEqualTo((short) -32768);
    assertThat(tag.intValue()).isEqualTo(-32768);
    assertThat(tag.longValue()).isEqualTo(-32768L);
    assertThat(tag.floatValue()).isEqualTo(-32768.0f);
    assertThat(tag.doubleValue()).isEqualTo(-32768.0);
  }

  @Test
  void equalsShortOfSameValueOnly() {
    // spec: core-nbt R37
    assertThat(new ShortTag((short) 1)).isEqualTo(new ShortTag((short) 1));
    assertThat(new ShortTag((short) 1)).isNotEqualTo(new ByteTag((byte) 1));
    assertThat(new ShortTag((short) 1)).isNotEqualTo(new IntTag(1));
  }
}
