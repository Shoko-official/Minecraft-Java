package dev.shoko.minecraft.net.packet.login;

import dev.shoko.minecraft.net.buffer.ByteBlob;
import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.LoginClientbound;
import dev.shoko.minecraft.net.registry.PacketType;

/**
 * Encryption request, clientbound (R40, R50). Not sent in offline mode (R57).
 *
 * @param serverId the server id, String(20), empty (R50)
 * @param publicKey the RSA public key in DER, prefixed array of bytes (162 bytes for RSA-1024)
 * @param verifyToken the verify token, prefixed array of bytes (4 bytes sent by the server, R50)
 * @param shouldAuthenticate whether the client must contact the session service
 */
public record HelloClientboundPacket(
    String serverId, ByteBlob publicKey, ByteBlob verifyToken, boolean shouldAuthenticate)
    implements Packet {

  @Override
  public PacketType type() {
    return LoginClientbound.HELLO;
  }
}
