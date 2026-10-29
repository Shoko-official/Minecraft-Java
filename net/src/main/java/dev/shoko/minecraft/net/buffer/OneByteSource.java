package dev.shoko.minecraft.net.buffer;

import java.io.InputStream;

/**
 * A view of a byte range that hands out one byte per bulk read. The NBT reader buffers what it
 * reads, so a plain array stream would be drained past the end of the value; with one byte per read
 * it takes exactly what it consumes, and {@link #consumed()} is then the size of the value.
 */
final class OneByteSource extends InputStream {

  private final byte[] data;
  private final int start;
  private final int end;
  private int position;

  OneByteSource(byte[] data, int start, int end) {
    this.data = data;
    this.start = start;
    this.end = end;
    this.position = start;
  }

  int consumed() {
    return position - start;
  }

  @Override
  public int read() {
    return position < end ? data[position++] & 0xff : -1;
  }

  @Override
  public int read(byte[] target, int offset, int length) {
    if (length == 0) {
      return 0;
    }
    if (position >= end) {
      return -1;
    }
    target[offset] = data[position++];
    return 1;
  }
}
