package dev.shoko.minecraft.net.packet.login;

import dev.shoko.minecraft.net.buffer.ByteBlob;
import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.LoginServerbound;
import dev.shoko.minecraft.net.registry.PacketType;

/**
 * Answers the encryption request, serverbound (R51). Both fields are RSA PKCS#1 v1.5 ciphertexts
 * made with the server public key.
 *
 * @param sharedSecret the encrypted shared secret, prefixed array of bytes
 * @param verifyToken the encrypted verify token, prefixed array of bytes
 */
public record KeyPacket(ByteBlob sharedSecret, ByteBlob verifyToken) implements Packet {

  @Override
  public PacketType type() {
    return LoginServerbound.KEY;
  }
}
