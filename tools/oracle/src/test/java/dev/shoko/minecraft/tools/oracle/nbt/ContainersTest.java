package dev.shoko.minecraft.tools.oracle.nbt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Container trailers are checked to the end; a defect in any trailer field is refused. */
class ContainersTest {

  private static final byte[] PLAIN = NbtBytes.rootWithInt("v", 42);
  private static final long LIMIT = 1 << 20;

  private static byte[] gunzip(byte[] data) throws IOException {
    return Containers.gunzip(new ByteArrayInputStream(data), LIMIT);
  }

  private static byte[] inflate(byte[] data) throws IOException {
    return Containers.inflateZlib(new ByteArrayInputStream(data), LIMIT);
  }

  private static byte[] concat(byte[] first, byte[] second) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.writeBytes(first);
    out.writeBytes(second);
    return out.toByteArray();
  }

  @Test
  void decodesAWellFormedGzipMemberAndIgnoresBytesAfterIt() throws IOException {
    byte[] gzip = NbtBytes.gzip(PLAIN);

    assertThat(gunzip(gzip)).isEqualTo(PLAIN);
    assertThat(gunzip(concat(gzip, new byte[] {1, 2, 3}))).isEqualTo(PLAIN);
    assertThat(gunzip(concat(gzip, NbtBytes.gzip(new byte[] {9})))).isEqualTo(PLAIN);
  }

  @Test
  void refusesAWrongCrcOrSizeInTheGzipTrailer() {
    byte[] badCrc = NbtBytes.gzip(PLAIN);
    badCrc[badCrc.length - 8] ^= 0x01;
    byte[] badSize = NbtBytes.gzip(PLAIN);
    badSize[badSize.length - 4] ^= 0x01;

    assertThatThrownBy(() -> gunzip(badCrc)).hasMessageContaining("CRC-32 mismatch");
    assertThatThrownBy(() -> gunzip(badSize)).hasMessageContaining("ISIZE mismatch");
  }

  @Test
  void refusesAMissingOrTruncatedGzipTrailer() {
    byte[] gzip = NbtBytes.gzip(PLAIN);

    assertThatThrownBy(() -> gunzip(Arrays.copyOf(gzip, gzip.length - 8)))
        .hasMessageContaining("trailer missing");
    assertThatThrownBy(() -> gunzip(Arrays.copyOf(gzip, gzip.length - 3)))
        .hasMessageContaining("trailer missing");
    assertThatThrownBy(() -> gunzip(Arrays.copyOf(gzip, gzip.length / 2)))
        .hasMessageContaining("truncated");
  }

  @Test
  void skipsOptionalGzipHeaderFields() throws IOException {
    byte[] gzip = NbtBytes.gzip(PLAIN);
    byte[] name = "chunk.nbt\0".getBytes(StandardCharsets.US_ASCII);
    byte[] header = Arrays.copyOf(gzip, 10);
    header[3] = 0x08;
    byte[] withName = concat(concat(header, name), Arrays.copyOfRange(gzip, 10, gzip.length));

    assertThat(gunzip(withName)).isEqualTo(PLAIN);
  }

  @Test
  void decodesAWellFormedZlibStreamAndIgnoresBytesAfterIt() throws IOException {
    byte[] zlib = NbtBytes.zlib(PLAIN);

    assertThat(inflate(zlib)).isEqualTo(PLAIN);
    assertThat(inflate(concat(zlib, new byte[] {0, 0, 0, 7}))).isEqualTo(PLAIN);
  }

  @Test
  void refusesAWrongOrMissingAdler32() {
    byte[] badAdler = NbtBytes.zlib(PLAIN);
    badAdler[badAdler.length - 1] ^= 0x01;
    byte[] zlib = NbtBytes.zlib(PLAIN);

    assertThatThrownBy(() -> inflate(badAdler)).hasMessageContaining("incorrect data check");
    assertThatThrownBy(() -> inflate(Arrays.copyOf(zlib, zlib.length - 4)))
        .hasMessageContaining("truncated");
    assertThatThrownBy(() -> inflate(Arrays.copyOf(zlib, zlib.length - 1)))
        .hasMessageContaining("truncated");
  }
}
