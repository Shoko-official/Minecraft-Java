package dev.shoko.minecraft.core.nbt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class NbtExceptionTest {

  @Test
  void locatedFailureKeepsReasonAndOffset() {
    // spec: core-nbt "Entrees / sorties" (format error carries the offset of the faulty byte)
    var failure = new NbtException(Reason.TOO_DEEP, 2563L, "too deep");
    assertThat(failure.reason()).isEqualTo(Reason.TOO_DEEP);
    assertThat(failure.offset()).hasValue(2563L);
    assertThat(failure).hasMessage("too deep");
  }

  @Test
  void offsetZeroIsAPosition() {
    var failure = new NbtException(Reason.UNKNOWN_TYPE, 0L, "type 13");
    assertThat(failure.offset()).hasValue(0L);
  }

  @Test
  void failureWithoutPositionHasNoOffset() {
    // spec: core-nbt R13 (STRING_TOO_LONG has no offset)
    var failure = new NbtException(Reason.STRING_TOO_LONG, "too long");
    assertThat(failure.reason()).isEqualTo(Reason.STRING_TOO_LONG);
    assertThat(failure.offset()).isEmpty();
  }

  @Test
  void causedFailureKeepsCauseAndHasNoOffset() {
    var cause = new IOException("disk");
    var failure = new NbtException(Reason.IO_FAILURE, "read failed", cause);
    assertThat(failure.reason()).isEqualTo(Reason.IO_FAILURE);
    assertThat(failure.offset()).isEmpty();
    assertThat(failure).hasCause(cause);
  }
}
