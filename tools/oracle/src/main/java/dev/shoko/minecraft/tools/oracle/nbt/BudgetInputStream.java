package dev.shoko.minecraft.tools.oracle.nbt;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Counts the bytes read and fails once a fixed budget is exceeded. */
final class BudgetInputStream extends FilterInputStream {

  private final long limit;
  private long consumed;

  BudgetInputStream(InputStream in, long limit) {
    super(in);
    this.limit = limit;
  }

  long remaining() {
    return limit - consumed;
  }

  @Override
  public int read() throws IOException {
    int value = super.read();
    if (value >= 0) {
      count(1);
    }
    return value;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    int read = super.read(buffer, offset, (int) Math.min(length, remaining() + 1));
    if (read > 0) {
      count(read);
    }
    return read;
  }

  @Override
  public long skip(long n) throws IOException {
    throw new IOException("Skipping is not supported while decoding NBT");
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  private void count(int bytes) throws IOException {
    consumed += bytes;
    if (consumed > limit) {
      throw new IOException("Decoded NBT exceeds the limit of " + limit + " bytes");
    }
  }
}
