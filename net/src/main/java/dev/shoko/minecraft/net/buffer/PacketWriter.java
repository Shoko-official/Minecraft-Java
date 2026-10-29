package dev.shoko.minecraft.net.buffer;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.net.ProtocolException;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

/**
 * Writes the wire types of protocol 777 into a growing buffer that is the payload of one packet.
 * Mirror of {@link PacketReader}: what is written here reads back unchanged. The size is bounded,
 * and a write that would exceed the bound fails with {@code PAYLOAD_TOO_LARGE}. A failed write
 * leaves the writer unusable. Not thread-safe.
 */
public final class PacketWriter {

  /** Largest payload of a writer created without an explicit bound: 2 097 151 bytes (R27). */
  public static final int DEFAULT_MAX_SIZE = (1 << 21) - 1;

  private PacketWriter() {}

  /**
   * Creates an empty writer bounded by {@link #DEFAULT_MAX_SIZE}.
   *
   * @return the writer
   */
  public static PacketWriter create() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Creates an empty writer with an explicit bound.
   *
   * @param maxSize the largest payload in bytes, 0 or more
   * @return the writer
   * @throws IllegalArgumentException when {@code maxSize} is negative
   */
  public static PacketWriter create(int maxSize) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of bytes written so far.
   *
   * @return the size, 0 or more
   */
  public int size() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a copy of the bytes written so far.
   *
   * @return a new array of {@link #size()} bytes
   */
  public byte[] toByteArray() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Writes a Boolean as {@code 00} or {@code 01} (R5).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeBoolean(boolean value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes one byte.
   *
   * @param value the low 8 bits are written, so signed and unsigned values both work
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeByte(int value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a big-endian short.
   *
   * @param value the low 16 bits are written, so signed and unsigned values both work
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeShort(int value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a big-endian int (R5).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeInt(int value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a big-endian long (R5).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeLong(long value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a big-endian IEEE 754 binary32, keeping the bit pattern (R5).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeFloat(float value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a big-endian IEEE 754 binary64, keeping the bit pattern (R5).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeDouble(double value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a VarInt in its shortest form; negative values take 5 bytes (R6).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeVarInt(int value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a VarLong in its shortest form; negative values take 10 bytes (R7).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeVarLong(long value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a String(n): a VarInt byte length then UTF-8 (R9).
   *
   * @param value the string
   * @param maxChars the maximum length in UTF-16 code units, at most 32767
   * @throws ProtocolException with {@code STRING_TOO_LONG} when {@code value} is longer than {@code
   *     maxChars} units, or {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeString(String value, int maxChars) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes an identifier as a String(32767) (R10).
   *
   * @param value the identifier
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeIdentifier(Identifier value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a UUID: the most significant long, then the least significant one (R11).
   *
   * @param value the UUID
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeUuid(UUID value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a byte array preceded by its VarInt length (R14).
   *
   * @param value the bytes
   * @param maxLength the maximum length in bytes
   * @throws ProtocolException with {@code BYTES_TOO_LONG} when {@code value} is longer than {@code
   *     maxLength}, or {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writePrefixedBytes(ByteBlob value, int maxLength) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes bytes as they are, with no length: the fields documented as "up to the end of the
   * frame".
   *
   * @param value the bytes
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeRemainingBytes(ByteBlob value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a block position (R12).
   *
   * @param value the position
   * @throws ProtocolException with {@code VALUE_OUT_OF_RANGE} when x or z is outside -33554432 to
   *     33554431 or y outside -2048 to 2047, or {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writePosition(BlockPos value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a low-precision vector (R18).
   *
   * @param value the vector; components are quantized, so the value read back may differ
   * @throws ProtocolException with {@code VALUE_OUT_OF_RANGE} when a component is NaN or infinite,
   *     or {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeLpVec3(Vec3 value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a BitSet: a VarInt count of longs, then the longs (R16).
   *
   * @param value the bit set
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeBitSet(BitSet value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a fixed-size BitSet of {@code ceil(bits / 8)} bytes (R16).
   *
   * @param value the bit set
   * @param bits the number of bits, 0 or more
   * @throws ProtocolException with {@code VALUE_OUT_OF_RANGE} when a bit at or above {@code bits}
   *     is set, or {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeFixedBitSet(BitSet value, int bits) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a network NBT value: a type byte then the payload with no root name (R17).
   *
   * @param value the value
   * @throws ProtocolException with {@code INVALID_NBT}, caused by the {@code NbtException}, or
   *     {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeNbt(NbtTag value) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes the absent NBT value, the single End byte {@code 00} (R17).
   *
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeAbsentNbt() throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes a prefixed array: a VarInt count, then the elements (R14).
   *
   * @param <T> the element type
   * @param values the elements
   * @param maxLength the maximum number of elements
   * @param element writes one element
   * @throws ProtocolException with {@code ARRAY_TOO_LONG} when there are more than {@code
   *     maxLength} elements, or any failure of {@code element}
   */
  public <T> void writePrefixedArray(List<T> values, int maxLength, ElementWriter<T> element)
      throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes the absent form of a prefixed optional: the Boolean false (R14).
   *
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeAbsent() throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Writes the present form of a prefixed optional: the Boolean true, then the value (R14).
   *
   * @param <T> the value type
   * @param value the value
   * @param element writes the value
   * @throws ProtocolException with any failure of {@code element}
   */
  public <T> void writePresent(T value, ElementWriter<T> element) throws ProtocolException {
    Skeleton.throwNotImplemented();
  }
}
