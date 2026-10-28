package dev.shoko.minecraft.core.nbt.binary;

import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtException.Reason;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Big-endian reads (R3) over decompressed bytes, counting the bytes consumed. A read past the data
 * fails with UNEXPECTED_END at the number of bytes available (R33); a read past {@code maxBytes}
 * fails with TOO_LARGE at the limit (R32). The source may be read ahead of the consumed position.
 */
final class BinaryInput {

  private static final int BUFFER_SIZE = 8192;

  /** First allocation for a declared length, so that a huge length without data costs nothing. */
  private static final int INITIAL_CAPACITY = 1 << 16;

  private final InputStream in;
  private final long maxBytes;
  private final byte[] buffer = new byte[BUFFER_SIZE];
  private int start;
  private int end;
  private long position;
  private boolean exhausted;

  BinaryInput(InputStream in, long maxBytes) {
    this.in = in;
    this.maxBytes = maxBytes;
  }

  /** Returns the number of bytes consumed so far, the offset of the next byte. */
  long position() {
    return position;
  }

  int readUnsignedByte() throws NbtException {
    require(1);
    position++;
    return buffer[start++] & 0xFF;
  }

  byte readByte() throws NbtException {
    return (byte) readUnsignedByte();
  }

  int readUnsignedShort() throws NbtException {
    require(2);
    position += 2;
    int value = ((buffer[start] & 0xFF) << 8) | (buffer[start + 1] & 0xFF);
    start += 2;
    return value;
  }

  short readShort() throws NbtException {
    return (short) readUnsignedShort();
  }

  int readInt() throws NbtException {
    require(4);
    position += 4;
    int value = 0;
    for (int k = 0; k < 4; k++) {
      value = (value << 8) | (buffer[start++] & 0xFF);
    }
    return value;
  }

  long readLong() throws NbtException {
    long high = readInt() & 0xFFFFFFFFL;
    return (high << 32) | (readInt() & 0xFFFFFFFFL);
  }

  /** Reads {@code count} bytes, allocating only as the data actually arrives. */
  byte[] readBytes(int count) throws NbtException {
    byte[] out = new byte[Math.min(count, INITIAL_CAPACITY)];
    int done = 0;
    while (done < count) {
      if (start == end && !fill()) {
        throw unexpectedEnd(position);
      }
      if (position >= maxBytes) {
        throw tooLarge();
      }
      int chunk = (int) Math.min(Math.min(count - done, end - start), maxBytes - position);
      if (done + chunk > out.length) {
        out = Arrays.copyOf(out, (int) Math.min(count, Math.max(2L * out.length, done + chunk)));
      }
      System.arraycopy(buffer, start, out, done, chunk);
      start += chunk;
      position += chunk;
      done += chunk;
    }
    return out;
  }

  /** Makes {@code count} bytes (at most 8) available, or fails as the class describes. */
  private void require(int count) throws NbtException {
    boolean more = true;
    while (end - start < count && more) {
      more = fill();
    }
    int available = end - start;
    if (position + count > maxBytes) {
      throw position + available >= maxBytes ? tooLarge() : unexpectedEnd(position + available);
    }
    if (available < count) {
      throw unexpectedEnd(position + available);
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

  private NbtException tooLarge() {
    return new NbtException(
        Reason.TOO_LARGE, maxBytes, "read needs more than " + maxBytes + " bytes");
  }
}
