package dev.shoko.minecraft.net.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.frame.Cfb8Ciphers;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.frame.FrameDecoder;
import dev.shoko.minecraft.net.frame.FrameEncoder;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Wire;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: net-protocol-777 R28 (compression applies to "tous les paquets suivants"), R31 and R32
// (encryption starts right after key, the next bytes are encrypted), test plan L1 (the result does
// not depend on how the bytes are cut). The receiver reacts to a frame between two reads, as a
// connection does when it sees login_compression or key: a frame that completes in the same read
// as the frame that changes the state must still be read with the new state.
class FrameStateChangeHuntTest {

  private static final byte[] SECRET = Hex.ramp(16);
  private static final int LOGIN_COMPRESSION = 3;
  private static final int KEY = 1;
  private static final boolean CLIENT = true;
  private static final boolean SERVER = false;

  private static void react(Frame frame, FrameDecoder decoder, boolean client) {
    if (client && frame.packetId() == LOGIN_COMPRESSION) {
      try {
        decoder.setCompressionThreshold(frame.reader().readVarInt());
      } catch (ProtocolException e) {
        throw new IllegalStateException(e);
      }
    } else if (!client && frame.packetId() == KEY) {
      decoder.enableDecryption(Cfb8Ciphers.decryptor(SECRET));
    }
  }

  /**
   * Cuts the wire in chunks of {@code chunk} bytes, reacting to every frame as soon as it is out: a
   * client reacts to login_compression, a server to key.
   */
  private static List<Frame> receive(byte[] wire, int chunk, boolean client)
      throws ProtocolException {
    FrameDecoder decoder = FrameDecoder.create();
    List<Frame> all = new ArrayList<>();
    for (int position = 0; position < wire.length; position += chunk) {
      int length = Math.min(chunk, wire.length - position);
      for (Frame frame : decoder.feed(wire, position, length)) {
        all.add(frame);
        react(frame, decoder, client);
      }
    }
    return all;
  }

  private static byte[] clientWire(byte[] bigPayload) {
    return Hex.concat(
        Wire.frame(LOGIN_COMPRESSION, Wire.varInt(256)),
        Wire.compressedFrame(2, bigPayload, 256),
        Wire.compressedFrame(4, Hex.ramp(10), 256));
  }

  private static Frame keyFrame() {
    return Frame.of(
        KEY, Hex.concat(Wire.prefixedBytes(Hex.ramp(8)), Wire.prefixedBytes(Hex.ramp(8))));
  }

  private static byte[] serverWire() throws ProtocolException {
    FrameEncoder encoder = FrameEncoder.create();
    encoder.enableEncryption(Cfb8Ciphers.encryptor(SECRET));
    return Hex.concat(
        Wire.frame(KEY, keyFrame().payload()),
        encoder.encode(Frame.of(7, new byte[0])),
        encoder.encode(Frame.of(9, Hex.ramp(50))));
  }

  @Test
  @Timeout(30)
  void provisionalLoginCompressionAndTheNextCompressedFrameInOneSegmentAreReadAlike()
      throws ProtocolException {
    // spec: R28, R32. Today feed() decodes every complete frame of a call with the state it had at
    // the call: the second frame is read as a plain frame (silent corruption) when both arrive in
    // one segment, and correctly when they arrive apart.
    byte[] bigPayload = Hex.filled('x', 400);
    List<Frame> expected =
        List.of(
            Frame.of(LOGIN_COMPRESSION, Wire.varInt(256)),
            Frame.of(2, bigPayload),
            Frame.of(4, Hex.ramp(10)));
    byte[] wire = clientWire(bigPayload);
    assertThat(receive(wire, 1, CLIENT)).as("byte by byte").isEqualTo(expected);
    for (int chunk : new int[] {2, 3, 5, 8, 13, wire.length}) {
      assertThat(receive(wire, chunk, CLIENT)).as("chunks of %d", chunk).isEqualTo(expected);
    }
  }

  @Test
  @Timeout(30)
  void provisionalKeyAndTheFirstEncryptedFramesInOneSegmentAreReadAlike() throws ProtocolException {
    // spec: R31, R32. The bytes after key are encrypted; if they arrive with key they must still
    // be decrypted, whatever the cut.
    List<Frame> expected = List.of(keyFrame(), Frame.of(7, new byte[0]), Frame.of(9, Hex.ramp(50)));
    byte[] wire = serverWire();
    assertThat(receive(wire, 1, SERVER)).as("byte by byte").isEqualTo(expected);
    for (int chunk : new int[] {2, 7, 20, 30, wire.length}) {
      assertThat(receive(wire, chunk, SERVER)).as("chunks of %d", chunk).isEqualTo(expected);
    }
  }

  @Test
  @Timeout(30)
  void provisionalTheResultDoesNotDependOnTheCutOfTheWire() throws ProtocolException {
    // spec: test plan L1 (idempotence of the splitting into frames), applied to both switches
    byte[] client = clientWire(Hex.filled('y', 1000));
    byte[] server = serverWire();
    List<Frame> clientReference = receive(client, 1, CLIENT);
    List<Frame> serverReference = receive(server, 1, SERVER);
    for (int chunk = 1; chunk <= 64; chunk += 7) {
      assertThat(receive(client, chunk, CLIENT))
          .as("client, chunks of %d", chunk)
          .isEqualTo(clientReference);
      assertThat(receive(server, chunk, SERVER))
          .as("server, chunks of %d", chunk)
          .isEqualTo(serverReference);
    }
  }
}
