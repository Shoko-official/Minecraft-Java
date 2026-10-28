package dev.shoko.minecraft.tools.oracle.nbt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Writes raw NBT bytes field by field, so that tests can build valid documents as well as
 * deliberately broken ones. It encodes nothing on its own: each byte is chosen by the test.
 */
public final class NbtBytes {

  private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
  private final DataOutputStream out = new DataOutputStream(bytes);

  /** Type id followed by the name, as for the root and for compound entries. */
  public NbtBytes named(int type, String name) {
    return u8(type).utf(name);
  }

  public NbtBytes u8(int value) {
    return write(() -> out.writeByte(value));
  }

  public NbtBytes i16(int value) {
    return write(() -> out.writeShort(value));
  }

  public NbtBytes i32(int value) {
    return write(() -> out.writeInt(value));
  }

  public NbtBytes i64(long value) {
    return write(() -> out.writeLong(value));
  }

  public NbtBytes f32(float value) {
    return write(() -> out.writeFloat(value));
  }

  public NbtBytes f64(double value) {
    return write(() -> out.writeDouble(value));
  }

  /** Length-prefixed modified UTF-8, as produced by {@link DataOutputStream#writeUTF}. */
  public NbtBytes utf(String value) {
    return write(() -> out.writeUTF(value));
  }

  public NbtBytes raw(int... values) {
    for (int value : values) {
      u8(value);
    }
    return this;
  }

  public NbtBytes end() {
    return u8(0);
  }

  public byte[] toByteArray() {
    return bytes.toByteArray();
  }

  public static byte[] gzip(byte[] data) {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(result)) {
      gzip.write(data);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return result.toByteArray();
  }

  public static byte[] zlib(byte[] data) {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    try (DeflaterOutputStream zlib = new DeflaterOutputStream(result)) {
      zlib.write(data);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return result.toByteArray();
  }

  /** A root compound named {@code root} holding one int entry, used as a chunk stand-in. */
  public static byte[] rootWithInt(String key, int value) {
    return new NbtBytes().named(10, "root").named(3, key).i32(value).end().toByteArray();
  }

  private NbtBytes write(IoAction action) {
    try {
      action.run();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return this;
  }

  private interface IoAction {
    void run() throws IOException;
  }
}
