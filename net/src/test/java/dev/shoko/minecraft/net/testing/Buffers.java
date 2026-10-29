package dev.shoko.minecraft.net.testing;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.buffer.PacketReader;
import dev.shoko.minecraft.net.buffer.PacketWriter;

/** Shortcuts to write bytes with a {@link PacketWriter} and to read them back. */
public final class Buffers {

  private Buffers() {}

  /** One write sequence on a writer. */
  @FunctionalInterface
  public interface Write {
    /** Writes into the writer. */
    void accept(PacketWriter out) throws ProtocolException;
  }

  /** One read on a reader. */
  @FunctionalInterface
  public interface Read<T> {
    /** Reads from the reader. */
    T accept(PacketReader in) throws ProtocolException;
  }

  /** Runs the sequence on a writer with the default bound and returns the bytes. */
  public static byte[] write(Write sequence) throws ProtocolException {
    PacketWriter out = PacketWriter.create();
    sequence.accept(out);
    return out.toByteArray();
  }

  /** Reads a value from the bytes and requires that every byte is used. */
  public static <T> T readAll(byte[] data, Read<T> read) throws ProtocolException {
    PacketReader in = PacketReader.wrap(data);
    T value = read.accept(in);
    in.requireFullyConsumed();
    return value;
  }

  /** Reads a value from bytes written as hexadecimal and requires that every byte is used. */
  public static <T> T readAll(String hex, Read<T> read) throws ProtocolException {
    return readAll(Hex.bytes(hex), read);
  }

  /** Opens a reader over bytes written as hexadecimal. */
  public static PacketReader reader(String hex) {
    return PacketReader.wrap(Hex.bytes(hex));
  }
}
