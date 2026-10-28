package dev.shoko.minecraft.world.support;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds and parses the LZ4Block frame of AR-R15 with plain literals, for test streams. Written
 * from the spec, not from the code under test.
 */
public final class Lz4Frames {

  public static final int SEED = 0x9747B28C;
  public static final int RAW = 0x10;
  public static final int LZ4 = 0x20;
  private static final byte[] MAGIC = "LZ4Block".getBytes(StandardCharsets.US_ASCII);

  /** One block of a stream. */
  public record Block(int token, int compressedLength, int length, int checksum) {}

  private Lz4Frames() {}

  public static int checksum(byte[] plain) {
    return Xxh32.hash(plain, SEED) & 0x0FFFFFFF;
  }

  /** A block with explicit fields, for building valid and damaged streams. */
  public static byte[] block(
      int token, int compressedLength, int length, int checksum, byte[] body) {
    ByteBuffer header = ByteBuffer.allocate(21).order(ByteOrder.LITTLE_ENDIAN);
    header.put(MAGIC).put((byte) token).putInt(compressedLength).putInt(length).putInt(checksum);
    return Hex.concat(header.array(), body);
  }

  public static byte[] rawBlock(byte[] plain, int level) {
    return block(RAW | level, plain.length, plain.length, checksum(plain), plain);
  }

  /** A block holding the plain bytes as one literal-only LZ4 sequence. */
  public static byte[] literalBlock(byte[] plain, int level) {
    byte[] body = literalSequence(plain);
    return block(LZ4 | level, body.length, plain.length, checksum(plain), body);
  }

  public static byte[] endMark(int level) {
    return block(RAW | level, 0, 0, 0, new byte[0]);
  }

  /** The stream of raw blocks of at most {@code 1 << (level + 10)} bytes, then the end mark. */
  public static byte[] rawStream(byte[] data, int level) {
    int size = 1 << (level + 10);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int from = 0; from < data.length; from += size) {
      byte[] part = java.util.Arrays.copyOfRange(data, from, Math.min(data.length, from + size));
      out.writeBytes(rawBlock(part, level));
    }
    out.writeBytes(endMark(level));
    return out.toByteArray();
  }

  /** The default writer's stream shape: raw blocks of 65536 bytes, level 6. */
  public static byte[] defaultStream(byte[] data) {
    return rawStream(data, 6);
  }

  /**
   * A block of the compressed method that expands to {@code length} bytes of {@code 'a'}: one
   * literal, one long match at distance 1, then five literals. Small on the wire, large when
   * decoded, so it serves to test limits. The length is at least 11.
   */
  public static byte[] runBlock(int length, int level) {
    int match = length - 6;
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    int extra = match - 4;
    body.write((1 << 4) | Math.min(extra, 15));
    body.write('a');
    body.write(1);
    body.write(0);
    if (extra >= 15) {
      int rest = extra - 15;
      while (rest >= 255) {
        body.write(255);
        rest -= 255;
      }
      body.write(rest);
    }
    body.write(5 << 4);
    body.writeBytes("aaaaa".getBytes(StandardCharsets.US_ASCII));
    byte[] plain = new byte[length];
    java.util.Arrays.fill(plain, (byte) 'a');
    byte[] bytes = body.toByteArray();
    return block(LZ4 | level, bytes.length, length, checksum(plain), bytes);
  }

  /** An LZ4 sequence made of literals only, which is a valid last sequence of a block. */
  public static byte[] literalSequence(byte[] literals) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int length = literals.length;
    out.write(Math.min(length, 15) << 4);
    if (length >= 15) {
      int rest = length - 15;
      while (rest >= 255) {
        out.write(255);
        rest -= 255;
      }
      out.write(rest);
    }
    out.writeBytes(literals);
    return out.toByteArray();
  }

  /** Parses a stream of this shape; fails on anything the spec does not allow. */
  public static List<Block> parse(byte[] stream) {
    ByteBuffer in = ByteBuffer.wrap(stream).order(ByteOrder.LITTLE_ENDIAN);
    List<Block> blocks = new ArrayList<>();
    while (true) {
      byte[] magic = new byte[8];
      in.get(magic);
      if (!java.util.Arrays.equals(magic, MAGIC)) {
        throw new IllegalArgumentException("bad magic");
      }
      int token = in.get() & 0xFF;
      int compressed = in.getInt();
      int length = in.getInt();
      int checksum = in.getInt();
      in.position(in.position() + compressed);
      blocks.add(new Block(token, compressed, length, checksum));
      if (length == 0) {
        if (in.hasRemaining()) {
          throw new IllegalArgumentException("bytes after the end mark");
        }
        return blocks;
      }
    }
  }
}
