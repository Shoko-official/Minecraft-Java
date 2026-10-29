package dev.shoko.minecraft.world.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal: the output of a bounded decompression, kept as the pieces it was produced in and joined
 * once at the end. A growing buffer would hold the old and the new array while it grows, and a
 * stream copy would add a third: here the peak is the pieces plus the final array, that is twice
 * the bytes actually produced, never a multiple of the limit (AR-R29).
 */
final class OutputPieces {

  /**
   * The most bytes one Java array can hold; a limit above it is capped, as no output can exceed.
   */
  static final long MAX_BYTES = Integer.MAX_VALUE - 8L;

  private final List<byte[]> pieces = new ArrayList<>();
  private long size;

  /** The limit a decoder enforces: the caller's limit, capped to what an array can hold. */
  static long effectiveLimit(long limit) {
    return Math.min(limit, MAX_BYTES);
  }

  /** Adds a piece, which the caller no longer modifies. */
  void add(byte[] piece) {
    pieces.add(piece);
    size += piece.length;
  }

  long size() {
    return size;
  }

  byte[] toByteArray() {
    if (pieces.size() == 1) {
      return pieces.getFirst();
    }
    byte[] out = new byte[(int) size];
    int at = 0;
    for (byte[] piece : pieces) {
      System.arraycopy(piece, 0, out, at, piece.length);
      at += piece.length;
    }
    return out;
  }
}
