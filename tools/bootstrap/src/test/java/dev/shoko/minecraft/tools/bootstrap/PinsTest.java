package dev.shoko.minecraft.tools.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class PinsTest {

  @Test
  void pinsDescribeTheTargetVersion() throws IOException {
    Pins pins = Pins.load();

    assertThat(pins.gameVersion()).isEqualTo("26.3");
    assertThat(pins.worldVersion()).isEqualTo(5023);
    assertThat(pins.protocolVersion()).isEqualTo(777);
    assertThat(pins.packVersion()).isEqualTo(new Pins.PackVersion("121.0", "97.1"));
    assertThat(pins.versionJson().sha1()).hasSize(40).matches("[0-9a-f]+");
    assertThat(pins.server().sha1()).hasSize(40).matches("[0-9a-f]+");
    assertThat(pins.server().sha256()).hasSize(64).matches("[0-9a-f]+");
    assertThat(pins.innerServerSha256()).hasSize(64).matches("[0-9a-f]+");
    assertThat(pins.server().size()).isPositive();
  }

  @Test
  void versionJsonUrlEmbedsItsOwnSha1() throws IOException {
    Pins pins = Pins.load();

    assertThat(pins.versionJson().url()).contains(pins.versionJson().sha1());
  }
}
