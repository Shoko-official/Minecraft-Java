package dev.shoko.minecraft.data.pack;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R40 (sources displayed "built-in", "feature", "world")
class PackOriginTest {

  @Test
  void provisionalEachOriginHasTheTextTheGameShows() {
    assertThat(PackOrigin.BUILT_IN.label()).isEqualTo("built-in");
    assertThat(PackOrigin.FEATURE.label()).isEqualTo("feature");
    assertThat(PackOrigin.WORLD.label()).isEqualTo("world");
  }
}
