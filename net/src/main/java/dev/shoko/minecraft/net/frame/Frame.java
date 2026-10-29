package dev.shoko.minecraft.net.frame;

import dev.shoko.minecraft.net.buffer.PacketReader;

/**
 * One packet as it crosses the framing layer: a packet id and the payload that follows it, before
 * any packet-specific decoding. The state and the direction are not part of a frame: the receiver
 * knows them (R2). Immutable.
 */
public final class Frame {

  private Frame() {}

  /**
   * Creates a frame.
   *
   * @param packetId the VarInt packet id, 0 or more
   * @param payload the bytes after the id; copied
   * @return the frame
   */
  public static Frame of(int packetId, byte[] payload) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns the packet id.
   *
   * @return the id
   */
  public int packetId() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Returns a copy of the payload.
   *
   * @return a new array, empty for a packet with no field
   */
  public byte[] payload() {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Opens a reader bounded to the payload (R90).
   *
   * @return a new reader at the first payload byte
   */
  public PacketReader reader() {
    return Skeleton.throwNotImplemented();
  }

  /** Equal when the ids and the payload bytes are equal. */
  @Override
  public boolean equals(Object other) {
    return Skeleton.throwNotImplemented();
  }

  /** Returns a hash consistent with {@link #equals(Object)}. */
  @Override
  public int hashCode() {
    return Skeleton.throwNotImplemented();
  }

  /** Returns a description for humans: id and payload length. */
  @Override
  public String toString() {
    return Skeleton.throwNotImplemented();
  }
}
