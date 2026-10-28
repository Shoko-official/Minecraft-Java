package dev.shoko.minecraft.world.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Builds and reads region files by hand from AR-R5 to AR-R10, to feed and to check the code under
 * test without using it.
 */
public final class Regions {

  public static final int SECTOR = 4096;
  public static final long TIMESTAMP_A = 1_790_000_000L;
  public static final long TIMESTAMP_B = 1_790_000_001L;

  /** One record to place in a file: header index, compression byte, payload, timestamp. */
  public static final class Blob {
    private final int index;
    private final int type;
    private final byte[] payload;
    private final long timestamp;

    public Blob(int index, int type, byte[] payload, long timestamp) {
      this.index = index;
      this.type = type;
      this.payload = payload.clone();
      this.timestamp = timestamp;
    }

    public int index() {
      return index;
    }

    public int type() {
      return type;
    }

    public byte[] payload() {
      return payload.clone();
    }

    public long timestamp() {
      return timestamp;
    }
  }

  private Regions() {}

  public static byte[] zlib(byte[] data) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (DeflaterOutputStream deflate = new DeflaterOutputStream(out, new Deflater(6))) {
      deflate.write(data);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out.toByteArray();
  }

  public static byte[] gzip(byte[] data) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
      gzip.write(data);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out.toByteArray();
  }

  /** The compressed payload of a record for the compression byte 1 to 4. */
  public static byte[] compress(int type, byte[] data) {
    return switch (type) {
      case 1 -> gzip(data);
      case 2 -> zlib(data);
      case 3 -> data.clone();
      case 4 -> Lz4Frames.defaultStream(data);
      default -> throw new IllegalArgumentException("type " + type);
    };
  }

  /** The record of a blob: length, type, payload, zero padding to a whole number of sectors. */
  public static byte[] record(int type, byte[] payload) {
    ByteBuffer head = ByteBuffer.allocate(5);
    head.putInt(payload.length + 1).put((byte) type);
    byte[] raw = Hex.concat(head.array(), payload);
    return java.util.Arrays.copyOf(raw, sectorsFor(raw.length) * SECTOR);
  }

  public static int sectorsFor(int bytes) {
    return (bytes + SECTOR - 1) / SECTOR;
  }

  /** A region file with these blobs placed one after the other from sector 2. */
  public static byte[] file(List<Blob> blobs) {
    byte[] header = new byte[2 * SECTOR];
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    int sector = 2;
    for (Blob blob : blobs) {
      byte[] record = record(blob.type(), blob.payload());
      int count = record.length / SECTOR;
      putInt(header, 4 * blob.index(), (sector << 8) | count);
      putInt(header, SECTOR + 4 * blob.index(), (int) blob.timestamp());
      body.writeBytes(record);
      sector += count;
    }
    return Hex.concat(header, body.toByteArray());
  }

  /** The file of vector RG1 for the given compression of chunk A (-1, -1). */
  public static byte[] rg1(int typeOfA, byte[] nbtOfA, byte[] nbtOfB) {
    List<Blob> blobs = new ArrayList<>();
    blobs.add(new Blob(1023, typeOfA, compress(typeOfA, nbtOfA), TIMESTAMP_A));
    blobs.add(new Blob(0, 3, nbtOfB, TIMESTAMP_B));
    return file(blobs);
  }

  public static void putInt(byte[] array, int at, int value) {
    ByteBuffer.wrap(array).putInt(at, value);
  }

  public static int getInt(byte[] array, int at) {
    return ByteBuffer.wrap(array).getInt(at);
  }

  public static String ascii(byte[] bytes) {
    return new String(bytes, StandardCharsets.US_ASCII);
  }

  /** The location entry of a header index. */
  public static int location(byte[] file, int index) {
    return getInt(file, 4 * index);
  }

  public static int sectorOf(byte[] file, int index) {
    return location(file, index) >>> 8;
  }

  public static int countOf(byte[] file, int index) {
    return location(file, index) & 0xFF;
  }

  public static long timestamp(byte[] file, int index) {
    return getInt(file, SECTOR + 4 * index) & 0xFFFFFFFFL;
  }

  /** The length field {@code L} of the record of an index. */
  public static int lengthOf(byte[] file, int index) {
    return getInt(file, sectorOf(file, index) * SECTOR);
  }

  public static int typeOf(byte[] file, int index) {
    return file[sectorOf(file, index) * SECTOR + 4] & 0xFF;
  }

  /** The {@code L - 1} payload bytes of the record of an index. */
  public static byte[] payloadOf(byte[] file, int index) {
    int start = sectorOf(file, index) * SECTOR + 5;
    return java.util.Arrays.copyOfRange(file, start, start + lengthOf(file, index) - 1);
  }

  /** The header indices whose location is not zero, in increasing order. */
  public static List<Integer> usedIndices(byte[] file) {
    List<Integer> used = new ArrayList<>();
    for (int index = 0; index < 1024; index++) {
      if (location(file, index) != 0) {
        used.add(index);
      }
    }
    return used;
  }
}
