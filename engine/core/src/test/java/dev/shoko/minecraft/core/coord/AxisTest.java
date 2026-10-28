package dev.shoko.minecraft.core.coord;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AxisTest {

  // spec: core-math R34 (D): axes x, y, z; horizontal axes x and z
  @ParameterizedTest(name = "{0}: name {1}, horizontal {2}")
  @CsvSource({"X, x, true", "Y, y, false", "Z, z, true"})
  void namesAndHorizontalityMatchTheData(Axis axis, String name, boolean horizontal) {
    assertThat(axis.serializedName()).isEqualTo(name);
    assertThat(axis.isHorizontal()).isEqualTo(horizontal);
  }

  // spec: core-math R34 (D): exactly three axes
  @Test
  void thereAreExactlyThreeAxes() {
    assertThat(Axis.values()).containsExactly(Axis.X, Axis.Y, Axis.Z);
  }
}
