package dev.shoko.minecraft.world.storage;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * The LZ4 frame of compression type 4, the "LZ4Block" stream of the lz4-java library, not the
 * standard LZ4 Frame (AR-R15, AR-R16). Implements the public LZ4 block format and XXH32.
 *
 * <p>A stream is a series of blocks with a 21 byte header: the 8 ASCII bytes {@code LZ4Block}, a
 * token (method {@code 0x10} raw or {@code 0x20} LZ4, ORed with the level {@code max(0,
 * ceil(log2(blockSize)) - 10)}), then three little-endian ints: compressed length, decompressed
 * length, checksum. The checksum is {@code XXH32(decompressed block, seed 0x9747b28c) &
 * 0x0FFFFFFF}. The stream ends with a raw-method header whose three ints are 0. A block is written
 * raw when compression does not shrink it. Vector LZ1 of {@code world-vectors.md} fixes the bytes
 * for {@code minecraft} repeated 8 times.
 *
 * <p>Decoding is bounded (AR-R29): the output never exceeds the limit given, and no length
 * announced by a block header is believed before it has been compared with the budget left.
 */
public final class Lz4BlockCodec {

  /** Block size the writer uses, 65 536 bytes, level 6, tokens {@code 0x16} and {@code 0x26}. */
  public static final int BLOCK_SIZE = 65_536;

  private static final byte[] MAGIC = "LZ4Block".getBytes(StandardCharsets.US_ASCII);
  private static final int HEADER_LENGTH = 21;
  private static final int METHOD_MASK = 0xF0;
  private static final int METHOD_RAW = 0x10;
  private static final int METHOD_LZ4 = 0x20;
  private static final int LEVEL_MASK = 0x0F;
  private static final int LEVEL_BASE = 10;
  private static final int WRITER_LEVEL = 6;
  private static final int SEED = 0x9747B28C;
  private static final int CHECKSUM_MASK = 0x0FFFFFFF;

  private Lz4BlockCodec() {}

  /**
   * Decodes a whole stream (AR-R15, AR-R16, AR-R29). Accepts any block size the token allows, both
   * methods, and verifies the masked checksum. A total of exactly {@code maxOutputBytes} is
   * accepted, one byte more is not.
   *
   * @param stream the stream including its end marker
   * @param maxOutputBytes the most bytes the whole stream may produce, all blocks together
   * @return the decompressed bytes
   * @throws OutputLimitExceededException if the output would exceed the limit; raised from the
   *     header of a block whose decompressed length exceeds the budget left, before any allocation
   *     for it, and during decoding otherwise
   * @throws CorruptStreamException if the stream is malformed: missing end marker or bytes after
   *     it, unknown method, non-positive length, decompressed length above the block size of the
   *     token, raw block with unequal lengths, copy distance 0 or beyond the output, wrong
   *     checksum. The first event in stream order decides between the two exceptions
   */
  public static byte[] decode(byte[] stream, long maxOutputBytes)
      throws CorruptStreamException, OutputLimitExceededException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int at = 0;
    while (true) {
      if (stream.length - at < HEADER_LENGTH) {
        throw new CorruptStreamException("LZ4 stream has no end mark, cut at byte " + at);
      }
      if (!Arrays.equals(stream, at, at + MAGIC.length, MAGIC, 0, MAGIC.length)) {
        throw new CorruptStreamException("LZ4 block at byte " + at + " has no LZ4Block magic");
      }
      int token = stream[at + 8] & 0xFF;
      int method = token & METHOD_MASK;
      int compressed = readIntLittleEndian(stream, at + 9);
      int length = readIntLittleEndian(stream, at + 13);
      int checksum = readIntLittleEndian(stream, at + 17);
      int bodyStart = at + HEADER_LENGTH;
      if (method == METHOD_RAW && compressed == 0 && length == 0 && checksum == 0) {
        if (bodyStart != stream.length) {
          throw new CorruptStreamException("bytes follow the LZ4 end mark");
        }
        return out.toByteArray();
      }
      checkHeader(token, compressed, length);
      if (length > maxOutputBytes - out.size()) {
        throw new OutputLimitExceededException(maxOutputBytes);
      }
      if (compressed > stream.length - bodyStart) {
        throw new CorruptStreamException("LZ4 block at byte " + at + " is cut short");
      }
      byte[] block =
          method == METHOD_RAW
              ? Arrays.copyOfRange(stream, bodyStart, bodyStart + length)
              : Lz4Sequences.decode(stream, bodyStart, bodyStart + compressed, length);
      if ((Xxh32.hash(block, SEED) & CHECKSUM_MASK) != checksum) {
        throw new CorruptStreamException("LZ4 block at byte " + at + " has a wrong checksum");
      }
      out.writeBytes(block);
      at = bodyStart + compressed;
    }
  }

  private static void checkHeader(int token, int compressed, int length)
      throws CorruptStreamException {
    int method = token & METHOD_MASK;
    if (method != METHOD_RAW && method != METHOD_LZ4) {
      throw new CorruptStreamException("unknown LZ4 block method " + method);
    }
    if (compressed <= 0 || length <= 0) {
      throw new CorruptStreamException("LZ4 block lengths must be positive");
    }
    if (length > 1 << ((token & LEVEL_MASK) + LEVEL_BASE)) {
      throw new CorruptStreamException("LZ4 block is larger than its level allows");
    }
    if (method == METHOD_RAW && compressed != length) {
      throw new CorruptStreamException("raw LZ4 block has unequal lengths");
    }
  }

  /**
   * Encodes bytes like lz4-java's default output stream: blocks of {@link #BLOCK_SIZE}, raw when
   * not smaller, then the end marker. The exact compressed bytes of a block are not fixed by the
   * spec; only {@link #decode(byte[], long)} of the result must give the input.
   *
   * @param data the bytes, possibly empty
   * @return the stream
   */
  public static byte[] encode(byte[] data) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int from = 0; from < data.length; from += BLOCK_SIZE) {
      int length = Math.min(BLOCK_SIZE, data.length - from);
      byte[] block = Arrays.copyOfRange(data, from, from + length);
      byte[] packed = Lz4Sequences.encode(block, length);
      int checksum = Xxh32.hash(block, SEED) & CHECKSUM_MASK;
      if (packed.length < length) {
        writeBlock(out, METHOD_LZ4, packed, length, checksum);
      } else {
        writeBlock(out, METHOD_RAW, block, length, checksum);
      }
    }
    writeBlock(out, METHOD_RAW, new byte[0], 0, 0);
    return out.toByteArray();
  }

  private static void writeBlock(
      ByteArrayOutputStream out, int method, byte[] body, int length, int checksum) {
    ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
    header.put(MAGIC).put((byte) (method | WRITER_LEVEL));
    header.putInt(body.length).putInt(length).putInt(checksum);
    out.writeBytes(header.array());
    out.writeBytes(body);
  }

  private static int readIntLittleEndian(byte[] bytes, int at) {
    return ByteBuffer.wrap(bytes, at, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
  }
}
