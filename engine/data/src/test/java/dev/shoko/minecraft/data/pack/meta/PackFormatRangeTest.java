package dev.shoko.minecraft.data.pack.meta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R1, R5, table "pack.mcmeta" (classification of a format range)
class PackFormatRangeTest {

  private static final int ALL_MINORS = Integer.MAX_VALUE;

  private static PackFormatRange range(int minMajor, int minMinor, int maxMajor, int maxMinor) {
    return new PackFormatRange(
        new PackFormat(minMajor, minMinor), new PackFormat(maxMajor, maxMinor));
  }

  private static PackCompatibility data(PackFormatRange range) {
    return range.compatibilityWith(PackType.DATA.targetFormat());
  }

  @Test
  void provisionalSingleIntegerBoundsCoverEveryMinor() {
    // spec: table "pack.mcmeta" row 121 / 121: min (121, 0) and max (121, MAX) contain (121, 0)
    assertThat(data(range(121, 0, 121, ALL_MINORS))).isEqualTo(PackCompatibility.COMPATIBLE);
  }

  @Test
  void provisionalExactBoundsOfTheTargetAreCompatible() {
    // spec: table row [121, 0] / [121, 0]
    assertThat(data(range(121, 0, 121, 0))).isEqualTo(PackCompatibility.COMPATIBLE);
  }

  @Test
  void provisionalAnOlderMinimumIsCompatible() {
    // spec: table row 120 / 121 and [120, 5] / [121, 0]
    assertThat(data(range(120, 0, 121, ALL_MINORS))).isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(data(range(120, 5, 121, 0))).isEqualTo(PackCompatibility.COMPATIBLE);
  }

  @Test
  void provisionalAMinimumAboveTheTargetIsTooNew() {
    // spec: table row [121, 1] / 121 ("incompatible, plus recent"); R5
    assertThat(data(range(121, 1, 121, ALL_MINORS))).isEqualTo(PackCompatibility.TOO_NEW);
    assertThat(data(range(122, 0, 123, 0))).isEqualTo(PackCompatibility.TOO_NEW);
  }

  @Test
  void provisionalAMaximumBelowTheTargetIsTooOld() {
    // spec: table row 100 / 120 ("incompatible, plus ancien"); R5
    assertThat(data(range(100, 0, 120, ALL_MINORS))).isEqualTo(PackCompatibility.TOO_OLD);
    assertThat(data(range(1, 0, 1, 0))).isEqualTo(PackCompatibility.TOO_OLD);
  }

  @Test
  void provisionalAMinimumAboveTheMaximumIsBroken() {
    // spec: table row 122 / 121 ("casse"); R5; BROKEN wins over TOO_NEW and TOO_OLD
    assertThat(data(range(122, 0, 121, ALL_MINORS))).isEqualTo(PackCompatibility.BROKEN);
    assertThat(data(range(120, 0, 110, 0))).isEqualTo(PackCompatibility.BROKEN);
    assertThat(data(range(130, 0, 125, 0))).isEqualTo(PackCompatibility.BROKEN);
    assertThat(data(range(121, 1, 121, 0))).isEqualTo(PackCompatibility.BROKEN);
  }

  @Test
  void provisionalBoundsAreInclusive() {
    // spec: R5 (min <= target <= max)
    assertThat(data(range(121, 0, 130, 0))).isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(data(range(100, 0, 121, 0))).isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(data(range(121, 0, 121, 0))).isEqualTo(PackCompatibility.COMPATIBLE);
  }

  @Test
  void provisionalTheMinorNumberDecidesOnTheTargetMajor() {
    // spec: R5 ((major, minor) lexicographic): max (120, MAX) is below (121, 0)
    assertThat(data(range(100, 0, 120, ALL_MINORS))).isEqualTo(PackCompatibility.TOO_OLD);
    assertThat(data(range(121, 0, 121, 0))).isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(data(range(121, 1, 121, 1))).isEqualTo(PackCompatibility.TOO_NEW);
  }

  @Test
  void provisionalResourcePacksAreClassifiedAgainst97_1() {
    // spec: R1, R5
    PackFormat target = PackType.RESOURCE.targetFormat();
    assertThat(range(97, 0, 97, ALL_MINORS).compatibilityWith(target))
        .isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(range(97, 0, 97, 0).compatibilityWith(target)).isEqualTo(PackCompatibility.TOO_OLD);
    assertThat(range(97, 2, 98, 0).compatibilityWith(target)).isEqualTo(PackCompatibility.TOO_NEW);
    assertThat(range(121, 0, 121, 0).compatibilityWith(target))
        .isEqualTo(PackCompatibility.TOO_NEW);
  }

  @Test
  void provisionalAnArbitraryTargetIsAccepted() {
    // spec: PackFormatRange.compatibilityWith Javadoc
    assertThat(range(5, 0, 7, 3).compatibilityWith(new PackFormat(6, 9)))
        .isEqualTo(PackCompatibility.COMPATIBLE);
    assertThat(range(5, 0, 7, 3).compatibilityWith(new PackFormat(7, 4)))
        .isEqualTo(PackCompatibility.TOO_OLD);
    assertThat(range(5, 0, 7, 3).compatibilityWith(new PackFormat(4, 99)))
        .isEqualTo(PackCompatibility.TOO_NEW);
  }
}
