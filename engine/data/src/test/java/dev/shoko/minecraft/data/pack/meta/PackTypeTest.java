package dev.shoko.minecraft.data.pack.meta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R1 (target formats: data 121.0, resources 97.1)
class PackTypeTest {

  @Test
  void dataPacksTargetFormat121_0() {
    assertThat(PackType.DATA.targetFormat()).isEqualTo(new PackFormat(121, 0));
  }

  @Test
  void resourcePacksTargetFormat97_1() {
    assertThat(PackType.RESOURCE.targetFormat()).isEqualTo(new PackFormat(97, 1));
  }
}
