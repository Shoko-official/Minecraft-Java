package dev.shoko.minecraft.net.frame;

import dev.shoko.minecraft.net.buffer.PacketReader;
import java.util.Arrays;

/**
 * One packet as it crosses the framing layer: a packet id and the payload that follows it, before
 * any packet-specific decoding. The state and the direction are not part of a frame: the receiver
 * knows them (R2). Immutable.
 */
public final class Frame {

  private final int packetId;
  private final byte[] payload;

  private Frame(int packetId, byte[] payload) {
    this.packetId = packetId;
    this.payload = payload;
  }

  /**
   * Creates a frame.
   *
   * @param packetId the VarInt packet id, 0 or more
   * @param payload the bytes after the id; copied
   * @return the frame
   */
  public static Frame of(int packetId, byte[] payload) {
    return new Frame(packetId, payload.clone());
  }

  /** Creates a frame that takes ownership of an array nobody else holds. */
  static Frame adopt(int packetId, byte[] payload) {
    return new Frame(packetId, payload);
  }

  /**
   * Returns the packet id.
   *
   * @return the id
   */
  public int packetId() {
    return packetId;
  }

  /**
   * Returns a copy of the payload.
   *
   * @return a new array, empty for a packet with no field
   */
  public byte[] payload() {
    return payload.clone();
  }

  /**
   * Opens a reader bounded to the payload (R90).
   *
   * @return a new reader at the first payload byte
   */
  public PacketReader reader() {
    return PacketReader.wrap(payload);
  }

  /** Equal when the ids and the payload bytes are equal. */
  @Override
  public boolean equals(Object other) {
    return other instanceof Frame frame
        && packetId == frame.packetId
        && Arrays.equals(payload, frame.payload);
  }

  /** Returns a hash consistent with {@link #equals(Object)}. */
  @Override
  public int hashCode() {
    return 31 * packetId + Arrays.hashCode(payload);
  }

  /** Returns a description for humans: id and payload length. */
  @Override
  public String toString() {
    return "Frame[id=" + packetId + ", " + payload.length + " bytes]";
  }
}
