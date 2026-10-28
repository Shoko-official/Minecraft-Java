package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ByteTagTest {

  @Test
  void typeIsByte() {
    // spec: core-nbt R1
    assertThat(new ByteTag((byte) 1).type()).isEqualTo(TagType.BYTE);
  }

  @Test
  void keepsExtremeValues() {
    // spec: core-nbt "Cas limites" (Byte -128 and 127)
    assertThat(new ByteTag(Byte.MIN_VALUE).value()).isEqualTo((byte) -128);
    assertThat(new ByteTag(Byte.MAX_VALUE).value()).isEqualTo((byte) 127);
  }

  @Test
  void widensExactlyToEveryNumericType() {
    // spec: core-nbt R38 (integer to wider integer exact, integer to float or double nearest)
    var tag = new ByteTag((byte) -128);
    assertThat(tag.byteValue()).isEqualTo((byte) -128);
    assertThat(tag.shortValue()).isEqualTo((short) -128);
    assertThat(tag.intValue()).isEqualTo(-128);
    assertThat(tag.longValue()).isEqualTo(-128L);
    assertThat(tag.floatValue()).isEqualTo(-128.0f);
    assertThat(tag.doubleValue()).isEqualTo(-128.0);
  }

  @Test
  void equalsByteOfSameValueOnly() {
    // spec: core-nbt R37 (same type required: Int 1 differs from Byte 1)
    assertThat(new ByteTag((byte) 1)).isEqualTo(new ByteTag((byte) 1));
    assertThat(new ByteTag((byte) 1)).hasSameHashCodeAs(new ByteTag((byte) 1));
    assertThat(new ByteTag((byte) 1)).isNotEqualTo(new ByteTag((byte) 0));
    assertThat(new ByteTag((byte) 1)).isNotEqualTo(new IntTag(1));
  }
}
