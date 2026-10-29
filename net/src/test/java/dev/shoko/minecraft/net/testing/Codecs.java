package dev.shoko.minecraft.net.testing;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.net.ProtocolException;
import dev.shoko.minecraft.net.frame.Frame;
import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.packet.PacketCodecs;
import dev.shoko.minecraft.net.state.ConnectionState;
import dev.shoko.minecraft.net.state.PacketDirection;

/** Shortcuts to decode a payload given as bytes or hexadecimal, and to check both directions. */
public final class Codecs {

  private Codecs() {}

  /** Decodes a payload under a state, a direction and an id. */
  public static Packet decode(
      ConnectionState state, PacketDirection direction, int id, byte[] payload)
      throws ProtocolException {
    return PacketCodecs.decode(state, direction, Frame.of(id, payload));
  }

  /** Decodes a payload written as hexadecimal. */
  public static Packet decode(
      ConnectionState state, PacketDirection direction, int id, String payloadHex)
      throws ProtocolException {
    return decode(state, direction, id, Hex.bytes(payloadHex));
  }

  /**
   * Checks both directions of a packet: it encodes to this id and these payload bytes, and these
   * bytes decode to an equal packet.
   */
  public static void assertWire(
      ConnectionState state, PacketDirection direction, int id, Packet packet, String payloadHex)
      throws ProtocolException {
    Frame frame = PacketCodecs.encode(packet);
    assertThat(frame.packetId()).as("packet id").isEqualTo(id);
    assertThat(Hex.format(frame.payload())).as("payload").isEqualTo(payloadHex);
    assertThat(decode(state, direction, id, payloadHex)).isEqualTo(packet);
  }
}
