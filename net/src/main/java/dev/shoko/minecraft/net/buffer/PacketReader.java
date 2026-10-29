package dev.shoko.minecraft.net.buffer;

import dev.shoko.minecraft.core.coord.BlockPos;
import dev.shoko.minecraft.core.coord.Vec3;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.net.ProtocolException;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the wire types of protocol 777 from a byte range that is the whole of one packet payload.
 * The range is the bound: no read goes past it and no allocation is sized from an announced length
 * that has not been compared with its maximum and with the bytes left (R90). Every method throws a
 * {@link ProtocolException} on malformed input and leaves the reader unusable after a failure. Not
 * thread-safe.
 */
public final class PacketReader {

  private PacketReader() {}

  /**
   * Creates a reader over a payload.
   *
   * @param data the payload; the reader does not copy it and the caller must not change it
   * @param offset index of the first byte to read
   * @param length number of bytes of the payload
   * @return the reader
   * @throws IndexOutOfBoundsException when the range is not inside {@code data}
   */
  public static PacketReader wrap(byte[] data, int offset, int length) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Creates a reader over a whole array.
   *
   * @param data the payload; the reader does not copy it and the caller must not change it
   * @return the reader
   */
  public static PacketReader wrap(byte[] data) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of bytes not yet read.
   *
   * @return the remaining byte count, 0 or more
   */
  public int remaining() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Checks that the payload has been read to its last byte (R90).
   *
   * @throws ProtocolException with {@code TRAILING_BYTES} when bytes remain
   */
  public void requireFullyConsumed() throws ProtocolException {
    Skeleton.throwNotImplemented();
  }

  /**
   * Reads a Boolean: {@code 00} false, any other value true (R5, provisional).
   *
   * @return the value
   * @throws ProtocolException with {@code UNEXPECTED_END} at the end of the payload
   */
  public boolean readBoolean() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a signed byte.
   *
   * @return the value
   * @throws ProtocolException with {@code UNEXPECTED_END} at the end of the payload
   */
  public byte readByte() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads an unsigned byte.
   *
   * @return the value, 0 to 255
   * @throws ProtocolException with {@code UNEXPECTED_END} at the end of the payload
   */
  public int readUnsignedByte() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a big-endian signed short (R5).
   *
   * @return the value
   * @throws ProtocolException with {@code UNEXPECTED_END} at the end of the payload
   */
  public short readShort() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a big-endian unsigned short, such as a server port.
   *
   * @return the value, 0 to 65535
   * @throws ProtocolException with {@code UNEXPECTED_END} at the end of the payload
   */
  public int readUnsignedShort() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a big-endian int (R5).
   *
   * @return the value
   * @throws ProtocolException with {@code UNEXPECTED_END} at the end of the payload
   */
  public int readInt() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a big-endian long (R5).
   *
   * @return the value
   * @throws ProtocolException with {@code UNEXPECTED_END} at the end of the payload
   */
  public long readLong() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a big-endian IEEE 754 binary32, keeping the bit pattern (R5).
   *
   * @return the value
   * @throws ProtocolException with {@code UNEXPECTED_END} at the end of the payload
   */
  public float readFloat() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a big-endian IEEE 754 binary64, keeping the bit pattern (R5).
   *
   * @return the value
   * @throws ProtocolException with {@code UNEXPECTED_END} at the end of the payload
   */
  public double readDouble() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a VarInt (R6, R8): up to 5 bytes, 7 bits per byte, low group first. Overlong encodings
   * are accepted within 5 bytes; the bits of the fifth byte beyond the 32nd are dropped (vector
   * N22).
   *
   * @return the value
   * @throws ProtocolException with {@code VAR_INT_TOO_BIG} when the fifth byte still has the
   *     continuation bit (vector N21), or {@code UNEXPECTED_END} when the payload ends first
   */
  public int readVarInt() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a VarLong (R7, R8): the same scheme on up to 10 bytes.
   *
   * @return the value
   * @throws ProtocolException with {@code VAR_INT_TOO_BIG} when the tenth byte still has the
   *     continuation bit, or {@code UNEXPECTED_END} when the payload ends first
   */
  public long readVarLong() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a String(n): a VarInt byte length, then UTF-8 without terminator (R9).
   *
   * @param maxChars the maximum length in UTF-16 code units, at most 32767
   * @return the string
   * @throws ProtocolException with {@code NEGATIVE_LENGTH}, {@code STRING_TOO_LONG} (more than
   *     {@code maxChars} units, or more than {@code 3 * maxChars + 3} bytes, checked before the
   *     bytes are read), {@code INVALID_UTF8} or {@code UNEXPECTED_END}
   */
  public String readString(int maxChars) throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads an identifier: a String(32767) that must follow the identifier grammar (R10).
   *
   * @return the identifier
   * @throws ProtocolException with {@code INVALID_IDENTIFIER} when the text is not an identifier,
   *     or any failure of {@link #readString(int)}
   */
  public Identifier readIdentifier() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a UUID: the most significant long, then the least significant one (R11).
   *
   * @return the UUID
   * @throws ProtocolException with {@code UNEXPECTED_END} at the end of the payload
   */
  public UUID readUuid() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a byte array preceded by its VarInt length (R14).
   *
   * @param maxLength the maximum length in bytes
   * @return the bytes
   * @throws ProtocolException with {@code NEGATIVE_LENGTH}, {@code BYTES_TOO_LONG} or {@code
   *     UNEXPECTED_END}; the length is compared with the maximum and with the bytes left before any
   *     allocation
   */
  public ByteBlob readPrefixedBytes(int maxLength) throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads every byte left, for the fields documented as "up to the end of the frame".
   *
   * @param maxLength the maximum length in bytes
   * @return the bytes, possibly empty
   * @throws ProtocolException with {@code BYTES_TOO_LONG} when more than {@code maxLength} bytes
   *     remain
   */
  public ByteBlob readRemainingBytes(int maxLength) throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a block position (R12): x on the 26 high bits, z on the next 26, y on the 12 low bits,
   * each sign-extended.
   *
   * @return the position
   * @throws ProtocolException with {@code UNEXPECTED_END} at the end of the payload
   */
  public BlockPos readPosition() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a low-precision vector (R18): one {@code 00} byte for the zero vector, else 6 bytes plus
   * a VarInt when the scale exceeds 3.
   *
   * @return the vector, each component dequantized
   * @throws ProtocolException with {@code UNEXPECTED_END} or {@code VAR_INT_TOO_BIG}
   */
  public Vec3 readLpVec3() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a BitSet: a VarInt count of longs, then the longs (R16).
   *
   * @return the bit set
   * @throws ProtocolException with {@code NEGATIVE_LENGTH} or {@code UNEXPECTED_END}; the count is
   *     compared with the bytes left before any allocation
   */
  public BitSet readBitSet() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a fixed-size BitSet of {@code ceil(bits / 8)} bytes, bit i being bit {@code i % 8} of
   * byte {@code i / 8} (R16). It is not equivalent to {@link #readBitSet()}.
   *
   * @param bits the number of bits, 0 or more
   * @return the bit set; bits beyond {@code bits} in the last byte are ignored
   * @throws ProtocolException with {@code UNEXPECTED_END}
   */
  public BitSet readFixedBitSet(int bits) throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a network NBT value (R17): a type byte then the payload with no root name.
   *
   * @return the value
   * @throws ProtocolException with {@code INVALID_NBT}, caused by the {@code NbtException}, when
   *     the value is malformed or is the End byte alone, which {@link #readNbtOrAbsent()} accepts
   */
  public NbtTag readNbt() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a network NBT value that may be the single End byte {@code 00} (R17).
   *
   * @return the value, or empty for the End byte
   * @throws ProtocolException with {@code INVALID_NBT}, caused by the {@code NbtException}
   */
  public Optional<NbtTag> readNbtOrAbsent() throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a prefixed array: a VarInt count, then the elements (R14).
   *
   * @param <T> the element type
   * @param maxLength the maximum number of elements
   * @param element reads one element
   * @return the elements in wire order, unmodifiable
   * @throws ProtocolException with {@code NEGATIVE_LENGTH}, {@code ARRAY_TOO_LONG} or {@code
   *     UNEXPECTED_END}; the count is compared with the maximum and with the bytes left before any
   *     allocation, an element taking at least one byte
   */
  public <T> List<T> readPrefixedArray(int maxLength, ElementReader<T> element)
      throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads a prefixed optional: a Boolean, then the value when it is true (R14).
   *
   * @param <T> the value type
   * @param element reads the value
   * @return the value, or empty when the Boolean is false
   * @throws ProtocolException on a malformed value or {@code UNEXPECTED_END}
   */
  public <T> Optional<T> readPrefixedOptional(ElementReader<T> element) throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }
}
