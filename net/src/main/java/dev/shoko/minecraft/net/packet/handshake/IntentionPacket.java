package dev.shoko.minecraft.net.packet.handshake;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.HandshakeServerbound;
import dev.shoko.minecraft.net.registry.PacketType;
import dev.shoko.minecraft.net.state.ConnectionIntent;

/**
 * First packet of every connection, serverbound (R34, vectors F01, F04). Selects the next state by
 * its intention.
 *
 * @param protocolVersion the protocol number the client speaks, VarInt; 777 for this version, -1 is
 *     admitted for a status ping (R36)
 * @param serverAddress the address the client typed, String(255)
 * @param serverPort the port, unsigned short 0 to 65535
 * @param intent the intention VarInt, 1 status, 2 login, 3 transfer (R34)
 */
public record IntentionPacket(
    int protocolVersion, String serverAddress, int serverPort, ConnectionIntent intent)
    implements Packet {

  @Override
  public PacketType type() {
    return HandshakeServerbound.INTENTION;
  }
}
