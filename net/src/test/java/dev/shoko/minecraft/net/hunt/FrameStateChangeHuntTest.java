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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: net-protocol-777 R28 (compression applies to "tous les paquets suivants"), R31 and R32
// (encryption starts right after key, the next bytes are encrypted), R30 d and R91 (volume), test
// plan L1 (the result does not depend on how the bytes are cut). The receiver stores the bytes with
// append and reads one frame at a time with poll, reacting between two polls as a connection does
// when it sees login_compression or key: a frame that is already buffered when the frame that
// changes the mode is read must still be read with the new mode.
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
   * Cuts the wire in chunks of {@code chunk} bytes, appends each chunk, then polls every frame it
   * completes, reacting after each one: a client to login_compression, a server to key.
   */
  private static List<Frame> receive(byte[] wire, int chunk, boolean client)
      throws ProtocolException {
    FrameDecoder decoder = FrameDecoder.create();
    List<Frame> all = new ArrayList<>();
    for (int position = 0; position < wire.length; position += chunk) {
      decoder.append(wire, position, Math.min(chunk, wire.length - position));
      for (Optional<Frame> frame = decoder.poll(); frame.isPresent(); frame = decoder.poll()) {
        all.add(frame.get());
        react(frame.get(), decoder, client);
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
  void loginCompressionAndTheNextCompressedFrameInOneSegmentAreReadAlike()
      throws ProtocolException {
    // spec: R28, R32. login_compression and a compressed frame arrive in one segment.
    byte[] bigPayload = Hex.filled('x', 400);
    List<Frame> expected =
        List.of(
            Frame.of(LOGIN_COMPRESSION, Wire.varInt(256)),
            Frame.of(2, bigPayload),
            Frame.of(4, Hex.ramp(10)));
    byte[] wire = clientWire(bigPayload);
    assertThat(receive(wire, 1, CLIENT)).as("byte by byte").isEqualTo(expected);
    for (int chunk : new int[] {2, 3, 5, 7, 8, 13, 20, 30, 64, wire.length}) {
      assertThat(receive(wire, chunk, CLIENT)).as("chunks of %d", chunk).isEqualTo(expected);
    }
  }

  @Test
  @Timeout(30)
  void keyAndTheFirstEncryptedFramesInOneSegmentAreReadAlike() throws ProtocolException {
    // spec: R31, R32. The bytes after key are encrypted; if they arrive with key they must still
    // be decrypted, whatever the cut.
    List<Frame> expected = List.of(keyFrame(), Frame.of(7, new byte[0]), Frame.of(9, Hex.ramp(50)));
    byte[] wire = serverWire();
    assertThat(receive(wire, 1, SERVER)).as("byte by byte").isEqualTo(expected);
    for (int chunk : new int[] {2, 3, 5, 7, 8, 13, 20, 30, 64, wire.length}) {
      assertThat(receive(wire, chunk, SERVER)).as("chunks of %d", chunk).isEqualTo(expected);
    }
  }

  @Test
  @Timeout(30)
  void theResultDoesNotDependOnTheCutOfTheWire() throws ProtocolException {
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

  /** Frames and bytes of a stream with a threshold already set: no mode change to react to. */
  private static byte[] plainModeWire(List<Frame> frames) throws ProtocolException {
    FrameEncoder encoder = FrameEncoder.create();
    encoder.setCompressionThreshold(64);
    byte[] wire = new byte[0];
    for (Frame frame : frames) {
      wire = Hex.concat(wire, encoder.encode(frame));
    }
    return wire;
  }

  @Test
  @Timeout(30)
  void feedAndAppendThenPollGiveTheSameFramesWhenTheModeDoesNotChange() throws ProtocolException {
    // spec: test plan L1. Without a mode change between two frames, feed is append then poll.
    List<Frame> frames = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      frames.add(Frame.of(i, Hex.filled('a' + i, i * 40)));
    }
    byte[] wire = plainModeWire(frames);
    for (int chunk : new int[] {1, 7, 100, wire.length}) {
      FrameDecoder fed = FrameDecoder.create();
      fed.setCompressionThreshold(64);
      FrameDecoder polled = FrameDecoder.create();
      polled.setCompressionThreshold(64);
      List<Frame> fromFeed = new ArrayList<>();
      List<Frame> fromPoll = new ArrayList<>();
      for (int position = 0; position < wire.length; position += chunk) {
        int length = Math.min(chunk, wire.length - position);
        fromFeed.addAll(fed.feed(wire, position, length));
        polled.append(wire, position, length);
        for (Optional<Frame> frame = polled.poll(); frame.isPresent(); frame = polled.poll()) {
          fromPoll.add(frame.get());
        }
      }
      assertThat(fromFeed).as("feed, chunks of %d", chunk).isEqualTo(frames);
      assertThat(fromPoll).as("poll, chunks of %d", chunk).isEqualTo(frames);
    }
  }

  @Test
  @Timeout(30)
  void feedStopsAfterTheFrameThatCrossesThePayloadCapAndAnEmptyFeedGivesTheRest()
      throws ProtocolException {
    // spec: R30 d, R91. One feed does not materialize more than about MAX_FEED_PAYLOAD payload
    // bytes: 8 frames of 1 MiB come out over several calls, in order, none lost.
    int size = FrameDecoder.MAX_FEED_PAYLOAD;
    List<Frame> frames = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      frames.add(Frame.of(10 + i, Hex.filled(i, size)));
    }
    byte[] wire = plainModeWire(frames);
    assertThat(wire.length).as("the wire is small, the frames are not").isLessThan(1 << 20);

    FrameDecoder fed = FrameDecoder.create();
    fed.setCompressionThreshold(64);
    List<Frame> first = fed.feed(wire, 0, wire.length);
    assertThat(first).as("first call").hasSizeBetween(1, 2);
    List<Frame> all = new ArrayList<>(first);
    int calls = 0;
    while (all.size() < frames.size() && calls++ < 20) {
      all.addAll(fed.feed(new byte[0], 0, 0));
    }
    assertThat(all).isEqualTo(frames);
    assertThat(fed.bufferedBytes()).isZero();

    FrameDecoder polled = FrameDecoder.create();
    polled.setCompressionThreshold(64);
    polled.append(wire, 0, wire.length);
    List<Frame> fromPoll = new ArrayList<>();
    for (Optional<Frame> frame = polled.poll(); frame.isPresent(); frame = polled.poll()) {
      fromPoll.add(frame.get());
    }
    assertThat(all).isEqualTo(fromPoll);
  }
}
