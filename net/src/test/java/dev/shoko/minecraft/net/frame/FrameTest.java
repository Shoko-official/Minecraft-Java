package dev.shoko.minecraft.net.frame;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.buffer.PacketReader;
import dev.shoko.minecraft.net.testing.Hex;
import org.junit.jupiter.api.Test;

// spec: net-protocol-777 R27 (a frame is a packet id and a payload), R90 (reader bounded to the
// frame)
class FrameTest {

  @Test
  void aFrameKeepsItsIdAndPayload() {
    Frame frame = Frame.of(0x7c, Hex.bytes("01 02 03"));
    assertThat(frame.packetId()).isEqualTo(0x7c);
    assertThat(frame.payload()).containsExactly(1, 2, 3);
  }

  @Test
  void aFrameWithoutFieldsHasAnEmptyPayload() {
    // spec: R27 (vector F02: a status_request has no field)
    assertThat(Frame.of(0, new byte[0]).payload()).isEmpty();
  }

  @Test
  void theConstructorAndTheAccessorCopyThePayload() {
    // spec: API contract of Frame (immutable)
    byte[] source = Hex.bytes("01 02 03");
    Frame frame = Frame.of(1, source);
    source[0] = 0x7f;
    byte[] first = frame.payload();
    first[1] = 0x7f;
    assertThat(frame.payload()).containsExactly(1, 2, 3);
  }

  @Test
  void theReaderIsBoundedToThePayload() throws ProtocolException {
    // spec: R90
    PacketReader reader = Frame.of(4, Hex.bytes("11 22 33 44 55 66 77 88")).reader();
    assertThat(reader.remaining()).isEqualTo(8);
    assertThat(reader.readLong()).isEqualTo(0x1122334455667788L);
    reader.requireFullyConsumed();
  }

  @Test
  void eachCallToReaderStartsAtTheFirstPayloadByte() throws ProtocolException {
    // spec: API contract of reader (a new reader)
    Frame frame = Frame.of(0, Hex.bytes("2a"));
    assertThat(frame.reader().readUnsignedByte()).isEqualTo(42);
    assertThat(frame.reader().readUnsignedByte()).isEqualTo(42);
  }

  @Test
  void equalityIsOnIdAndPayloadBytes() {
    Frame a = Frame.of(3, Hex.bytes("01 02"));
    Frame b = Frame.of(3, Hex.bytes("01 02"));
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    assertThat(a).isNotEqualTo(Frame.of(4, Hex.bytes("01 02")));
    assertThat(a).isNotEqualTo(Frame.of(3, Hex.bytes("01 03")));
    assertThat(a).isNotEqualTo(Frame.of(3, Hex.bytes("01")));
    assertThat(a).isNotEqualTo(null).isNotEqualTo("frame");
  }

  @Test
  void theDescriptionIsNotEmpty() {
    // spec: API contract of toString (id and payload length)
    assertThat(Frame.of(3, new byte[12]).toString()).isNotEmpty();
  }
}
