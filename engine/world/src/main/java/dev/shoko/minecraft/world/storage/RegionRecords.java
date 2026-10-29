package dev.shoko.minecraft.world.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Internal: the byte layout of a region file record and the choice of its sectors (AR-R10, R12).
 */
final class RegionRecords {

  private static final int FIRST_DATA_SECTOR = 2;
  private static final int MAX_SECTOR = 1 << 24;

  private RegionRecords() {}

  /** A record of {@code length}, type and payload, zero padded to whole sectors. */
  static byte[] record(int type, byte[] payload) {
    int total = Integer.BYTES + 1 + payload.length;
    byte[] record = new byte[sectorsFor(total) * RegionFile.SECTOR];
    ByteBuffer buffer = ByteBuffer.wrap(record);
    buffer.putInt(payload.length + 1).put((byte) type).put(payload);
    return record;
  }

  /** The record of a chunk stored in a {@code .mcc} file: {@code L = 1} and the type plus 128. */
  static byte[] externalRecord(RegionCompression compression) {
    return record(compression.id() | RegionFile.EXTERNAL_FLAG, new byte[0]);
  }

  static int sectorsFor(int bytes) {
    return (bytes + RegionFile.SECTOR - 1) / RegionFile.SECTOR;
  }

  /**
   * Finds the first run of {@code needed} free sectors after the header, given the 1024 locations
   * of the file. Every location that points past the header counts as used, the current record of
   * the chunk being written included: the old record stays intact until the new one is complete.
   *
   * @throws IOException if the offset no longer fits the 3 bytes of a location
   */
  static int firstFit(byte[] locations, int needed) throws IOException {
    ByteBuffer buffer = ByteBuffer.wrap(locations);
    List<int[]> used = new ArrayList<>();
    while (buffer.hasRemaining()) {
      int location = buffer.getInt();
      int offset = location >>> 8;
      int count = location & 0xFF;
      if (offset >= FIRST_DATA_SECTOR && count > 0) {
        used.add(new int[] {offset, count});
      }
    }
    used.sort(Comparator.comparingInt(range -> range[0]));
    int candidate = FIRST_DATA_SECTOR;
    for (int[] range : used) {
      if (candidate + needed <= range[0]) {
        break;
      }
      candidate = Math.max(candidate, range[0] + range[1]);
    }
    if (candidate + needed > MAX_SECTOR) {
      throw new IOException("region file has no room for " + needed + " more sectors");
    }
    return candidate;
  }
}
