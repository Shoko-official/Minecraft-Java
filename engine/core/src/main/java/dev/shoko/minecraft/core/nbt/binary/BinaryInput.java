package dev.shoko.minecraft.core.nbt.binary;

import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Big-endian reads (R3) over decompressed bytes, with the memory quota of R60: units are the bytes
 * consumed plus {@link #TAG_UNITS} per tag object, and the running total may not exceed the quota.
 * A read past the data fails with UNEXPECTED_END at the number of bytes available (R33); the quota
 * is charged after the bytes are taken, so a premature end is reported as such. The source may be
 * read ahead of the consumed position.
 */
final class BinaryInput {

  /** Units of one tag object: the root, a compound value or a list element (R60). */
  static final long TAG_UNITS = 24;

  private static final int BUFFER_SIZE = 8192;

  /** First allocation for a declared length, so that a huge length without data costs nothing. */
  private static final int INITIAL_CAPACITY = 1 << 16;

  private final InputStream in;
  private final long quota;
  private final byte[] buffer = new byte[BUFFER_SIZE];
  private int start;
  private int end;
  private long position;
  private long tags;
  private boolean exhausted;

  BinaryInput(InputStream in, long quota) {
    this.in = in;
    this.quota = quota;
  }

  /** Returns the number of bytes consumed so far, the offset of the next byte. */
  long position() {
    return position;
  }

  /** Counts one more tag object (R60). */
  void countTag() throws NbtException {
    tags++;
    reserve(0);
  }

  /**
   * Fails with TOO_LARGE when the running total plus {@code cost} exceeds the quota: the early
   * check of R60 at an array or list header, before anything is read or allocated.
   */
  void reserve(long cost) throws NbtException {
    long total = position + TAG_UNITS * tags + cost;
    if (total > quota) {
      throw new NbtException(
          Reason.TOO_LARGE, position, total + " units of estimated memory, quota " + quota);
    }
  }

  int readUnsignedByte() throws NbtException {
    require(1);
    int value = buffer[start] & 0xFF;
    consumed(1);
    return value;
  }

  byte readByte() throws NbtException {
    return (byte) readUnsignedByte();
  }

  int readUnsignedShort() throws NbtException {
    require(2);
    int value = ((buffer[start] & 0xFF) << 8) | (buffer[start + 1] & 0xFF);
    consumed(2);
    return value;
  }

  short readShort() throws NbtException {
    return (short) readUnsignedShort();
  }

  int readInt() throws NbtException {
    require(4);
    int value = 0;
    for (int k = 0; k < 4; k++) {
      value = (value << 8) | (buffer[start + k] & 0xFF);
    }
    consumed(4);
    return value;
  }

  long readLong() throws NbtException {
    require(8);
    long value = 0;
    for (int k = 0; k < 8; k++) {
      value = (value << 8) | (buffer[start + k] & 0xFF);
    }
    consumed(8);
    return value;
  }

  /** Reads {@code count} bytes, allocating only as the data actually arrives. */
  byte[] readBytes(int count) throws NbtException {
    byte[] out = new byte[Math.min(count, INITIAL_CAPACITY)];
    int done = 0;
    while (done < count) {
      if (start == end && !fill()) {
        throw unexpectedEnd(position + done);
      }
      int chunk = Math.min(count - done, end - start);
      if (done + chunk > out.length) {
        out = Arrays.copyOf(out, (int) Math.min(count, Math.max(2L * out.length, done + chunk)));
      }
      System.arraycopy(buffer, start, out, done, chunk);
      start += chunk;
      done += chunk;
    }
    position += count;
    reserve(0);
    return out;
  }

  /**
   * Reads the source to its end, discarding the bytes: a container is then checked to its trailer
   * (R61). The discarded bytes follow the root and are not counted (R34, R60).
   */
  void drain() throws NbtException {
    start = end;
    while (fill()) {
      start = end;
    }
  }

  private void consumed(int count) throws NbtException {
    start += count;
    position += count;
    reserve(0);
  }

  /** Makes {@code count} bytes (at most 8) available, or fails with UNEXPECTED_END. */
  private void require(int count) throws NbtException {
    boolean more = true;
    while (end - start < count && more) {
      more = fill();
    }
    if (end - start < count) {
      throw unexpectedEnd(position + end - start);
    }
  }

  /** Reads more bytes after the buffered ones; returns false at the end of the data. */
  private boolean fill() throws NbtException {
    if (exhausted) {
      return false;
    }
    if (start > 0) {
      System.arraycopy(buffer, start, buffer, 0, end - start);
      end -= start;
      start = 0;
    }
    int read;
    try {
      read = in.read(buffer, end, buffer.length - end);
    } catch (IOException e) {
      throw ContainerCodec.readFailure(e);
    }
    if (read < 0) {
      exhausted = true;
      return false;
    }
    end += read;
    return true;
  }

  private static NbtException unexpectedEnd(long available) {
    return new NbtException(Reason.UNEXPECTED_END, available, "data ends after " + available);
  }
}
