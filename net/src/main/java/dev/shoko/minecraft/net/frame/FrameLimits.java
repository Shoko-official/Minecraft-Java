package dev.shoko.minecraft.net.frame;

/**
 * Size limits of framing. The defaults are the rules of the spec; a smaller value lets a test
 * exercise a limit without building a huge frame.
 *
 * @param maxFrameLength the largest value of the frame length field, which is the compressed size
 *     when compression is on (R27, R30)
 * @param maxDecompressedLength the largest decompressed size accepted before allocating (R30 d)
 */
public record FrameLimits(int maxFrameLength, int maxDecompressedLength) {

  /** Largest frame length: 2 097 151, which fits in 3 VarInt bytes (R27). */
  public static final int MAX_FRAME_LENGTH = (1 << 21) - 1;

  /** Largest decompressed size: 2^23 = 8 388 608 bytes (R30, provisional). */
  public static final int MAX_DECOMPRESSED_LENGTH = 1 << 23;

  /** The limits of R27 and R30. */
  public static final FrameLimits DEFAULT =
      new FrameLimits(MAX_FRAME_LENGTH, MAX_DECOMPRESSED_LENGTH);
}
