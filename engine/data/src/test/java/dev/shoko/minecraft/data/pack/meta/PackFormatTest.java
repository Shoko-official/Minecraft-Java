package dev.shoko.minecraft.data.pack.meta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R5 (formats are compared major first, then minor)
class PackFormatTest {

  @Test
  void provisionalFormatsCompareMajorThenMinor() {
    assertThat(new PackFormat(121, 0).compareTo(new PackFormat(121, 1))).isNegative();
    assertThat(new PackFormat(121, 1).compareTo(new PackFormat(121, 0))).isPositive();
    assertThat(new PackFormat(120, 5).compareTo(new PackFormat(121, 0))).isNegative();
    assertThat(new PackFormat(122, 0).compareTo(new PackFormat(121, Integer.MAX_VALUE)))
        .isPositive();
    assertThat(new PackFormat(121, 3).compareTo(new PackFormat(121, 3))).isZero();
    assertThat(new PackFormat(97, Integer.MAX_VALUE).compareTo(new PackFormat(98, 0))).isNegative();
  }

  @Test
  void provisionalFormatsCompareAsSignedIntegers() {
    assertThat(new PackFormat(-1, 0).compareTo(new PackFormat(0, 0))).isNegative();
    assertThat(new PackFormat(1, -5).compareTo(new PackFormat(1, 0))).isNegative();
  }

  @Test
  void aFormatIsAValueOfTwoNumbers() {
    PackFormat format = new PackFormat(121, 0);
    assertThat(format.major()).isEqualTo(121);
    assertThat(format.minor()).isZero();
    assertThat(format).isEqualTo(new PackFormat(121, 0)).isNotEqualTo(new PackFormat(121, 1));
  }
}
