package dev.shoko.minecraft.world.storage;

import java.io.ByteArrayOutputStream;

/**
 * The LZ4 block format of the public specification (sequences of literals and matches), without any
 * frame. The decoder is bounded by the length the frame announced for the block; the encoder is a
 * greedy single-pass matcher, deterministic and valid for any reader of the format.
 */
final class Lz4Sequences {

  private static final int MIN_MATCH = 4;
  private static final int MAX_OFFSET = 0xFFFF;
  // The format ends a block with at least 5 literals and starts no match in the last 12 bytes.
  private static final int LAST_LITERALS = 5;
  private static final int MATCH_FIND_LIMIT = 12;
  private static final int HASH_BITS = 12;
  private static final int HASH_MULTIPLIER = 0x9E3779B1;
  private static final int NIBBLE = 15;

  private Lz4Sequences() {}

  /**
   * Decodes the sequences of {@code source[from, to)} into exactly {@code length} bytes.
   *
   * @throws CorruptStreamException if the sequences do not describe exactly that many bytes
   */
  static byte[] decode(byte[] source, int from, int to, int length) throws CorruptStreamException {
    byte[] out = new byte[length];
    int in = from;
    int op = 0;
    while (in < to) {
      int token = source[in++] & 0xFF;
      long literals = (long) token >>> 4;
      if (literals == NIBBLE) {
        long[] extended = extension(source, in, to, literals);
        literals = extended[0];
        in = (int) extended[1];
      }
      if (literals > to - in || literals > length - op) {
        throw new CorruptStreamException("LZ4 literal run exceeds the block");
      }
      System.arraycopy(source, in, out, op, (int) literals);
      in += (int) literals;
      op += (int) literals;
      if (in == to) {
        break;
      }
      if (to - in < 2) {
        throw new CorruptStreamException("LZ4 match offset is cut short");
      }
      int offset = (source[in] & 0xFF) | (source[in + 1] & 0xFF) << 8;
      in += 2;
      long match = token & NIBBLE;
      if (match == NIBBLE) {
        long[] extended = extension(source, in, to, match);
        match = extended[0];
        in = (int) extended[1];
      }
      match += MIN_MATCH;
      if (offset == 0 || offset > op) {
        throw new CorruptStreamException("LZ4 match offset " + offset + " is outside the output");
      }
      if (match > length - op) {
        throw new CorruptStreamException("LZ4 match exceeds the block");
      }
      for (int k = 0; k < match; k++, op++) {
        out[op] = out[op - offset];
      }
    }
    if (op != length) {
      throw new CorruptStreamException(
          "LZ4 block decodes to " + op + " bytes, " + length + " announced");
    }
    return out;
  }

  /** Reads the 255-terminated extension of a length; returns the length and the new position. */
  private static long[] extension(byte[] source, int from, int to, long base)
      throws CorruptStreamException {
    long total = base;
    int in = from;
    int next;
    do {
      if (in >= to) {
        throw new CorruptStreamException("LZ4 length extension is cut short");
      }
      next = source[in++] & 0xFF;
      total += next;
    } while (next == 255);
    return new long[] {total, in};
  }

  /** Compresses {@code data[0, length)} into one block of sequences. */
  static byte[] encode(byte[] data, int length) {
    ByteArrayOutputStream out = new ByteArrayOutputStream(length / 2 + 16);
    int[] table = new int[1 << HASH_BITS];
    int anchor = 0;
    int ip = 0;
    int searchLimit = length - MATCH_FIND_LIMIT;
    int matchLimit = length - LAST_LITERALS;
    while (ip <= searchLimit) {
      int sequence = readInt(data, ip);
      int slot = (sequence * HASH_MULTIPLIER) >>> (Integer.SIZE - HASH_BITS);
      int ref = table[slot] - 1;
      table[slot] = ip + 1;
      if (ref >= 0 && ip - ref <= MAX_OFFSET && readInt(data, ref) == sequence) {
        int matchLength = MIN_MATCH;
        while (ip + matchLength < matchLimit && data[ref + matchLength] == data[ip + matchLength]) {
          matchLength++;
        }
        writeSequence(out, data, anchor, ip - anchor, ip - ref, matchLength);
        ip += matchLength;
        anchor = ip;
      } else {
        ip++;
      }
    }
    writeLastLiterals(out, data, anchor, length - anchor);
    return out.toByteArray();
  }

  private static void writeSequence(
      ByteArrayOutputStream out, byte[] data, int from, int literals, int offset, int match) {
    int matchCode = match - MIN_MATCH;
    out.write(Math.min(literals, NIBBLE) << 4 | Math.min(matchCode, NIBBLE));
    writeExtension(out, literals);
    out.write(data, from, literals);
    out.write(offset);
    out.write(offset >>> 8);
    writeExtension(out, matchCode);
  }

  private static void writeLastLiterals(
      ByteArrayOutputStream out, byte[] data, int from, int literals) {
    out.write(Math.min(literals, NIBBLE) << 4);
    writeExtension(out, literals);
    out.write(data, from, literals);
  }

  private static void writeExtension(ByteArrayOutputStream out, int value) {
    if (value < NIBBLE) {
      return;
    }
    int rest = value - NIBBLE;
    while (rest >= 255) {
      out.write(255);
      rest -= 255;
    }
    out.write(rest);
  }

  private static int readInt(byte[] data, int at) {
    return (data[at] & 0xFF)
        | (data[at + 1] & 0xFF) << 8
        | (data[at + 2] & 0xFF) << 16
        | (data[at + 3] & 0xFF) << 24;
  }
}
