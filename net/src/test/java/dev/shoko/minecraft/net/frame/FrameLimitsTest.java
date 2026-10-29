package dev.shoko.minecraft.net.frame;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R27 (2^21 - 1), R30 d (2^23)
class FrameLimitsTest {

  @Test
  void theDefaultLimitsAreTheRulesOfTheSpec() {
    assertThat(FrameLimits.MAX_FRAME_LENGTH).isEqualTo(2_097_151);
    assertThat(FrameLimits.MAX_DECOMPRESSED_LENGTH).isEqualTo(8_388_608);
    assertThat(FrameLimits.DEFAULT.maxFrameLength()).isEqualTo(2_097_151);
    assertThat(FrameLimits.DEFAULT.maxDecompressedLength()).isEqualTo(8_388_608);
  }

  @Test
  void theMaximumFrameLengthIsTheLargestValueOfThreeVarIntBytes() {
    // spec: R27 (the length fits in 3 bytes at most)
    assertThat(FrameLimits.MAX_FRAME_LENGTH).isEqualTo((1 << 21) - 1);
  }
}
