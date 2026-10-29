package dev.shoko.minecraft.net.buffer;

/**
 * An immutable sequence of bytes, for binary packet fields such as a public key or a verify token.
 * It exists so that records can hold bytes with a content-based {@code equals}; a bare array would
 * compare by identity.
 */
public final class ByteBlob {

  private ByteBlob() {}

  /**
   * Copies bytes into a blob.
   *
   * @param bytes the bytes; later changes to the array do not affect the blob
   * @return the blob
   */
  public static ByteBlob of(byte[] bytes) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the number of bytes.
   *
   * @return the length, 0 or more
   */
  public int length() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a copy of the bytes.
   *
   * @return a new array
   */
  public byte[] toByteArray() {
    return Skeleton.throwNotImplemented();
  }

  /** Equal when both hold the same bytes in the same order. */
  @Override
  public boolean equals(Object other) {
    return Skeleton.throwNotImplemented();
  }

  /** Returns a hash consistent with {@link #equals(Object)}. */
  @Override
  public int hashCode() {
    return Skeleton.throwNotImplemented();
  }

  /** Returns a description for humans: the length, not the content. */
  @Override
  public String toString() {
    return Skeleton.throwNotImplemented();
  }
}
