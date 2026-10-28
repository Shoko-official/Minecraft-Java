package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FloatTagTest {

  private static final int NEGATIVE_ZERO = 0x80000000;
  private static final int CANONICAL_NAN = 0x7fc00000;

  @Test
  void typeIsFloat() {
    // spec: core-nbt R1
    assertThat(new FloatTag(0).type()).isEqualTo(TagType.FLOAT);
  }

  @Test
  void ofKeepsBitsOfOrdinaryValuesZerosAndInfinities() {
    // spec: core-nbt R35
    assertThat(FloatTag.of(1.5f).bits()).isEqualTo(0x3fc00000);
    assertThat(FloatTag.of(-0.0f).bits()).isEqualTo(NEGATIVE_ZERO);
    assertThat(FloatTag.of(0.0f).bits()).isZero();
    assertThat(FloatTag.of(Float.POSITIVE_INFINITY).bits()).isEqualTo(0x7f800000);
    assertThat(FloatTag.of(Float.NEGATIVE_INFINITY).bits()).isEqualTo(0xff800000);
  }

  @Test
  void ofWritesEveryComputedNanAsCanonicalPattern() {
    // spec: core-nbt R35, provisional (a NaN produced by computation is 7fc00000)
    assertThat(FloatTag.of(Float.NaN).bits()).isEqualTo(CANONICAL_NAN);
    assertThat(FloatTag.of(Float.intBitsToFloat(0xffc00000)).bits()).isEqualTo(CANONICAL_NAN);
    assertThat(FloatTag.of(Float.intBitsToFloat(0x7fc00001)).bits()).isEqualTo(CANONICAL_NAN);
  }

  @Test
  void constructorKeepsAnyBitPattern() {
    // spec: core-nbt R35 (NaN payloads read from a file are kept)
    assertThat(new FloatTag(0x7fc00001).bits()).isEqualTo(0x7fc00001);
    assertThat(new FloatTag(0xffc00000).bits()).isEqualTo(0xffc00000);
  }

  @Test
  void floatValueKeepsNegativeZeroAndInfinity() {
    // spec: core-nbt R35, R38
    assertThat(Float.floatToRawIntBits(new FloatTag(NEGATIVE_ZERO).floatValue()))
        .isEqualTo(NEGATIVE_ZERO);
    assertThat(new FloatTag(0x7f800000).floatValue()).isEqualTo(Float.POSITIVE_INFINITY);
    assertThat(new FloatTag(CANONICAL_NAN).floatValue()).isNaN();
  }

  @Test
  void truncatesTowardZeroToIntAndLong() {
    // spec: core-nbt R38, probe PS2 (7.9 gives 7, -7.9 gives -7), provisional
    assertThat(FloatTag.of(7.9f).intValue()).isEqualTo(7);
    assertThat(FloatTag.of(-7.9f).intValue()).isEqualTo(-7);
    assertThat(FloatTag.of(7.9f).longValue()).isEqualTo(7L);
    assertThat(FloatTag.of(-7.9f).longValue()).isEqualTo(-7L);
    assertThat(FloatTag.of(-0.5f).intValue()).isZero();
  }

  @Test
  void nanConvertsToZero() {
    // spec: core-nbt R38, provisional
    var nan = new FloatTag(CANONICAL_NAN);
    assertThat(nan.intValue()).isZero();
    assertThat(nan.longValue()).isZero();
    assertThat(nan.shortValue()).isZero();
    assertThat(nan.byteValue()).isZero();
  }

  @Test
  void saturatesAtIntAndLongBounds() {
    // spec: core-nbt R38 (saturation at the bounds), provisional
    assertThat(FloatTag.of(Float.POSITIVE_INFINITY).intValue()).isEqualTo(Integer.MAX_VALUE);
    assertThat(FloatTag.of(Float.NEGATIVE_INFINITY).intValue()).isEqualTo(Integer.MIN_VALUE);
    assertThat(FloatTag.of(Float.POSITIVE_INFINITY).longValue()).isEqualTo(Long.MAX_VALUE);
    assertThat(FloatTag.of(Float.NEGATIVE_INFINITY).longValue()).isEqualTo(Long.MIN_VALUE);
    assertThat(FloatTag.of(3.0e9f).intValue()).isEqualTo(Integer.MAX_VALUE);
    assertThat(FloatTag.of(3.0e9f).longValue()).isEqualTo(3000000000L);
  }

  @Test
  void convertsToByteAndShortThroughInt() {
    // spec: core-nbt R38 (first to int, then low-order bits), provisional
    assertThat(FloatTag.of(300.5f).byteValue()).isEqualTo((byte) 44);
    assertThat(FloatTag.of(300.5f).shortValue()).isEqualTo((short) 300);
    assertThat(FloatTag.of(-129.9f).byteValue()).isEqualTo((byte) 127);
    // 1e10 saturates to Integer.MAX_VALUE (0x7fffffff), whose low-order bits are all ones
    assertThat(FloatTag.of(1.0e10f).byteValue()).isEqualTo((byte) -1);
    assertThat(FloatTag.of(1.0e10f).shortValue()).isEqualTo((short) -1);
  }

  @Test
  void widensExactlyToDouble() {
    // spec: core-nbt R38
    assertThat(FloatTag.of(7.9f).doubleValue()).isEqualTo((double) 7.9f);
    assertThat(Double.doubleToRawLongBits(new FloatTag(NEGATIVE_ZERO).doubleValue()))
        .isEqualTo(0x8000000000000000L);
  }

  @Test
  void equalityComparesBitPatterns() {
    // spec: core-nbt R37 (0.0 differs from -0.0, a NaN equals the same NaN pattern only)
    assertThat(new FloatTag(0)).isNotEqualTo(new FloatTag(NEGATIVE_ZERO));
    assertThat(new FloatTag(CANONICAL_NAN)).isEqualTo(new FloatTag(CANONICAL_NAN));
    assertThat(new FloatTag(CANONICAL_NAN)).hasSameHashCodeAs(new FloatTag(CANONICAL_NAN));
    assertThat(new FloatTag(CANONICAL_NAN)).isNotEqualTo(new FloatTag(0x7fc00001));
    assertThat(new FloatTag(0x3f800000)).isNotEqualTo(new DoubleTag(0x3ff0000000000000L));
  }
}
