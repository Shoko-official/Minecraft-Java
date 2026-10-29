package dev.shoko.minecraft.net.buffer;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.core.nbt.NbtException;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.binary.NbtCompression;
import dev.shoko.minecraft.core.nbt.binary.NbtWriter;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.ProtocolException.Reason;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

  private static final int MAX_IDENTIFIER_CHARS = 32767;
  private static final int HORIZONTAL_BITS = 26;
  private static final int Y_BITS = 12;
  private static final long HORIZONTAL_MASK = (1L << HORIZONTAL_BITS) - 1;
  private static final long Y_MASK = (1L << Y_BITS) - 1;
  private static final int INITIAL_CAPACITY = 64;
  private static final NbtWriter NBT_WRITER = NbtWriter.of(NbtCompression.NONE);

  private final int maxSize;
  private byte[] buffer = new byte[INITIAL_CAPACITY];
  private int size;

  private PacketWriter(int maxSize) {
    this.maxSize = maxSize;
  }

  /**
   * Creates an empty writer bounded by {@link #DEFAULT_MAX_SIZE}.
   *
   * @return the writer
   */
  public static PacketWriter create() {
    return new PacketWriter(DEFAULT_MAX_SIZE);
  }

  /**
   * Creates an empty writer with an explicit bound.
   *
   * @param maxSize the largest payload in bytes, 0 or more
   * @return the writer
   * @throws IllegalArgumentException when {@code maxSize} is negative
   */
  public static PacketWriter create(int maxSize) {
    if (maxSize < 0) {
      throw new IllegalArgumentException("negative bound: " + maxSize);
    }
    return new PacketWriter(maxSize);
  }

  /**
   * Returns the number of bytes written so far.
   *
   * @return the size, 0 or more
   */
  public int size() {
    return size;
  }

  /**
   * Returns a copy of the bytes written so far.
   *
   * @return a new array of {@link #size()} bytes
   */
  public byte[] toByteArray() {
    return Arrays.copyOf(buffer, size);
  }

  /**
   * Writes a Boolean as {@code 00} or {@code 01} (R5).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeBoolean(boolean value) throws ProtocolException {
    writeByte(value ? 1 : 0);
  }

  /**
   * Writes one byte.
   *
   * @param value the low 8 bits are written, so signed and unsigned values both work
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeByte(int value) throws ProtocolException {
    buffer[reserve(1)] = (byte) value;
  }

  /**
   * Writes a big-endian short.
   *
   * @param value the low 16 bits are written, so signed and unsigned values both work
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeShort(int value) throws ProtocolException {
    int at = reserve(2);
    buffer[at] = (byte) (value >> 8);
    buffer[at + 1] = (byte) value;
  }

  /**
   * Writes a big-endian int (R5).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeInt(int value) throws ProtocolException {
    int at = reserve(4);
    for (int i = 0; i < 4; i++) {
      buffer[at + i] = (byte) (value >> (24 - 8 * i));
    }
  }

  /**
   * Writes a big-endian long (R5).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeLong(long value) throws ProtocolException {
    int at = reserve(8);
    for (int i = 0; i < 8; i++) {
      buffer[at + i] = (byte) (value >> (56 - 8 * i));
    }
  }

  /**
   * Writes a big-endian IEEE 754 binary32, keeping the bit pattern (R5).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeFloat(float value) throws ProtocolException {
    writeInt(Float.floatToRawIntBits(value));
  }

  /**
   * Writes a big-endian IEEE 754 binary64, keeping the bit pattern (R5).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeDouble(double value) throws ProtocolException {
    writeLong(Double.doubleToRawLongBits(value));
  }

  /**
   * Writes a VarInt in its shortest form; negative values take 5 bytes (R6).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeVarInt(int value) throws ProtocolException {
    int rest = value;
    while ((rest & ~0x7f) != 0) {
      writeByte((rest & 0x7f) | 0x80);
      rest >>>= 7;
    }
    writeByte(rest);
  }

  /**
   * Writes a VarLong in its shortest form; negative values take 10 bytes (R7).
   *
   * @param value the value
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeVarLong(long value) throws ProtocolException {
    long rest = value;
    while ((rest & ~0x7fL) != 0) {
      writeByte((int) (rest & 0x7f) | 0x80);
      rest >>>= 7;
    }
    writeByte((int) rest);
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
    if (value.length() > maxChars) {
      throw new ProtocolException(
          Reason.STRING_TOO_LONG, "string of " + value.length() + " units, limit " + maxChars);
    }
    byte[] encoded = encodeUtf8(value);
    writeVarInt(encoded.length);
    writeBytes(encoded);
  }

  /**
   * Writes an identifier as a String(32767) (R10).
   *
   * @param value the identifier
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeIdentifier(Identifier value) throws ProtocolException {
    writeString(value.toString(), MAX_IDENTIFIER_CHARS);
  }

  /**
   * Writes a UUID: the most significant long, then the least significant one (R11).
   *
   * @param value the UUID
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeUuid(UUID value) throws ProtocolException {
    writeLong(value.getMostSignificantBits());
    writeLong(value.getLeastSignificantBits());
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
    if (value.length() > maxLength) {
      throw new ProtocolException(
          Reason.BYTES_TOO_LONG, value.length() + " bytes, limit " + maxLength);
    }
    writeVarInt(value.length());
    writeBytes(value.toByteArray());
  }

  /**
   * Writes bytes as they are, with no length: the fields documented as "up to the end of the
   * frame".
   *
   * @param value the bytes
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeRemainingBytes(ByteBlob value) throws ProtocolException {
    writeBytes(value.toByteArray());
  }

  /**
   * Writes a block position (R12).
   *
   * @param value the position
   * @throws ProtocolException with {@code VALUE_OUT_OF_RANGE} when x or z is outside -33554432 to
   *     33554431 or y outside -2048 to 2047, or {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writePosition(BlockPos value) throws ProtocolException {
    if (!fits(value.x(), HORIZONTAL_BITS)
        || !fits(value.z(), HORIZONTAL_BITS)
        || !fits(value.y(), Y_BITS)) {
      throw new ProtocolException(Reason.VALUE_OUT_OF_RANGE, "position out of range: " + value);
    }
    writeLong(
        ((value.x() & HORIZONTAL_MASK) << 38)
            | ((value.z() & HORIZONTAL_MASK) << Y_BITS)
            | (value.y() & Y_MASK));
  }

  /**
   * Writes a low-precision vector (R18).
   *
   * @param value the vector; components are quantized, so the value read back may differ
   * @throws ProtocolException with {@code VALUE_OUT_OF_RANGE} when a component is NaN or infinite,
   *     or {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeLpVec3(Vec3 value) throws ProtocolException {
    LpVec3Codec.write(this, value);
  }

  /**
   * Writes a BitSet: a VarInt count of longs, then the longs (R16).
   *
   * @param value the bit set
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeBitSet(BitSet value) throws ProtocolException {
    long[] words = value.toLongArray();
    writeVarInt(words.length);
    for (long word : words) {
      writeLong(word);
    }
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
    if (value.length() > bits) {
      throw new ProtocolException(
          Reason.VALUE_OUT_OF_RANGE, "bit " + (value.length() - 1) + " set, size " + bits);
    }
    int length = (int) (((long) bits + 7) / 8);
    int at = reserve(length);
    byte[] packed = value.toByteArray();
    System.arraycopy(packed, 0, buffer, at, packed.length);
  }

  /**
   * Writes a network NBT value: a type byte then the payload with no root name (R17).
   *
   * @param value the value
   * @throws ProtocolException with {@code INVALID_NBT}, caused by the {@code NbtException}, or
   *     {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeNbt(NbtTag value) throws ProtocolException {
    try {
      writeBytes(NBT_WRITER.writeUnnamed(value));
    } catch (NbtException e) {
      throw new ProtocolException(Reason.INVALID_NBT, "NBT value cannot be written", e);
    }
  }

  /**
   * Writes the absent NBT value, the single End byte {@code 00} (R17).
   *
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeAbsentNbt() throws ProtocolException {
    writeByte(0);
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
    if (values.size() > maxLength) {
      throw new ProtocolException(
          Reason.ARRAY_TOO_LONG, values.size() + " elements, limit " + maxLength);
    }
    writeVarInt(values.size());
    for (T value : values) {
      element.write(this, value);
    }
  }

  /**
   * Writes the absent form of a prefixed optional: the Boolean false (R14).
   *
   * @throws ProtocolException with {@code PAYLOAD_TOO_LARGE} past the bound
   */
  public void writeAbsent() throws ProtocolException {
    writeBoolean(false);
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
    writeBoolean(true);
    element.write(this, value);
  }

  /** Reserves {@code count} bytes at the end and returns the index of the first one. */
  private int reserve(int count) throws ProtocolException {
    if (count > maxSize - size) {
      throw new ProtocolException(
          Reason.PAYLOAD_TOO_LARGE, "payload would pass the bound of " + maxSize + " bytes");
    }
    int needed = size + count;
    if (needed > buffer.length) {
      buffer = Arrays.copyOf(buffer, (int) Math.min(maxSize, Math.max(2L * buffer.length, needed)));
    }
    int at = size;
    size = needed;
    return at;
  }

  private void writeBytes(byte[] bytes) throws ProtocolException {
    int at = reserve(bytes.length);
    System.arraycopy(bytes, 0, buffer, at, bytes.length);
  }

  private static boolean fits(int value, int bits) {
    return value >= -(1 << (bits - 1)) && value < (1 << (bits - 1));
  }

  private static byte[] encodeUtf8(String value) throws ProtocolException {
    try {
      ByteBuffer encoded =
          StandardCharsets.UTF_8
              .newEncoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .encode(CharBuffer.wrap(value));
      byte[] bytes = new byte[encoded.remaining()];
      encoded.get(bytes);
      return bytes;
    } catch (CharacterCodingException e) {
      throw new ProtocolException(Reason.INVALID_UTF8, "string is not encodable as UTF-8", e);
    }
  }
}
