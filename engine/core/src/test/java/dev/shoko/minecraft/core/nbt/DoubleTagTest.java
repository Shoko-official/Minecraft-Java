package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DoubleTagTest {

  private static final long NEGATIVE_ZERO = 0x8000000000000000L;
  private static final long CANONICAL_NAN = 0x7ff8000000000000L;

  @Test
  void typeIsDouble() {
    // spec: core-nbt R1
    assertThat(new DoubleTag(0L).type()).isEqualTo(TagType.DOUBLE);
  }

  @Test
  void ofKeepsBitsOfOrdinaryValuesZerosAndInfinities() {
    // spec: core-nbt R35
    assertThat(DoubleTag.of(1.5).bits()).isEqualTo(0x3ff8000000000000L);
    assertThat(DoubleTag.of(-0.0).bits()).isEqualTo(NEGATIVE_ZERO);
    assertThat(DoubleTag.of(0.0).bits()).isZero();
    assertThat(DoubleTag.of(Double.POSITIVE_INFINITY).bits()).isEqualTo(0x7ff0000000000000L);
    assertThat(DoubleTag.of(Double.NEGATIVE_INFINITY).bits()).isEqualTo(0xfff0000000000000L);
  }

  @Test
  void ofWritesEveryComputedNanAsCanonicalPattern() {
    // spec: core-nbt R35, provisional (a NaN produced by computation is 7ff8000000000000)
    assertThat(DoubleTag.of(Double.NaN).bits()).isEqualTo(CANONICAL_NAN);
    assertThat(DoubleTag.of(Double.longBitsToDouble(0xfff8000000000000L)).bits())
        .isEqualTo(CANONICAL_NAN);
    assertThat(DoubleTag.of(Double.longBitsToDouble(0x7ff8000000000001L)).bits())
        .isEqualTo(CANONICAL_NAN);
  }

  @Test
  void constructorKeepsAnyBitPattern() {
    // spec: core-nbt R35, probe PW2 (n4 = 7ff0000000000001)
    assertThat(new DoubleTag(0x7ff0000000000001L).bits()).isEqualTo(0x7ff0000000000001L);
  }

  @Test
  void doubleValueKeepsNegativeZero() {
    // spec: core-nbt R35, R38
    assertThat(Double.doubleToRawLongBits(new DoubleTag(NEGATIVE_ZERO).doubleValue()))
        .isEqualTo(NEGATIVE_ZERO);
    assertThat(new DoubleTag(CANONICAL_NAN).doubleValue()).isNaN();
  }

  @Test
  void truncatesTowardZeroToIntAndLong() {
    // spec: core-nbt R38, probe PS2 (7.9d gives 7, -7.9d gives -7), provisional
    assertThat(DoubleTag.of(7.9).intValue()).isEqualTo(7);
    assertThat(DoubleTag.of(-7.9).intValue()).isEqualTo(-7);
    assertThat(DoubleTag.of(7.9).longValue()).isEqualTo(7L);
    assertThat(DoubleTag.of(-7.9).longValue()).isEqualTo(-7L);
  }

  @Test
  void nanConvertsToZero() {
    // spec: core-nbt R38, provisional
    var nan = new DoubleTag(CANONICAL_NAN);
    assertThat(nan.intValue()).isZero();
    assertThat(nan.longValue()).isZero();
    assertThat(nan.shortValue()).isZero();
    assertThat(nan.byteValue()).isZero();
  }

  @Test
  void saturatesAtIntAndLongBounds() {
    // spec: core-nbt R38, provisional
    assertThat(DoubleTag.of(1.0e10).intValue()).isEqualTo(Integer.MAX_VALUE);
    assertThat(DoubleTag.of(-1.0e10).intValue()).isEqualTo(Integer.MIN_VALUE);
    assertThat(DoubleTag.of(1.0e10).longValue()).isEqualTo(10000000000L);
    assertThat(DoubleTag.of(1.0e19).longValue()).isEqualTo(Long.MAX_VALUE);
    assertThat(DoubleTag.of(-1.0e19).longValue()).isEqualTo(Long.MIN_VALUE);
  }

  @Test
  void convertsToByteAndShortThroughInt() {
    // spec: core-nbt R38 (first to int, then low-order bits), provisional
    assertThat(DoubleTag.of(70000.9).shortValue()).isEqualTo((short) 4464);
    assertThat(DoubleTag.of(-129.9).byteValue()).isEqualTo((byte) 127);
    assertThat(DoubleTag.of(1.0e10).byteValue()).isEqualTo((byte) -1);
  }

  @Test
  void roundsToNearestFloat() {
    // spec: core-nbt R38 (double to float: IEEE round to nearest)
    assertThat(DoubleTag.of(0.1).floatValue()).isEqualTo(0.1f);
    assertThat(DoubleTag.of(-7.9).floatValue()).isEqualTo(-7.9f);
    assertThat(DoubleTag.of(1.0e300).floatValue()).isEqualTo(Float.POSITIVE_INFINITY);
    assertThat(DoubleTag.of(-1.0e300).floatValue()).isEqualTo(Float.NEGATIVE_INFINITY);
    assertThat(Float.floatToRawIntBits(new DoubleTag(NEGATIVE_ZERO).floatValue()))
        .isEqualTo(0x80000000);
  }

  @Test
  void equalityComparesBitPatterns() {
    // spec: core-nbt R37
    assertThat(new DoubleTag(0L)).isNotEqualTo(new DoubleTag(NEGATIVE_ZERO));
    assertThat(new DoubleTag(CANONICAL_NAN)).isEqualTo(new DoubleTag(CANONICAL_NAN));
    assertThat(new DoubleTag(CANONICAL_NAN)).hasSameHashCodeAs(new DoubleTag(CANONICAL_NAN));
    assertThat(new DoubleTag(CANONICAL_NAN)).isNotEqualTo(new DoubleTag(0x7ff0000000000001L));
  }
}
