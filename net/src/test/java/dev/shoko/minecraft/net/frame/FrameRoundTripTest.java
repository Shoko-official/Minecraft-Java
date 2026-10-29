package dev.shoko.minecraft.net.frame;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.testing.Hex;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: net-protocol-777 test plan L1 (idempotence of the splitting into frames), R27 to R32
class FrameRoundTripTest {

  private static final byte[] SECRET = Hex.ramp(16);

  private static List<Frame> randomFrames(Random random, int count) {
    List<Frame> frames = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      int size = random.nextInt(4) == 0 ? random.nextInt(3000) : random.nextInt(40);
      byte[] payload = new byte[size];
      if (random.nextBoolean()) {
        random.nextBytes(payload);
      } else {
        java.util.Arrays.fill(payload, (byte) random.nextInt(4));
      }
      frames.add(Frame.of(random.nextInt(300), payload));
    }
    return frames;
  }

  private static List<Frame> decodeInRandomChunks(FrameDecoder decoder, byte[] data, Random random)
      throws ProtocolException {
    List<Frame> out = new ArrayList<>();
    int position = 0;
    while (position < data.length) {
      int step =
          Math.min(data.length - position, 1 + random.nextInt(random.nextBoolean() ? 5 : 700));
      out.addAll(decoder.feed(data, position, step));
      position += step;
    }
    return out;
  }

  @Test
  @Timeout(60)
  void framesSurviveEncodingAndDecodingForEveryThresholdWithAndWithoutEncryption()
      throws ProtocolException {
    // spec: R27 to R32, test plan L1 (idempotence, fixed seed)
    Random random = new Random(0xF2A3EL);
    int[] thresholds = {-1, 0, 1, 20, 256, 1000};
    for (int threshold : thresholds) {
      for (boolean encrypted : new boolean[] {false, true}) {
        FrameEncoder encoder = FrameEncoder.create();
        FrameDecoder decoder = FrameDecoder.create();
        encoder.setCompressionThreshold(threshold);
        decoder.setCompressionThreshold(threshold);
        if (encrypted) {
          encoder.enableEncryption(Cfb8Ciphers.encryptor(SECRET));
          decoder.enableDecryption(Cfb8Ciphers.decryptor(SECRET));
        }
        List<Frame> frames = randomFrames(random, 120);
        ByteArrayOutputStream wire = new ByteArrayOutputStream();
        for (Frame frame : frames) {
          wire.writeBytes(encoder.encode(frame));
        }
        List<Frame> decoded = decodeInRandomChunks(decoder, wire.toByteArray(), random);
        assertThat(decoded)
            .as("threshold %d, encrypted %b", threshold, encrypted)
            .containsExactlyElementsOf(frames);
        assertThat(decoder.bufferedBytes()).isZero();
      }
    }
  }

  @Test
  @Timeout(60)
  void aSessionThatTurnsCompressionThenEncryptionOnMidStreamStaysInSync() throws ProtocolException {
    // spec: R28, R31, R32 (compression after login_compression, encryption after the key exchange)
    Random random = new Random(0x5E55L);
    FrameEncoder encoder = FrameEncoder.create();
    FrameDecoder decoder = FrameDecoder.create();
    List<Frame> all = new ArrayList<>();
    ByteArrayOutputStream wire = new ByteArrayOutputStream();
    for (int phase = 0; phase < 3; phase++) {
      if (phase == 1) {
        encoder.setCompressionThreshold(64);
        decoder.setCompressionThreshold(64);
      } else if (phase == 2) {
        encoder.enableEncryption(Cfb8Ciphers.encryptor(SECRET));
        decoder.enableDecryption(Cfb8Ciphers.decryptor(SECRET));
      }
      List<Frame> frames = randomFrames(random, 40);
      all.addAll(frames);
      List<Frame> decoded = new ArrayList<>();
      for (Frame frame : frames) {
        byte[] bytes = encoder.encode(frame);
        wire.writeBytes(bytes);
        decoded.addAll(decoder.feed(bytes, 0, bytes.length));
      }
      assertThat(decoded).as("phase %d", phase).containsExactlyElementsOf(frames);
    }
    assertThat(all).hasSize(120);
  }
}
