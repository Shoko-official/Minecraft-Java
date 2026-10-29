package dev.shoko.minecraft.net;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException.Reason;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R1 (protocol 777, version name 26.3)
class ProtocolVersionTest {

  @Test
  void theProtocolIs777AndTheGameVersion263() {
    // spec: R1
    assertThat(ProtocolVersion.PROTOCOL).isEqualTo(777);
    assertThat(ProtocolVersion.GAME_VERSION).isEqualTo("26.3");
  }

  @Test
  void aProtocolExceptionKeepsItsReasonAndCause() {
    // spec: API contract of ProtocolException (callers rely on reason() and getCause())
    IllegalStateException cause = new IllegalStateException("zlib");
    ProtocolException failure =
        new ProtocolException(Reason.CORRUPT_COMPRESSION, "bad stream", cause);
    assertThat(failure.reason()).isEqualTo(Reason.CORRUPT_COMPRESSION);
    assertThat(failure.getCause()).isSameAs(cause);
    ProtocolException plain = new ProtocolException(Reason.UNEXPECTED_END, "short");
    assertThat(plain.reason()).isEqualTo(Reason.UNEXPECTED_END);
    assertThat(plain.getCause()).isNull();
  }
}
