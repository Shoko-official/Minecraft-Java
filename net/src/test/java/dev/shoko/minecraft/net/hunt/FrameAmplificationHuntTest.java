package dev.shoko.minecraft.net.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.frame.FrameDecoder;
import dev.shoko.minecraft.net.frame.FrameLimits;
import dev.shoko.minecraft.net.testing.Hex;
import dev.shoko.minecraft.net.testing.Wire;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// spec: net-protocol-777 R30 d (2^23 per frame), R90, R91 (a denial of service by decompression is
// bounded). The limit of R30 d is per frame: 64 frames of 8 MiB, about 8 KiB each on the wire, make
// 512 MiB out of a few hundred KiB. Whatever the answer (an aggregate cap that refuses with a
// typed error, or frames handed out lazily, one at a time), a 256 MiB heap must not run out.
class FrameAmplificationHuntTest {

  private static final int FRAMES = 64;

  /** Entry point of the child JVM, started with a small heap. */
  public static final class Child {

    private Child() {}

    public static void main(String[] args) throws Exception {
      int limit = FrameLimits.MAX_DECOMPRESSED_LENGTH;
      byte[] body = new byte[limit];
      body[0] = 1;
      byte[] stream = Wire.zlib(body);
      body = null;
      byte[] one = frame(limit, stream);
      ByteArrayOutputStream wire = new ByteArrayOutputStream();
      for (int i = 0; i < FRAMES; i++) {
        wire.writeBytes(one);
      }
      byte[] all = wire.toByteArray();
      System.out.println("input=" + all.length);
      FrameDecoder decoder = FrameDecoder.create();
      decoder.setCompressionThreshold(256);
      int decoded = 0;
      try {
        List<Frame> frames = decoder.feed(all, 0, all.length);
        decoded += frames.size();
        frames = null;
        while (decoder.bufferedBytes() > 0) {
          List<Frame> more = decoder.feed(new byte[0], 0, 0);
          if (more.isEmpty()) {
            break;
          }
          decoded += more.size();
        }
        System.out.println("DECODED " + decoded);
      } catch (ProtocolException typed) {
        System.out.println("TYPED " + typed.reason());
      }
    }

    private static byte[] frame(int dataLength, byte[] stream) {
      byte[] inner = Hex.concat(Wire.varInt(dataLength), stream);
      return Hex.concat(Wire.varInt(inner.length), inner);
    }
  }

  @Test
  @Timeout(60)
  void provisionalSixtyFourFramesOfEightMebibytesFitInTwoHundredFiftySixMebibytesOfHeap()
      throws IOException, InterruptedException {
    // spec: R91. Today feed() materializes every frame of the call: 64 x 8 MiB, out of memory.
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    Process process =
        new ProcessBuilder(
                java,
                "-Xmx256m",
                "-cp",
                System.getProperty("java.class.path"),
                Child.class.getName())
            .redirectErrorStream(true)
            .start();
    byte[] raw = process.getInputStream().readAllBytes();
    String output = new String(raw, StandardCharsets.UTF_8);
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
      process.destroyForcibly();
    }
    assertThat(output).as("child output").doesNotContain("OutOfMemoryError");
    assertThat(process.exitValue()).as(output).isZero();
    assertThat(output).containsAnyOf("DECODED", "TYPED");
  }
}
