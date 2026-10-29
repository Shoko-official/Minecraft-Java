package dev.shoko.minecraft.net.packet;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.registry.PacketType;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;

/**
 * Converts between {@link Frame} and {@link Packet}. This first batch covers the handshake, status,
 * login and configuration packets listed in the packages {@code packet.handshake}, {@code
 * packet.status}, {@code packet.login} and {@code packet.configuration}; the other packets of the
 * 260 are known to the registry but not yet to this class (play packets come in a second batch).
 */
public final class PacketCodecs {

  private PacketCodecs() {}

  /**
   * Tells whether this class can decode and encode a packet type.
   *
   * @param type the packet type
   * @return true for the packets of the first batch, false for the others
   */
  public static boolean isSupported(PacketType type) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Decodes a frame received in a given state and direction: finds the packet type of the frame id,
   * then reads its fields, which must use the payload exactly (R90).
   *
   * @param state the state of the connection when the frame arrived
   * @param direction the direction of the frame
   * @param frame the frame
   * @return the packet
   * @throws ProtocolException with {@code UNKNOWN_PACKET_ID} when the id is not in the table of the
   *     pair (R49), the failure of the field that is malformed (string or array too long, invalid
   *     UTF-8 or identifier, bad NBT, unknown intention, truncated payload), or {@code
   *     TRAILING_BYTES} when bytes remain after the last field
   * @throws IllegalArgumentException when the packet type of the id exists but is not supported
   *     (see {@link #isSupported(PacketType)})
   */
  public static Packet decode(ConnectionState state, PacketDirection direction, Frame frame)
      throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Encodes a packet as a frame, with the id of its type.
   *
   * @param packet the packet
   * @return the frame, ready for a {@code FrameEncoder}
   * @throws ProtocolException when a field cannot be written: string, array or byte array over its
   *     maximum, value out of range, invalid NBT
   * @throws IllegalArgumentException when the type of the packet is not supported (see {@link
   *     #isSupported(PacketType)})
   */
  public static Frame encode(Packet packet) throws ProtocolException {
    return Skeleton.throwNotImplemented();
  }
}
