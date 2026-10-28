package dev.shoko.minecraft.world.storage;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Internal: measures how many bytes a binary NBT document takes without building it, to find the
 * bytes that follow the root (AR-R30 d). The NBT reader of {@code core} stops at the end of the
 * root but does not say where that is.
 */
final class NbtExtent {

  private static final int TYPE_END = 0;
  private static final int TYPE_BYTE_ARRAY = 7;
  private static final int TYPE_STRING = 8;
  private static final int TYPE_LIST = 9;
  private static final int TYPE_COMPOUND = 10;
  private static final int TYPE_INT_ARRAY = 11;
  private static final int TYPE_LONG_ARRAY = 12;
  // Payload size of the types 1 to 6 (byte, short, int, long, float, double).
  private static final int[] FIXED_SIZE = {0, 1, 2, 4, 8, 4, 8};

  private final ByteBuffer in;

  private NbtExtent(byte[] data) {
    this.in = ByteBuffer.wrap(data);
  }

  /**
   * Returns the length of the named root at the start of the data.
   *
   * @param data a document the NBT reader accepted
   * @return the number of bytes of the root, or the length of the data if it cannot be measured
   */
  static int rootLength(byte[] data) {
    NbtExtent extent = new NbtExtent(data);
    try {
      int type = extent.in.get() & 0xFF;
      if (type != TYPE_END) {
        extent.skip(extent.in.getShort() & 0xFFFF);
        extent.payload(type);
      }
      return extent.in.position();
    } catch (BufferUnderflowException | IllegalArgumentException e) {
      return data.length;
    }
  }

  private void skip(long count) {
    if (count < 0 || count > in.remaining()) {
      throw new BufferUnderflowException();
    }
    in.position(in.position() + (int) count);
  }

  private void payload(int type) {
    if (type >= 1 && type < FIXED_SIZE.length) {
      skip(FIXED_SIZE[type]);
      return;
    }
    switch (type) {
      case TYPE_BYTE_ARRAY -> skip(in.getInt());
      case TYPE_STRING -> skip(in.getShort() & 0xFFFF);
      case TYPE_INT_ARRAY -> skip(4L * in.getInt());
      case TYPE_LONG_ARRAY -> skip(8L * in.getInt());
      case TYPE_LIST -> list();
      case TYPE_COMPOUND -> compound();
      default -> throw new IllegalArgumentException("unknown tag type " + type);
    }
  }

  private void list() {
    int elementType = in.get() & 0xFF;
    int count = in.getInt();
    if (elementType >= 1 && elementType < FIXED_SIZE.length) {
      skip((long) FIXED_SIZE[elementType] * count);
      return;
    }
    for (int index = 0; index < count; index++) {
      payload(elementType);
    }
  }

  private void compound() {
    while (true) {
      int type = in.get() & 0xFF;
      if (type == TYPE_END) {
        return;
      }
      skip(in.getShort() & 0xFFFF);
      payload(type);
    }
  }
}
