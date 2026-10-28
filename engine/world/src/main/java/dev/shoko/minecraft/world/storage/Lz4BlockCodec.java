package dev.shoko.minecraft.world.storage;

import java.io.IOException;

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
 */
public final class Lz4BlockCodec {

  /** Block size the writer uses, 65 536 bytes, level 6, tokens {@code 0x16} and {@code 0x26}. */
  public static final int BLOCK_SIZE = 65_536;

  private Lz4BlockCodec() {}

  /**
   * Decodes a whole stream (AR-R15, AR-R16). Accepts any block size the token allows, both methods,
   * and verifies the masked checksum.
   *
   * @param stream the stream including its end marker
   * @return the decompressed bytes
   * @throws IOException if the stream is malformed: missing end marker or bytes after it, unknown
   *     method, non-positive length, decompressed length above the block size of the token, raw
   *     block with unequal lengths, copy distance 0 or beyond the output, wrong checksum
   */
  public static byte[] decode(byte[] stream) throws IOException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Encodes bytes like lz4-java's default output stream: blocks of {@link #BLOCK_SIZE}, raw when
   * not smaller, then the end marker. The exact compressed bytes of a block are not fixed by the
   * spec; only {@link #decode(byte[])} of the result must give the input.
   *
   * @param data the bytes, possibly empty
   * @return the stream
   */
  public static byte[] encode(byte[] data) {
    return Skeleton.throwNotImplemented();
  }
}
