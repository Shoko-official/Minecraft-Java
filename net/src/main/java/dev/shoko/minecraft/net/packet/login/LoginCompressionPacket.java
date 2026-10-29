package dev.shoko.minecraft.net.packet.login;

import dev.shoko.minecraft.net.packet.Packet;
import dev.shoko.minecraft.net.registry.LoginClientbound;
import dev.shoko.minecraft.net.registry.PacketType;

/**
 * Sets the compression threshold for the rest of the connection, clientbound (R28, vector F07).
 *
 * @param threshold VarInt; 0 or more switches compression on, negative switches it off
 */
public record LoginCompressionPacket(int threshold) implements Packet {

  @Override
  public PacketType type() {
    return LoginClientbound.LOGIN_COMPRESSION;
  }
}
