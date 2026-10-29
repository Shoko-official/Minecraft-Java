package dev.shoko.minecraft.world.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** CF-R15 and CF-R30: absence of an array is a state of its own. */
class SectionLightTest {

  @Test
  void noneHasNeitherArray() {
    // spec: CF-R15
    SectionLight light = SectionLight.none();

    assertThat(light.blockLight()).isEmpty();
    assertThat(light.skyLight()).isEmpty();
  }

  @Test
  void arraysAreSetAndClearedIndependently() {
    // spec: SectionLight contract
    SectionLight light = SectionLight.none();

    light.setBlockLight(NibbleArray.filled(3));
    assertThat(light.blockLight()).isPresent();
    assertThat(light.skyLight()).isEmpty();

    light.setSkyLight(NibbleArray.filled(15));
    light.clearBlockLight();
    assertThat(light.blockLight()).isEmpty();
    assertThat(light.skyLight()).isPresent();

    light.clearSkyLight();
    assertThat(light.skyLight()).isEmpty();
  }

  @Test
  void arraysAreHeldWithoutCopy() {
    // spec: SectionLight.setBlockLight and setSkyLight contracts (held without copy)
    SectionLight light = SectionLight.none();
    NibbleArray sky = NibbleArray.zeros();
    NibbleArray block = NibbleArray.zeros();
    light.setSkyLight(sky);
    light.setBlockLight(block);

    sky.set(5, 9);
    block.set(6, 4);

    assertThat(light.skyLight().orElseThrow().get(5)).isEqualTo(9);
    assertThat(light.blockLight().orElseThrow().get(6)).isEqualTo(4);
  }

  @Test
  void zeroedSkyLightIsNotAbsentSkyLight() {
    // spec: CF-R30 (absent is distinct from present and all zero)
    SectionLight light = SectionLight.none();

    light.setSkyLight(NibbleArray.zeros());

    assertThat(light.skyLight()).isPresent();
    assertThat(light.skyLight().orElseThrow().toBytes()).containsOnly((byte) 0);
  }

  @Test
  void replacingAnArrayKeepsTheLatest() {
    SectionLight light = SectionLight.none();

    light.setSkyLight(NibbleArray.filled(1));
    light.setSkyLight(NibbleArray.filled(2));

    assertThat(light.skyLight().orElseThrow().get(0)).isEqualTo(2);
  }
}
